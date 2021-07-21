package org.telekit.plugins.linetest.provider.impl;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.telekit.plugins.linetest.domain.LineStatus;
import org.telekit.plugins.linetest.domain.MeasuredValue;
import org.telekit.plugins.linetest.domain.Measurement;

import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.telekit.plugins.linetest.domain.MeasuredValue.ValueType;

public class IskratelSAKOutputParser {

    private static final Logger LOG = Logger.getLogger(IskratelSAKOutputParser.class.getName());

    public Measurement parse(String output) {
        // assume parsing is failed by default
        Measurement measurement = new Measurement();
        measurement.setTestFailed(true);
        if (isBlank(output)) { return measurement; }

        // remove ANSI escape sequences, in particular DECSC, DECRC
        measurement.setRawOutput(cleanupOutput(output));

        try {
            if (output.contains("Line is in active mode")) {
                measurement.setLineStatus(LineStatus.CONNECTED);
                return measurement;
            }

            if (output.contains("Out Of Tolerance")) {
                measurement.setLineStatus(LineStatus.UNKNOWN);
                return measurement;
            }

            if (output.contains("ON-HOOK")) {
                measurement.setLineStatus(LineStatus.ON_HOOK);
            }

            if (output.contains("OFF-HOOK")) {
                measurement.setLineStatus(LineStatus.OFF_HOOK);
            }

            // filter
            output = output.substring(output.indexOf("Measurement"), output.indexOf("Press ENTER"));
            output = output.replaceAll("> 1000 KE", "10000000 Ohm");
            output = output.replaceAll("> 5000 KE", "10000000 Ohm");
            output = output.replaceAll("< 10 KE", "0.0 Ohm");
            output = output.replaceAll("< 10 nF", "0.000 nF");
            output = output.replaceAll("KE", "KOhm");
            output = output.replaceAll("[<>()]", " ");
        } catch (StringIndexOutOfBoundsException e) {
            return measurement;
        }

        double[] resistance = new double[3];
        double[] capacitance = new double[3];
        double[] dcVoltage = new double[3];
        double[] acVoltage = new double[3];

        // parse params
        for (String line : output.split("\n")) {
            line = line.trim();

            if (line.contains("Uab")) {
                if (line.contains("~")) { acVoltage[0] = parseValue(ValueType.AC_VOLTAGE, line); }
                if (line.contains("=")) { dcVoltage[0] = parseValue(ValueType.DC_VOLTAGE, line); }
            }
            if (line.contains("Uag")) {
                if (line.contains("~")) { acVoltage[1] = parseValue(ValueType.AC_VOLTAGE, line); }
                if (line.contains("=")) { dcVoltage[1] = parseValue(ValueType.DC_VOLTAGE, line); }
            }
            if (line.contains("Ubg")) {
                if (line.contains("~")) { acVoltage[2] = parseValue(ValueType.AC_VOLTAGE, line); }
                if (line.contains("=")) { dcVoltage[2] = parseValue(ValueType.DC_VOLTAGE, line); }
            }

            if (line.contains("Rab")) { resistance[0] = parseValue(ValueType.RESISTANCE, line); }
            if (line.contains("Rag")) { resistance[1] = parseValue(ValueType.RESISTANCE, line); }
            if (line.contains("Rbg")) { resistance[2] = parseValue(ValueType.RESISTANCE, line); }

            if (line.contains("Cab")) { capacitance[0] = parseValue(ValueType.CAPACITANCE, line); }
            if (line.contains("Cag")) { capacitance[1] = parseValue(ValueType.CAPACITANCE, line); }
            if (line.contains("Cbg")) { capacitance[2] = parseValue(ValueType.CAPACITANCE, line); }
        }

        measurement.setMeasuredValue(createMeasuredValue(ValueType.RESISTANCE, resistance));
        measurement.setMeasuredValue(createMeasuredValue(ValueType.CAPACITANCE, capacitance));
        measurement.setMeasuredValue(createMeasuredValue(ValueType.DC_VOLTAGE, dcVoltage));
        measurement.setMeasuredValue(createMeasuredValue(ValueType.AC_VOLTAGE, acVoltage));
        measurement.setTestFailed(false);

        return measurement;
    }

    static String cleanupOutput(String output) {
        return output
                // remove escape codes
                .replaceAll("\u001B\\d|\u001B\\[(\\d{1,2}(;[\\d]{1,2})?)?[mC]", "")
                // restore input command
                .replaceAll("^start_test", "> start_test")
                .trim();
    }

    private double parseValue(ValueType valueType, String s) {
        if (isBlank(s)) { return 0; }

        String[] chunks = s.split(":");
        if (chunks.length != 2) {
            LOG.warning("Unexpected output format. Unable to parse measured value: " + s);
            return 0;
        }

        String valueAndUnitStr = trim(chunks[1]);
        int unitPos = valueAndUnitStr.indexOf(" ");
        if (unitPos < 0) {
            LOG.warning("Unexpected output format. Unable to parse measured value: " + s);
            return 0;
        }

        String valueStr = trim(valueAndUnitStr.substring(0, unitPos));
        String unitStr = trim(valueAndUnitStr.substring(unitPos).toLowerCase());

        try {
            double value = Double.parseDouble(valueStr);

            if (unitStr.contains("nf")) { return value * 1e-9; }
            if (unitStr.contains("uf") | unitStr.contains("mf")) { return value * 1e-6; }
            if (unitStr.contains("megohm") | unitStr.contains("mohm")) { return value * 1e6; }
            if (unitStr.contains("kilohm") | unitStr.contains("kohm")) { return value * 1e3; }

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