package org.telekit.plugins.linetest.provider.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.telekit.plugins.linetest.domain.LineStatus;
import org.telekit.plugins.linetest.domain.MeasuredValue;
import org.telekit.plugins.linetest.domain.Measurement;

import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class HuaweiMA5600OutputParserTest {

    private final HuaweiMA5600OutputParser parser = new HuaweiMA5600OutputParser();

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testParsingSuccessfulLinetestResult() throws Exception {
        String output = IOUtils.toString(Objects.requireNonNull(getClass().getResource("/output/ma5600_success.txt")), UTF_8);
        Measurement measurement = parser.parse(output);

        System.out.println(measurement);
        System.out.println(measurement.getRawOutput());

        assertFalse(measurement.isTestFailed());
        assertEquals(measurement.getLineStatus(), LineStatus.ON_HOOK);
        assertEquals(0, measurement.getMeasuredValue(MeasuredValue.ValueType.AC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(0, measurement.getMeasuredValue(MeasuredValue.ValueType.DC_VOLTAGE).getTipRing(), 0.0);
        assertEquals(5e6, measurement.getMeasuredValue(MeasuredValue.ValueType.RESISTANCE).getTipRing(), 0.0);
        assertEquals(1.076e-6, measurement.getMeasuredValue(MeasuredValue.ValueType.CAPACITANCE).getTipRing(), 1e-15);
    }
}