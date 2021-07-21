package org.telekit.plugins.linetest.tool;

import org.telekit.plugins.linetest.domain.MeasurementTask;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RunningTaskRegistry {

    private final Map<UUID, MeasurementTask> registry = new ConcurrentHashMap<>();

    public void put(MeasurementTask task) {
        registry.put(task.getId(), Objects.requireNonNull(task));
    }

    public boolean contains(MeasurementTask task) {
        return task != null && registry.containsKey(task.getId());
    }

    public void remove(MeasurementTask task) {
        Objects.requireNonNull(task);
        registry.remove(task.getId());
    }

    public boolean isHostInUse(String host) {
        return host != null && registry.values().stream().anyMatch(task ->
                task.getConnectionParams() != null && Objects.equals(host, task.getConnectionParams().getHost())
        );
    }
}
