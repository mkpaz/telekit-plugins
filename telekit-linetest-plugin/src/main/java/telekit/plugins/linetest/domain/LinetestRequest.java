package telekit.plugins.linetest.domain;

import telekit.base.domain.security.Credentials;
import telekit.base.net.connection.BaseConnectionParams;
import telekit.base.net.connection.ConnectionParams;
import telekit.base.net.connection.Scheme;

import java.net.URI;
import java.util.Objects;

public class LinetestRequest {

    public static final LinetestRequest LOCALHOST = new LinetestRequest(
            new BaseConnectionParams(Scheme.SSH, "127.0.0.1", 22), "", ""
    );

    private ConnectionParams connectionParams;
    private String provider;
    private String line;

    public LinetestRequest(ConnectionParams connectionParams, String provider, String line) {
        this.connectionParams = Objects.requireNonNull(connectionParams);
        this.provider = Objects.requireNonNull(provider);
        this.line = Objects.requireNonNull(line);
    }

    public LinetestRequest(LinetestRequest that) {
        this.connectionParams = that.connectionParams;
        this.provider = that.provider;
        this.line = that.line;
    }

    public ConnectionParams getConnectionParams() {
        return connectionParams;
    }

    public void setConnectionParams(ConnectionParams connectionParams) {
        this.connectionParams = Objects.requireNonNull(connectionParams);
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = Objects.requireNonNull(line);
    }

    @Override
    public String toString() {
        return "LinetestRequest{" +
                "connectionParams=" + connectionParams +
                ", provider='" + provider + '\'' +
                ", line='" + line + '\'' +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////

    public static LinetestRequest of(URI url, Credentials credentials, String provider, String line) {
        BaseConnectionParams baseConnectionParams = BaseConnectionParams.fromUrl(url, credentials);
        return new LinetestRequest(baseConnectionParams, provider, line);
    }
}
