package telekit.plugins.linetest.provider.impl;

import javafx.scene.Node;
import telekit.base.domain.security.UsernamePasswordCredentials;
import telekit.base.net.connection.BaseConnectionParams;
import telekit.base.net.connection.ConnectionParams;
import telekit.base.net.connection.Scheme;
import telekit.plugins.linetest.domain.Equipment;
import telekit.plugins.linetest.provider.LinetestProvider;
import telekit.plugins.linetest.provider.LinetestSession;

import java.util.Set;

import static telekit.base.net.connection.Scheme.SSH;

public class HuaweiMA5600Provider implements LinetestProvider {

    static final Equipment SUPPORTED_EQ = new Equipment("Huawei", "MA5600", "800.18.*");
    static final Set<Scheme> SUPPORTED_CONNECTIONS = Set.of(SSH);

    @Override
    public String getId() {
        return "HUAWEI_MA5600";
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
        if (params.getScheme() == SSH) {
            return new HuaweiMA5600SSHSession(params);
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
        if (scheme == SSH) {
            return new BaseConnectionParams(SSH, "192.168.1.1", 22, UsernamePasswordCredentials.of("root", "admin123"));
        }
        throw new IllegalArgumentException("Unsupported connection type: " + scheme);
    }
}
