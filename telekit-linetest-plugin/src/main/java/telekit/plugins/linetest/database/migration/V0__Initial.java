package telekit.plugins.linetest.database.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V0__Initial extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String sql = """
                CREATE TABLE measurement_task
                (
                    id                  UUID NOT NULL PRIMARY KEY,
                    datetime            TIMESTAMP WITH TIME ZONE NOT NULL,
                    provider            VARCHAR(64),
                    line                VARCHAR(64),
                    url                 VARCHAR(256),
                    username            VARCHAR(64),
                    password            VARCHAR(64),
                    duration            INTEGER,
                    connection_status   INTEGER,
                    test_failed         BOOLEAN DEFAULT FALSE,
                    line_status         VARCHAR(16),
                    resistance          DOUBLE ARRAY,
                    capacitance         DOUBLE ARRAY,
                    dc_voltage          DOUBLE ARRAY,
                    ac_voltage          DOUBLE ARRAY,
                    raw_output          VARCHAR(65535)
                );
                """;

        try (Statement statement = context.getConnection().createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
