package org.telekit.plugins.linetest.provider.impl;

import org.jetbrains.annotations.Nullable;
import org.telekit.base.domain.security.UsernamePasswordCredentials;
import org.telekit.base.net.connection.ConnectionParams;

public final class SessionHelper {

    public static @Nullable UsernamePasswordCredentials getUsernamePasswordCredentials(ConnectionParams params) {
        return params.getCredentials() instanceof UsernamePasswordCredentials userPassword ? userPassword : null;
    }

    public static void requireUsernamePasswordCredentials(ConnectionParams params) {
        UsernamePasswordCredentials userPassword = getUsernamePasswordCredentials(params);
        if (userPassword == null || userPassword.getPasswordAsString() == null) {
            throw new IllegalArgumentException("Invalid credentials");
        }
    }
}
