package org.telekit.plugins.linetest.provider.impl;

import org.telekit.base.net.connection.ConnectionParams;
import org.telekit.plugins.linetest.domain.LineStatus;
import org.telekit.plugins.linetest.domain.MeasuredValue;
import org.telekit.plugins.linetest.domain.Measurement;
import org.telekit.plugins.linetest.provider.LinetestSession;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import static org.telekit.plugins.linetest.domain.MeasuredValue.ValueType.*;

public class FakeSession implements LinetestSession {

    private static final Logger LOG = Logger.getLogger(FakeSession.class.getName());
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private final ConnectionParams connectionParams;

    public FakeSession(ConnectionParams connectionParams) {
        this.connectionParams = connectionParams;
    }

    @Override
    public void connect() {
        LOG.info("Fake Session started");
        LOG.info("Connecting to " + connectionParams);
    }

    @Override
    public Measurement runTest(String lineId) {
        LOG.info("Running line test for " + lineId);

        if ("42".equals(lineId)) {
            LOG.info("Test failed");
            return createErrorResult();
        }

        if ("43".equals(lineId)) {
            LOG.info("Connection failed");
            throw new RuntimeException("Connection failed");
        }

        try {
            Thread.sleep(getAverageTestDuration().toMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return getErrorResult();
    }

    @Override
    public void disconnect() {
        LOG.info("Disconnecting");
    }

    @Override
    public Duration getAverageTestDuration() {
        return Duration.ofMillis(2_000);
    }

    private Measurement getSuccessResult() {
        Measurement measurement = new Measurement();
        measurement.setTestFailed(false);
        measurement.setLineStatus(LineStatus.ON_HOOK);
        measurement.setMeasuredValue(new MeasuredValue(
                RESISTANCE,
                RANDOM.nextDouble(5e6, 10e6),
                RANDOM.nextDouble(5e6, 10e6),
                RANDOM.nextDouble(5e6, 10e6)
        ));
        measurement.setMeasuredValue(new MeasuredValue(
                CAPACITANCE,
                RANDOM.nextDouble(0.2e-6, 0.5e-6),
                RANDOM.nextDouble(0.2e-6, 0.5e-6),
                RANDOM.nextDouble(0.2e-6, 0.5e-6)
        ));
        measurement.setMeasuredValue(new MeasuredValue(
                AC_VOLTAGE,
                RANDOM.nextDouble(0, 5),
                RANDOM.nextDouble(0, 5),
                RANDOM.nextDouble(0, 5)
        ));
        measurement.setMeasuredValue(new MeasuredValue(
                DC_VOLTAGE,
                RANDOM.nextDouble(0, 5),
                RANDOM.nextDouble(0, 5),
                RANDOM.nextDouble(0, 5)
        ));
        measurement.setRawOutput("""
                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                Nullam ornare purus sagittis lacus blandit volutpat.
                Quisque imperdiet interdum quam, vitae varius odio placerat et.

                Suspendisse feugiat mauris quis augue dignissim, at bibendum urna dignissim.
                Nullam vestibulum libero purus, ac congue lacus vehicula et.

                Nunc ut nisi at justo fermentum dignissim at quis est.
                Phasellus pharetra semper dui, nec condimentum quam malesuada a.
                Pellentesque maximus ex vel tortor vehicula congue.
                """);
        return measurement;
    }

    private Measurement getErrorResult() {
        Measurement measurement = new Measurement();
        measurement.setTestFailed(false);
        measurement.setLineStatus(LineStatus.ON_HOOK);
        measurement.setMeasuredValue(new MeasuredValue(
                RESISTANCE,
                RANDOM.nextDouble(0.1e6, 1e6),
                RANDOM.nextDouble(0.1e6, 1e6),
                RANDOM.nextDouble(0.1e6, 1e6)
        ));
        measurement.setMeasuredValue(new MeasuredValue(
                CAPACITANCE,
                RANDOM.nextDouble(0, 100e-9),
                RANDOM.nextDouble(0, 100e-9),
                RANDOM.nextDouble(0, 100e-9)
        ));
        measurement.setMeasuredValue(new MeasuredValue(
                AC_VOLTAGE,
                RANDOM.nextDouble(5, 45),
                RANDOM.nextDouble(5, 45),
                RANDOM.nextDouble(5, 45)
        ));
        measurement.setMeasuredValue(new MeasuredValue(
                DC_VOLTAGE,
                RANDOM.nextDouble(5, 45),
                RANDOM.nextDouble(5, 45),
                RANDOM.nextDouble(5, 45)
        ));
        measurement.setRawOutput("""
                Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                Nullam ornare purus sagittis lacus blandit volutpat.
                Quisque imperdiet interdum quam, vitae varius odio placerat et.

                Suspendisse feugiat mauris quis augue dignissim, at bibendum urna dignissim.
                Nullam vestibulum libero purus, ac congue lacus vehicula et.

                Nunc ut nisi at justo fermentum dignissim at quis est.
                Phasellus pharetra semper dui, nec condimentum quam malesuada a.
                Pellentesque maximus ex vel tortor vehicula congue.
                """);
        return measurement;
    }

    private Measurement createErrorResult() {
        Measurement measurement = new Measurement();
        measurement.setTestFailed(true);
        measurement.setRawOutput("""
                Test is failed.
                Do something as soon as possible.
                """);
        return measurement;
    }
}
