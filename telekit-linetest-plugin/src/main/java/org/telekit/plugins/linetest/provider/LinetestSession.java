package org.telekit.plugins.linetest.provider;

import org.telekit.plugins.linetest.domain.Measurement;

import java.time.Duration;

public interface LinetestSession extends AutoCloseable {

    void connect();

    Measurement runTest(String lineId);

    void disconnect();

    Duration getAverageTestDuration();

    @Override
    default void close() {
        disconnect();
    }
}
