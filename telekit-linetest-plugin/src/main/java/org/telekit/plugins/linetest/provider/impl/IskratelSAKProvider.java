package org.telekit.plugins.linetest.provider.impl;

import javafx.scene.Node;
import org.telekit.base.domain.security.UsernamePasswordCredentials;
import org.telekit.base.net.connection.BaseConnectionParams;
import org.telekit.base.net.connection.ConnectionParams;
import org.telekit.base.net.connection.Scheme;
import org.telekit.plugins.linetest.domain.Equipment;
import org.telekit.plugins.linetest.provider.LinetestProvider;
import org.telekit.plugins.linetest.provider.LinetestSession;

import java.util.Set;

import static org.telekit.base.net.connection.Scheme.TELNET;

public class IskratelSAKProvider implements LinetestProvider {

    static final Equipment SUPPORTED_EQ = new Equipment("Iskratel", "SAK", null);
    static final Set<Scheme> SUPPORTED_CONNECTIONS = Set.of(TELNET);

    @Override
    public String getId() {
        return "ISKRATEL_SAK";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Node getIcon() {
        return null;
    }

    @Override
    public Equipment getSupportedEquipment() {
        return SUPPORTED_EQ;
    }

    @Override
    public LinetestSession createSession(ConnectionParams params) {
        if (params.getScheme() == TELNET) {
            return new IskratelSAKTelnetSession(params);
        } else {
            throw new IllegalArgumentException("Unsupported connection type");
        }
    }

    @Override
    public Set<Scheme> getSupportedConnections() {
        return SUPPORTED_CONNECTIONS;
    }

    @Override
    public ConnectionParams getDefaultConnectionParams(Scheme scheme) {
        if (scheme == TELNET) {
            return new BaseConnectionParams(TELNET, "192.168.1.1", 23, UsernamePasswordCredentials.of("sysadmin", "sysadmin"));
        }
        throw new IllegalArgumentException("Unsupported connection type: " + scheme);
    }
}
