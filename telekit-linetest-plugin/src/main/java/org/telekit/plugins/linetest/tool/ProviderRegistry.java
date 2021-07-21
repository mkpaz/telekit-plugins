package org.telekit.plugins.linetest.tool;

import org.jetbrains.annotations.Nullable;
import org.telekit.base.Env;
import org.telekit.plugins.linetest.domain.Equipment;
import org.telekit.plugins.linetest.provider.LinetestProvider;
import org.telekit.plugins.linetest.provider.impl.FakeLinetestProvider;
import org.telekit.plugins.linetest.provider.impl.HuaweiMA5600Provider;
import org.telekit.plugins.linetest.provider.impl.IskratelSAKProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public class ProviderRegistry {

    private final TreeMap<String, LinetestProvider> providers = new TreeMap<>();

    public ProviderRegistry() {}

    public void register(LinetestProvider provider) {
        providers.put(
                Objects.requireNonNull(provider.getId()),
                Objects.requireNonNull(provider)
        );
    }

    public void unregister(String id) {
        providers.remove(id);
    }

    public List<LinetestProvider> getAll() {
        return new ArrayList<>(providers.values());
    }

    public @Nullable LinetestProvider get(String id) {
        return providers.get(id);
    }

    public LinetestProvider getOrDefault(String id, LinetestProvider defaultProvider) {
        return providers.getOrDefault(id, Objects.requireNonNull(defaultProvider));
    }

    public boolean isEmpty() {
        return providers.isEmpty();
    }

    public @Nullable Equipment findEquipmentById(String id) {
        if (id == null) { return null; }
        LinetestProvider provider = providers.get(id);
        return provider != null ? provider.getSupportedEquipment() : null;
    }

    public @Nullable LinetestProvider getDefaultProvider() {
        return !isEmpty() ? providers.firstEntry().getValue() : null;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static ProviderRegistry withDefaultProviders() {
        ProviderRegistry registry = new ProviderRegistry();
        if (Env.isDevMode()) {
            registry.register(new FakeLinetestProvider());
        }
        registry.register(new HuaweiMA5600Provider());
        registry.register(new IskratelSAKProvider());
        return registry;
    }
}
