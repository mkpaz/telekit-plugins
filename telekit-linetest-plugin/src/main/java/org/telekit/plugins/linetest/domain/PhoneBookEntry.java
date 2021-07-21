package org.telekit.plugins.linetest.domain;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PhoneBookEntry {

    private final String phoneNumber;
    private final LinetestRequest request;
    private final String description;

    public PhoneBookEntry(String phoneNumber, LinetestRequest request, @Nullable String description) {
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.request = Objects.requireNonNull(request);
        this.description = description;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LinetestRequest getRequest() {
        return request;
    }

    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "PhoneBookEntry{" +
                "phoneNumber='" + phoneNumber + '\'' +
                ", request=" + request +
                ", description='" + description + '\'' +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////

    public String getProvider() {
        return request.getProvider();
    }

    public String getLine() {
        return request.getLine();
    }
}
