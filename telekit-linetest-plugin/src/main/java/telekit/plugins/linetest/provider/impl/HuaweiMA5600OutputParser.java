package telekit.plugins.linetest.provider.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import telekit.base.domain.LineSeparator;
import telekit.plugins.linetest.domain.LineStatus;
import telekit.plugins.linetest.domain.MeasuredValue;
import telekit.plugins.linetest.domain.MeasuredValue.ValueType;
import telekit.plugins.linetest.domain.Measurement;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class HuaweiMA5600OutputParser {

    private static final Logger LOG = Logger.getLogger(HuaweiMA5600OutputParser.class.getName());
    private static final int PARSED_PARAM_COUNT = 12;

    public Measurement parse(String output) {
        // assume parsing is failed by default
        Measurement measurement = new Measurement();
        measurement.setTestFailed(true);
        if (isBlank(output)) { return measurement; }

        measurement.setRawOutput(cleanupOutput(output));

        // no expected result found, return but keep raw output
        if (!output.contains("rimary test result")) {
            return measurement;
        }

        // parse measured values

        double[] resistance = new double[3];
        double[] capacitance = new double[3];
        double[] dcVoltage = new double[3];
        double[] acVoltage = new double[3];

        AtomicInteger valueCount = new AtomicInteger(0);

        for (String line : output.split(LineSeparator.LINE_SPLIT_PATTERN)) {
            line = line.trim();

            if (line.contains("Terminal status")) {
                if (line.contains("on hook")) { measurement.setLineStatus(LineStatus.ON_HOOK); }
            }

            if (line.contains("A->B insulation resistance(low)")) { resistance[0] = parseValue(line, valueCount); }
            if (line.contains("A->ground insulation resistance")) { resistance[1] = parseValue(line, valueCount); }
            if (line.contains("B->ground insulation resistance")) { resistance[2] = parseValue(line, valueCount); }

            if (line.contains("A->B capacitance(low)")) { capacitance[0] = parseValue(line, valueCount); }
            if (line.contains("A->ground capacitance")) { capacitance[1] = parseValue(line, valueCount); }
            if (line.contains("B->ground capacitance")) { capacitance[2] = parseValue(line, valueCount); }

            if (line.contains("A->B DC voltage")) { dcVoltage[0] = parseValue(line, valueCount); }
            if (line.contains("A->G DC voltage")) { dcVoltage[1] = parseValue(line, valueCount); }
            if (line.contains("B->G DC voltage")) { dcVoltage[2] = parseValue(line, valueCount); }

            if (line.contains("A->B AC voltage")) { acVoltage[0] = parseValue(line, valueCount); }
            if (line.contains("A->G AC voltage")) { acVoltage[1] = parseValue(line, valueCount); }
            if (line.contains("B->G AC voltage")) { acVoltage[2] = parseValue(line, valueCount); }
        }

        measurement.setMeasuredValue(createMeasuredValue(ValueType.RESISTANCE, resistance));
        measurement.setMeasuredValue(createMeasuredValue(ValueType.CAPACITANCE, capacitance));
        measurement.setMeasuredValue(createMeasuredValue(ValueType.DC_VOLTAGE, dcVoltage));
        measurement.setMeasuredValue(createMeasuredValue(ValueType.AC_VOLTAGE, acVoltage));

        // test is successful when no exceptions raised and all values parsed
        if (valueCount.intValue() == PARSED_PARAM_COUNT) {
            measurement.setTestFailed(false);
        }

        return measurement;
    }

    String cleanupOutput(String output) {
        return output
                // remove empty prompt lines
                .replaceAll("(?m)^.*\\(config-test\\)#$", "")
                .trim();
    }

    private double parseValue(String s, AtomicInteger counter) {
        if (isBlank(s)) { return 0; }

        String[] chunks = s.split("\\s{2,}|\\t{1,}");

        try {
            String strValue = chunks[1];
            strValue = strValue.replaceAll("[<>]", "+");
            double value = Double.parseDouble(strValue);

            String unit = chunks[2];
            switch (unit.toLowerCase()) {
                case "nf" -> value = value * 1e-9;
                case "uf", "mf" -> value = value * 1e-6;
                case "megohm", "mohm" -> value = value * 1e6;
                case "kilohm", "kohm" -> value = value * 1e3;
            }

            counter.getAndIncrement();

            return value;
        } catch (Exception e) {
            LOG.warning("Unexpected output format. Unable to parse measured value: " + s);
            LOG.warning(ExceptionUtils.getStackTrace(e));
            return 0;
        }
    }

    private MeasuredValue createMeasuredValue(ValueType valueType, double[] values) {
        return new MeasuredValue(valueType, values[0], values[1], values[2]);
    }
}
