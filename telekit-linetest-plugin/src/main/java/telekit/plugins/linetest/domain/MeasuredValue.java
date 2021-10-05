package telekit.plugins.linetest.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MeasuredValue implements Comparable<MeasuredValue> {

    public enum ValueType {
        AC_VOLTAGE("AC Voltage", "V"),
        DC_VOLTAGE("DC Voltage", "V"),
        RESISTANCE("Resistance", "Ohm"),
        CAPACITANCE("Capacitance", "F");

        private final String title;
        private final String unit;

        ValueType(String title, String unit) {
            this.title = title;
            this.unit = unit;
        }

        public String getTitle() {
            return title;
        }

        public String getUnit() {
            return unit;
        }
    }

    private final ValueType valueType;
    private final double tipRing;    // A-B
    private final double tipGround;  // A-G
    private final double ringGround; // B-G

    public MeasuredValue(ValueType valueType, double tipRing, double tipGround, double ringGround) {
        this.valueType = Objects.requireNonNull(valueType);
        this.tipRing = tipRing;
        this.tipGround = tipGround;
        this.ringGround = ringGround;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public double getTipRing() {
        return tipRing;
    }

    public double getTipGround() {
        return tipGround;
    }

    public double getRingGround() {
        return ringGround;
    }

    public double[] toArray() {
        return new double[]{tipRing, tipGround, ringGround};
    }

    @Override
    public int compareTo(@NotNull MeasuredValue that) {
        return Integer.compare(
                this.getValueType().ordinal(),
                that.getValueType().ordinal()
        );
    }

    @Override
    public String toString() {
        return String.format("%s: A-B=%.3E, A-G=%.3E, B-G=%.3E", valueType, tipRing, tipGround, ringGround);
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String format(ValueType valueType, double value) {
        return switch (valueType) {
            case RESISTANCE -> String.format("%.0f K%s", value / 1e3, valueType.getUnit());  // KOhm
            case CAPACITANCE -> String.format("%.2f u%s", value * 1e6, valueType.getUnit()); // uF
            case AC_VOLTAGE -> String.format("%.1f %s", value, valueType.getUnit());
            case DC_VOLTAGE -> String.format("%.1f %s", value, valueType.getUnit());
        };
    }
}
