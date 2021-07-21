package org.telekit.plugins.linetest.domain;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Equipment {

    private final String vendor;
    private final String model;
    private final String version;

    public Equipment(String vendor, String model, @Nullable String version) {
        this.vendor = Objects.requireNonNull(vendor);
        this.model = Objects.requireNonNull(model);
        this.version = version;
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }

    public @Nullable String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Equipment{" +
                "vendor='" + vendor + '\'' +
                ", model='" + model + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public String printInOneLine() {
        return vendor + " " + model + (version != null ? " v" + version : "");
    }
}
