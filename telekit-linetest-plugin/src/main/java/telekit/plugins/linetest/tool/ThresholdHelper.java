package telekit.plugins.linetest.tool;

import org.apache.commons.lang3.math.NumberUtils;
import telekit.plugins.linetest.domain.MeasuredValue;
import telekit.plugins.linetest.domain.MeasurementTask;
import telekit.plugins.linetest.domain.ValueRange;
import telekit.plugins.linetest.provider.LinetestProvider;

import static telekit.plugins.linetest.domain.MeasuredValue.ValueType;

final class ThresholdHelper {

    static final int NO_ERROR = 0;
    static final int ERROR_LEVEL_WARN = 1;
    static final int ERROR_LEVEL_CRIT = 2;

    private final ProviderRegistry providerRegistry;
    private LinetestProvider provider;

    public ThresholdHelper(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public void update(MeasurementTask task) {
        provider = providerRegistry.get(task.getProvider());
    }

    public ValueRange getValueRange(ValueType valueType, boolean tipRing) {
        return provider.getValueRange(valueType, tipRing);
    }

    public int getErrorLevel(ValueType valueType, double value, boolean tipRing) {
        ValueRange r = getValueRange(valueType, tipRing);
        if (Math.abs(value) > r.getCritHigh()) { return ERROR_LEVEL_CRIT; }
        if (Math.abs(value) > r.getHigh()) { return ERROR_LEVEL_WARN; }
        if (Math.abs(value) < r.getCritLow()) { return ERROR_LEVEL_CRIT; }
        if (Math.abs(value) < r.getLow()) { return ERROR_LEVEL_WARN; }
        return NO_ERROR;
    }

    public boolean checkWarn(ValueType valueType, double value, boolean tipRing) {
        return getErrorLevel(valueType, value, tipRing) == ERROR_LEVEL_WARN;
    }

    public boolean checkCrit(ValueType valueType, double value, boolean tipRing) {
        return getErrorLevel(valueType, value, tipRing) == ERROR_LEVEL_CRIT;
    }

    public int getMaxErrorLevel(MeasuredValue value) {
        return NumberUtils.max(
                getErrorLevel(value.getValueType(), value.getTipRing(), true),
                getErrorLevel(value.getValueType(), value.getTipGround(), false),
                getErrorLevel(value.getValueType(), value.getRingGround(), false)
        );
    }
}