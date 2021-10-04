package telekit.plugins.linetest.database;

import org.flywaydb.core.Flyway;
import org.hsqldb.jdbc.JDBCDataSource;
import telekit.base.Env;
import telekit.base.service.jdbc.JdbcTransactionManager;
import telekit.base.service.jdbc.TransactionManager;
import telekit.plugins.linetest.LinetestPlugin;
import telekit.plugins.linetest.database.migration.V0__Initial;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.logging.Logger;

public class JdbcStore {

    private static final Logger LOG = Logger.getLogger(JdbcStore.class.getName());
    private static final String DATABASE_FILE_NAME = "database/sql";

    private final JdbcTransactionManager tm;
    private final DataSource rawDataSource;
    private final DataSource txDataSource;

    public JdbcStore(DataSource dataSource) {
        this.tm = new JdbcTransactionManager(dataSource);
        this.rawDataSource = dataSource;
        this.txDataSource = tm.getManagedDataSource();
    }

    public void migrate() {
        LOG.info("Starting database migration: java");

        // While Flyway doesn't have JPMS support we can't load file-based
        // SQL migrations from module path, because application plugin modules
        // are separate JARs and Flyway doesn't support jar:file:/ URLs.
        Flyway flyway = Flyway.configure()
                .dataSource(rawDataSource)
                .javaMigrations(new V0__Initial())
                .load();

        flyway.migrate();

        LOG.info("Database migration finished");
    }

    public TransactionManager transactionManager() {
        return tm;
    }

    public MeasurementTaskRepository measurementTaskRepository() {
        return new MeasurementTaskRepository(getDataSource());
    }

    public DataSource getDataSource() {
        return txDataSource;
    }

    public static JdbcStore createDefault() {
        return new JdbcStore(createDefaultDataSource());
    }

    public static DataSource createDefaultDataSource() {
        System.setProperty("hsqldb.reconfig_logging", "false");

        Path databasePath = Env.isDevMode() ?
                Env.DATA_DIR.resolve("linetest/" + DATABASE_FILE_NAME) :
                Env.getPluginDataDir(LinetestPlugin.class).resolve(DATABASE_FILE_NAME);

        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setDatabase("jdbc:hsqldb:file:" + databasePath.toAbsolutePath());
        return dataSource;
    }
}
