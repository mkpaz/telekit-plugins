package telekit.plugins.linetest.provider.impl;

import javafx.scene.Node;
import telekit.base.domain.security.Credentials;
import telekit.base.domain.security.UsernamePasswordCredentials;
import telekit.base.net.connection.BaseConnectionParams;
import telekit.base.net.connection.ConnectionParams;
import telekit.base.net.connection.Scheme;
import telekit.plugins.linetest.domain.Equipment;
import telekit.plugins.linetest.provider.LinetestProvider;
import telekit.plugins.linetest.provider.LinetestSession;

import java.util.Set;

import static telekit.base.net.connection.Scheme.*;

public class FakeLinetestProvider implements LinetestProvider {

    static final Equipment SUPPORTED_EQ = new Equipment("Fake", "Equipment", "1.0.0");
    static final Set<Scheme> SUPPORTED_CONNECTIONS = Set.of(SSH, TELNET, HTTP);

    @Override
    public String getId() {
        return "FAKE";
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
        return new FakeSession(params);
    }

    @Override
    public Set<Scheme> getSupportedConnections() {
        return SUPPORTED_CONNECTIONS;
    }

    @Override
    public ConnectionParams getDefaultConnectionParams(Scheme scheme) {
        Credentials credentials = UsernamePasswordCredentials.of("admin", "admin");
        return switch (scheme) {
            case SSH -> new BaseConnectionParams(scheme, "192.168.10.1", 10, credentials);
            case TELNET -> new BaseConnectionParams(scheme, "192.168.20.1", 20, credentials);
            case HTTP -> new BaseConnectionParams(scheme, "192.168.30.1", 30, credentials);
            default -> throw new IllegalArgumentException("Unsupported connection type: " + scheme);
        };
    }
}
