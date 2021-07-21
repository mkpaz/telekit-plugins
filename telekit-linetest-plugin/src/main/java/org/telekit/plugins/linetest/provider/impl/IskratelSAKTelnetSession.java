package org.telekit.plugins.linetest.provider.impl;

import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.telekit.base.domain.exception.TelekitException;
import org.telekit.base.domain.security.UsernamePasswordCredentials;
import org.telekit.base.net.ApacheTelnetClient;
import org.telekit.base.net.connection.ConnectionParams;
import org.telekit.plugins.linetest.domain.Measurement;
import org.telekit.plugins.linetest.provider.LinetestSession;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Logger;

import static org.telekit.base.i18n.BaseMessages.MSG_GENERIC_IO_ERROR;
import static org.telekit.base.i18n.I18n.t;
import static org.telekit.base.util.NumberUtils.isInteger;
import static org.telekit.plugins.linetest.i18n.LinetestMessages.LINETEST_ERROR_CONNECTION_FAILED;

public class IskratelSAKTelnetSession implements LinetestSession {

    private static final Logger LOG = Logger.getLogger(IskratelSAKTelnetSession.class.getName());

    private final ConnectionParams params;
    private final ApacheTelnetClient telnetClient;

    private boolean connected = false;

    public IskratelSAKTelnetSession(ConnectionParams params) {
        this.params = params;
        this.telnetClient = new ApacheTelnetClient(params.getHost(), params.getPort());
        SessionHelper.requireUsernamePasswordCredentials(params);
    }

    @Override
    public void connect() {
        try {
            LOG.fine("Connecting to " + params);

            telnetClient.addOptionHandlerSilently(new EchoOptionHandler(true, true, true, true));
            telnetClient.addOptionHandlerSilently(new TerminalTypeOptionHandler("dumb", true, true, true, true));
            telnetClient.addOptionHandlerSilently(new SuppressGAOptionHandler(false, false, false, false));

            telnetClient.connect();
        } catch (IOException e) {
            disconnectQuietly();
            throw new TelekitException(t(LINETEST_ERROR_CONNECTION_FAILED, params.getHost()), e);
        }

        try {
            String reply = telnetClient.readUntil("user", "password", "simultaneous connections");

            // maximum number of simultaneous connections reached
            if (reply != null && reply.contains("simultaneous connections")) {
                telnetClient.disconnect();
                throw new TelekitException(t(LINETEST_ERROR_CONNECTION_FAILED, params.getHost()));
            }

            UsernamePasswordCredentials userPassword = Objects.requireNonNull(
                    SessionHelper.getUsernamePasswordCredentials(params)
            );

            // old versions require user ID
            if (reply != null && reply.contains("user")) {
                telnetClient.sendLine(userPassword.getUsername());
                reply = telnetClient.readUntil("password");
            }

            // new versions only require password
            if (reply != null && reply.contains("password")) {
                telnetClient.sendLine(userPassword.getPasswordAsString());
                reply = telnetClient.readUntil(">");
            }

            if (reply == null) {
                throw new TelekitException(t(MSG_GENERIC_IO_ERROR));
            }
        } catch (IOException e) {
            disconnectQuietly();
            throw new TelekitException(t(LINETEST_ERROR_CONNECTION_FAILED, params.getHost()), e);
        }

        connected = true;
    }

    private String tidyUp(String s) {
        return s;
    }

    @Override
    public Measurement runTest(String lineId) {
        int portNumber = isInteger(lineId) ? Integer.parseInt(lineId) : -1;
        if (portNumber < 1 || portNumber > 64) {
            throw new IllegalArgumentException("Invalid port number [" + lineId + "]");
        }

        if (!connected) { throw new IllegalStateException(); }

        try {
            telnetClient.setInactivityTimeout(getAverageTestDuration().toMillis() * 2);

            String cmd = String.format("start_test %s A4 1", lineId);
            LOG.fine("Staring subscriber line test: " + cmd);
            telnetClient.sendLine(cmd);
            LOG.fine("Command accepted, waiting for result");

            String reply = telnetClient.readUntil("Press ENTER", "active mode");
            LOG.fine("Full session log:");
            LOG.fine(reply);

            return new IskratelSAKOutputParser().parse(reply);
        } catch (IOException e) {
            throw new TelekitException(t(MSG_GENERIC_IO_ERROR));
        }
    }

    @Override
    public void disconnect() {
        try {
            // logout
            if (connected && telnetClient.isConnected()) {
                LOG.fine("Disconnecting");
                telnetClient.sendString("\r\nexit\r\n");
            }
        } catch (IOException ignored) {
            // do nothing
        } finally {
            connected = false;
            disconnectQuietly();
        }
    }

    @Override
    public Duration getAverageTestDuration() {
        return Duration.ofSeconds(15);
    }

    private void disconnectQuietly() {
        try {
            if (telnetClient != null) {
                telnetClient.disconnect();
            }
        } catch (IOException ignored) {}
    }
}
