package telekit.plugins.linetest.database;

import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;
import telekit.base.domain.exception.TelekitException;
import telekit.base.domain.security.UsernamePasswordCredentials;
import telekit.base.net.connection.ConnectionParams;
import telekit.base.service.EntityRepository;
import telekit.base.service.crypto.EncryptionService;
import telekit.base.util.jdbc.MapperBasedHandler;
import telekit.base.util.jdbc.MapperBasedListHandler;
import telekit.base.util.jdbc.QueryRunnerAdapter;
import telekit.base.util.jdbc.ResultSetMapper;
import telekit.plugins.linetest.domain.*;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static telekit.base.i18n.BaseMessages.MGG_DATABASE_ERROR;
import static telekit.base.i18n.I18n.t;
import static telekit.plugins.linetest.domain.MeasuredValue.ValueType;

public class MeasurementTaskRepository implements EntityRepository<MeasurementTask, UUID> {

    private final DataSource dataSource;
    private final QueryRunnerAdapter runner;
    private final ResultSetMapper<MeasurementTask> mapper;
    private @Nullable StringEncryptor encryptor;

    public MeasurementTaskRepository(DataSource dataSource) {
        this.dataSource = dataSource;

        runner = new QueryRunnerAdapter(dataSource);
        mapper = new MeasurementTaskResultMapper();
    }

    public @Nullable EncryptionService getEncryptionService() {
        return encryptor != null ? encryptor.getEncryptionService() : null;
    }

    public void setEncryptionService(@Nullable EncryptionService encryptionService) {
        this.encryptor = encryptionService != null ? new StringEncryptor(encryptionService) : null;
    }

    @Override
    public Collection<MeasurementTask> getAll() {
        String sql = "SELECT * FROM measurement_task ORDER BY datetime DESC;";
        return runner.query(sql, new MapperBasedListHandler<>(mapper));
    }

    public Collection<MeasurementTask> getFromEnd(int offset, int count) {
        String sql = "SELECT * FROM measurement_task ORDER BY datetime DESC OFFSET ? LIMIT ?;";
        return runner.query(sql, new MapperBasedListHandler<>(mapper), offset, count);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(id) FROM measurement_task;";
        return runner.query(sql, new ScalarHandler<>());
    }

    @Override
    public Optional<MeasurementTask> find(MeasurementTask task) {
        return findById(task.getId());
    }

    @Override
    public Optional<MeasurementTask> findById(UUID id) {
        String sql = "SELECT * FROM measurement_task WHERE id = ?;";
        return Optional.ofNullable(runner.query(sql, new MapperBasedHandler<>(mapper), id));
    }

    @Override
    public boolean contains(MeasurementTask task) {
        return containsId(task.getId());
    }

    @Override
    public void add(MeasurementTask task) {
        Objects.requireNonNull(task.getId());

        String sql = """
                INSERT INTO measurement_task (
                    id,
                    datetime,
                    provider,
                    line,
                    url,
                    username,
                    password,
                    duration,
                    connection_status
                )
                VALUES (?,?,?,?,?,?,?,?,?);
                 """.indent(-4);

        Object[] params = new Object[9];
        params[0] = task.getId();
        params[1] = task.getDateTime().atOffset(ZoneOffset.UTC);

        LinetestRequest request = task.getRequest();
        ConnectionParams connectionParams = request.getConnectionParams();
        params[2] = request.getProvider();
        params[3] = request.getLine();
        params[4] = connectionParams.toUri().toString();
        if (connectionParams.getCredentials() instanceof UsernamePasswordCredentials userPassword) {
            params[5] = userPassword.getUsername();
            params[6] = encryptor != null ?
                    encryptor.encrypt(userPassword.getPassword()) :
                    userPassword.getPasswordAsString();
        }

        params[7] = task.getDuration();
        params[8] = task.getConnectionStatus();

        runner.update(sql, params);
    }

    @Override
    public UUID addAndReturnId(MeasurementTask task) {
        add(task);
        return task.getId();
    }

    @Override
    public void update(MeasurementTask task) {
        String sql = """
                UPDATE measurement_task
                SET datetime = ?,
                    provider = ?,
                    line = ?,
                    url = ?,
                    username = ?,
                    password = ?,
                    duration = ?,
                    connection_status = ?,
                    test_failed = ?,
                    line_status = ?,
                    resistance = ?,
                    capacitance = ?,
                    dc_voltage = ?,
                    ac_voltage = ?,
                    raw_output = ?
                WHERE id = ?;
                """.indent(-4);

        Object[] params = new Object[16];
        params[0] = task.getDateTime().atOffset(ZoneOffset.UTC);

        LinetestRequest request = task.getRequest();
        ConnectionParams connectionParams = request.getConnectionParams();
        params[1] = request.getProvider();
        params[2] = request.getLine();
        params[3] = connectionParams.toUri().toString();
        if (connectionParams.getCredentials() instanceof UsernamePasswordCredentials userPassword) {
            params[4] = userPassword.getUsername();
            params[5] = encryptor != null ?
                    encryptor.encrypt(userPassword.getPassword()) :
                    userPassword.getPasswordAsString();
        }

        params[6] = task.getDuration();
        params[7] = task.getConnectionStatus();

        // measurement
        try (Connection c = dataSource.getConnection()) {
            Measurement measurement = task.getResult();
            if (measurement != null) {
                params[8] = measurement.isTestFailed();

                if (measurement.getLineStatus() != null) {
                    params[9] = measurement.getLineStatus().toString();
                }
                params[10] = c.createArrayOf("double", getMeasuredValues(measurement, ValueType.RESISTANCE));
                params[11] = c.createArrayOf("double", getMeasuredValues(measurement, ValueType.CAPACITANCE));
                params[12] = c.createArrayOf("double", getMeasuredValues(measurement, ValueType.DC_VOLTAGE));
                params[13] = c.createArrayOf("double", getMeasuredValues(measurement, ValueType.AC_VOLTAGE));
                params[14] = measurement.getRawOutput();
            }

            params[15] = task.getId();

            runner.update(c, sql, params);
        } catch (SQLException e) {
            throw new TelekitException(t(MGG_DATABASE_ERROR), e);
        }
    }

    @Override
    public void remove(MeasurementTask task) {
        removeById(task.getId());
    }

    @Override
    public void removeById(UUID id) {
        String sql = "DELETE FROM measurement_task WHERE id = ?;";
        runner.update(sql, id);
    }

    @Override
    public void clear() {
        String sql = "TRUNCATE measurement_task;";
        runner.update(sql);
    }

    private Double[] getMeasuredValues(Measurement measurement, ValueType valueType) {
        MeasuredValue v = measurement.getMeasuredValue(valueType);
        return v != null ? ArrayUtils.toObject(v.toArray()) : new Double[]{0d, 0d, 0d};
    }

    ///////////////////////////////////////////////////////////////////////////

    class MeasurementTaskResultMapper implements ResultSetMapper<MeasurementTask> {

        @Override
        public MeasurementTask map(ResultSet rs) throws SQLException {
            UUID id = getUUID(rs, "id");
            String provider = rs.getString("provider");
            String line = rs.getString("line");
            URI url = createUrl(rs.getString("url"));
            if (ObjectUtils.anyNull(id, provider, line, url)) {
                throw new RuntimeException("Corrupted database record");
            }

            UsernamePasswordCredentials credentials = null;
            String username = rs.getString("username");
            String maybeEncryptedPassword = rs.getString("password");
            if (isNotBlank(username)) {
                String password = encryptor != null ?
                        encryptor.decrypt(maybeEncryptedPassword) :
                        maybeEncryptedPassword;
                credentials = UsernamePasswordCredentials.of(username, password);
            }
            LinetestRequest request = LinetestRequest.of(url, credentials, provider, line);

            MeasurementTask task = new MeasurementTask(id, request);
            task.setDateTime(getLocalDateTime(rs, "datetime"));
            task.setDuration(rs.getInt("duration"));
            task.setConnectionStatus(rs.getInt("connection_status"));

            // negative duration means that task was not finished, either with error or not
            if (task.getDuration() < 0) { return task; }

            Measurement measurement = new Measurement();
            task.setResult(measurement);

            measurement.setTestFailed(rs.getBoolean("test_failed"));
            measurement.setMeasuredValue(arrayToMeasuredValue(ValueType.RESISTANCE, rs.getArray("resistance")));
            measurement.setMeasuredValue(arrayToMeasuredValue(ValueType.CAPACITANCE, rs.getArray("capacitance")));
            measurement.setMeasuredValue(arrayToMeasuredValue(ValueType.DC_VOLTAGE, rs.getArray("dc_voltage")));
            measurement.setMeasuredValue(arrayToMeasuredValue(ValueType.AC_VOLTAGE, rs.getArray("ac_voltage")));
            measurement.setRawOutput(rs.getString("raw_output"));

            String lineStatus = rs.getString("line_status");
            if (lineStatus != null) {
                measurement.setLineStatus(LineStatus.valueOf(lineStatus.toUpperCase()));
            } else {
                measurement.setLineStatus(LineStatus.UNKNOWN);
            }

            return task;
        }

        private MeasuredValue arrayToMeasuredValue(ValueType valueType, Array array) throws SQLException {
            if (array == null) { return new MeasuredValue(valueType, 0, 0, 0); }
            Object[] values = (Object[]) array.getArray();
            return new MeasuredValue(valueType, (double) values[0], (double) values[1], (double) values[2]);
        }

        private @Nullable URI createUrl(String s) {
            try {
                return new URI(s);
            } catch (URISyntaxException e) {
                return null;
            }
        }
    }
}
