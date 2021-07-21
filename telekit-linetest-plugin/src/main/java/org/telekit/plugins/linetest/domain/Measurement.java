package org.telekit.plugins.linetest.domain;

import org.jetbrains.annotations.Nullable;
import org.telekit.plugins.linetest.domain.MeasuredValue.ValueType;

import java.util.*;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class Measurement {

    private boolean testFailed;
    private LineStatus lineStatus = LineStatus.UNKNOWN;
    private Map<ValueType, MeasuredValue> measuredValues = new HashMap<>();
    private String rawOutput;

    public Measurement() {}

    public boolean isTestFailed() {
        return testFailed;
    }

    public void setTestFailed(boolean testFailed) {
        this.testFailed = testFailed;
    }

    public LineStatus getLineStatus() {
        return lineStatus;
    }

    public void setLineStatus(LineStatus lineStatus) {
        this.lineStatus = Objects.requireNonNull(lineStatus);
    }

    public Map<ValueType, MeasuredValue> getMeasuredValues() {
        return measuredValues;
    }

    public void setMeasuredValues(Map<ValueType, MeasuredValue> measuredValues) {
        this.measuredValues = defaultIfNull(measuredValues, new HashMap<>());
    }

    public @Nullable String getRawOutput() {
        return rawOutput;
    }

    public void setRawOutput(@Nullable String rawOutput) {
        this.rawOutput = rawOutput;
    }

    @Override
    public String toString() {
        return "Measurement{\n" +
                "\ttestFailed=" + testFailed + "\n" +
                "\tlineStatus=" + lineStatus + "\n" +
                "\tresistance=" + getMeasuredValue(ValueType.RESISTANCE) + "\n" +
                "\tcapacitance=" + getMeasuredValue(ValueType.CAPACITANCE) + "\n" +
                "\tacVoltage=" + getMeasuredValue(ValueType.DC_VOLTAGE) + "\n" +
                "\tdcVoltage=" + getMeasuredValue(ValueType.AC_VOLTAGE) + "\n" +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////

    public @Nullable MeasuredValue getMeasuredValue(ValueType valueType) {
        return measuredValues.get(valueType);
    }

    public void setMeasuredValue(MeasuredValue value) {
        Objects.requireNonNull(value);
        measuredValues.put(value.getValueType(), value);
    }

    public List<MeasuredValue> getMeasuredValuesAsList() {
        List<MeasuredValue> list = new ArrayList<>(measuredValues.values());
        Collections.sort(list);
        return list;
    }
}
