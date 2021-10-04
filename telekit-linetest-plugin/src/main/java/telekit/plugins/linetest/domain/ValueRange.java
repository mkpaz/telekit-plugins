package telekit.plugins.linetest.domain;

import static java.lang.Double.MAX_VALUE;

public class ValueRange {

    public static final ValueRange INFINITE = new ValueRange(-MAX_VALUE, MAX_VALUE, -MAX_VALUE, MAX_VALUE);

    private final double low;
    private final double high;
    private final double critLow;
    private final double critHigh;

    public ValueRange(double low, double high, double critLow, double critHigh) {
        if (low >= high) {
            throw new IllegalArgumentException(String.format("Invalid range bounds: %f >= %f", low, high));
        }

        if (critLow > low) {
            throw new IllegalArgumentException(String.format("Invalid lower bound: %f > %f", critLow, low));
        }

        if (critHigh < high) {
            throw new IllegalArgumentException(String.format("Invalid upper bound: %f < %f", critHigh, high));
        }

        this.low = low;
        this.high = high;
        this.critLow = critLow;
        this.critHigh = critHigh;
    }

    public double getLow() {
        return low;
    }

    public double getHigh() {
        return high;
    }

    public double getCritLow() {
        return critLow;
    }

    public double getCritHigh() {
        return critHigh;
    }

    @Override
    public String toString() {
        return "ValueRange{" +
                "low=" + low +
                ", high=" + high +
                ", critLow=" + critLow +
                ", critHigh=" + critHigh +
                '}';
    }
}
