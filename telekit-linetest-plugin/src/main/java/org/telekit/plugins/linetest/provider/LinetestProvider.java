package org.telekit.plugins.linetest.provider;

import javafx.scene.Node;
import org.apache.commons.collections4.IterableUtils;
import org.jetbrains.annotations.Nullable;
import org.telekit.base.net.connection.ConnectionParams;
import org.telekit.base.net.connection.Scheme;
import org.telekit.plugins.linetest.domain.Equipment;
import org.telekit.plugins.linetest.domain.ValueRange;

import java.util.Set;

import static java.lang.Double.MAX_VALUE;
import static org.telekit.plugins.linetest.domain.MeasuredValue.ValueType;

public interface LinetestProvider {

    ValueRange RESISTANCE_RANGE = new ValueRange(1e6, MAX_VALUE, 0.5e6, MAX_VALUE);
    ValueRange CAPACITANCE_RANGE = new ValueRange(0.1e-6, 1.5e-6, 0.02e-6, 3e-6);
    ValueRange VOLTAGE_RANGE = new ValueRange(-MAX_VALUE, 5, -MAX_VALUE, 10);

    String getId();

    @Nullable String getDescription();

    @Nullable Node getIcon();

    Equipment getSupportedEquipment();

    LinetestSession createSession(ConnectionParams params);

    Set<Scheme> getSupportedConnections();

    ConnectionParams getDefaultConnectionParams(Scheme scheme);

    default ValueRange getValueRange(ValueType valueType, boolean tipRing) {
        return switch (valueType) {
            case RESISTANCE -> RESISTANCE_RANGE;
            case CAPACITANCE -> tipRing ? CAPACITANCE_RANGE : ValueRange.INFINITE;
            case AC_VOLTAGE, DC_VOLTAGE -> VOLTAGE_RANGE;
        };
    }

    default boolean supports(Scheme scheme) {
        return scheme != null && getSupportedConnections().contains(scheme);
    }

    default boolean supports(ConnectionParams params) {
        return params != null && supports(params.getScheme());
    }

    default ConnectionParams getDefaultConnectionParams() {
        return getDefaultConnectionParams(IterableUtils.get(getSupportedConnections(), 0));
    }
}
