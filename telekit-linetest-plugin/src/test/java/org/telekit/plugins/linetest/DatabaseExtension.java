package org.telekit.plugins.linetest;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.telekit.plugins.linetest.database.JdbcStore;
import org.telekit.test.HSQLMemoryDatabaseResolver;

import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseExtension extends HSQLMemoryDatabaseResolver implements BeforeAllCallback {

    private final JdbcStore store;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public DatabaseExtension() {
        super();
        store = new JdbcStore(dataSource);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type == JdbcStore.class || super.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        if (type == JdbcStore.class) { return store; }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        if (initialized.compareAndSet(false, true)) {
            store.migrate();
        }
    }
}
