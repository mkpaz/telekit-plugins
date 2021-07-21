package org.telekit.plugins.linetest;

import org.telekit.base.di.DependencyModule;
import org.telekit.base.di.Provides;
import org.telekit.plugins.linetest.database.JdbcStore;

import javax.inject.Singleton;
import javax.sql.DataSource;

public class LinetestDependencyModule implements DependencyModule {

    private final JdbcStore jdbcStore;

    public LinetestDependencyModule(JdbcStore jdbcStore) {
        this.jdbcStore = jdbcStore;
    }

    @Provides
    @Singleton
    public DataSource dataSource() {
        return jdbcStore.getDataSource();
    }

    @Provides
    @Singleton
    public JdbcStore jdbcStore() {
        return jdbcStore;
    }
}