package telekit.plugins.linetest.provider.impl;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.MultiResult;
import net.sf.expectit.matcher.Matcher;
import telekit.base.domain.exception.TelekitException;
import telekit.base.domain.security.UsernamePasswordCredentials;
import telekit.base.net.connection.ConnectionParams;
import telekit.plugins.linetest.domain.Measurement;
import telekit.plugins.linetest.provider.LinetestSession;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static net.sf.expectit.matcher.Matchers.anyOf;
import static net.sf.expectit.matcher.Matchers.contains;
import static telekit.base.i18n.I18n.t;
import static telekit.plugins.linetest.i18n.LinetestMessages.LINETEST_ERROR_CONNECTION_FAILED;
import static telekit.plugins.linetest.i18n.LinetestMessages.LINETEST_ERROR_DATA_TRANSFER_FAILED;

public class HuaweiMA5600SSHSession implements LinetestSession {

    private static final Logger LOG = Logger.getLogger(HuaweiMA5600SSHSession.class.getName());

    public static final int CONNECT_TIMEOUT = 10_000; // ms
    public static final int RESPONSE_TIMEOUT = 60;    // sec

    private final ConnectionParams params;
    private final int connectTimeout;
    private final HuaweiMA5600OutputParser parser = new HuaweiMA5600OutputParser();

    private boolean connected = false;

    public HuaweiMA5600SSHSession(ConnectionParams params) {
        this(params, CONNECT_TIMEOUT);
    }

    private SSHClient sshClient;

    public HuaweiMA5600SSHSession(ConnectionParams params, int connectTimeout) {
        this.params = params;
        this.connectTimeout = connectTimeout;

        SessionHelper.requireUsernamePasswordCredentials(params);
    }

    @Override
    public void connect() {
        try {
            sshClient = new SSHClient();
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.setConnectTimeout(CONNECT_TIMEOUT);

            LOG.fine("Connecting to " + params);
            sshClient.connect(InetAddress.getByName(params.getHost()), params.getPort());

            UsernamePasswordCredentials userPassword = Objects.requireNonNull(
                    SessionHelper.getUsernamePasswordCredentials(params)
            );

            sshClient.authPassword(
                    userPassword.getUsername(),
                    new ExactPasswordFinder(userPassword.getPassword())
            );

            connected = true;
        } catch (IOException e) {
            throw new TelekitException(t(LINETEST_ERROR_CONNECTION_FAILED, params.getHost()), e);
        }
    }

    @Override
    public Measurement runTest(String lineId) {
        if (!connected) { throw new IllegalStateException(); }

        int inputPos;

        try {
            Session session = sshClient.startSession();
            session.allocateDefaultPTY();

            StringBuilder outBuffer = new StringBuilder();
            Shell shell = session.startShell();
            Expect exchange = new ExpectBuilder()
                    .withOutput(shell.getOutputStream())
                    .withInputs(shell.getInputStream())
                    .withTimeout(RESPONSE_TIMEOUT, TimeUnit.SECONDS)
                    .withEchoInput(outBuffer)
                    .withExceptionOnFailure()
                    .build();

            exchange.expect(contains(">"));
            LOG.fine("Logged to system in unprivileged mode");

            LOG.fine("Activating machine to machine mode");
            exchange.sendLine("mmi-mode enable");
            exchange.expect(contains(">"));

            LOG.fine("Switching to privileged mode");
            exchange.sendLine("enable");
            exchange.expect(contains("#"));

            LOG.fine("Switching to config mode");
            exchange.sendLine("config");
            exchange.expect(contains("(config)#"));

            LOG.fine("Switching to test mode");
            exchange.sendLine("test");
            exchange.expect(contains("(config-test)#"));

            // keep position where error message will start if test failed
            inputPos = outBuffer.length() - "(config-test)#".length();
            String cmd = String.format("pots loop-line-test %s extend", lineId);
            LOG.fine("Staring subscriber line test: " + cmd);
            exchange.sendLine(cmd);

            Matcher<MultiResult> matchers = anyOf(
                    contains("Please wait"),     // "under testing, Please wait......"
                    contains("Parameter error"), // "% Parameter error, the error locates"
                    contains("Can not find")     // "Failure: Can not find the board"
            );
            exchange.expect(matchers);

            if (!outBuffer.toString().contains("Please wait")) {
                disconnect();
                return parser.parse(outBuffer.substring(inputPos));
            }

            LOG.fine("Command accepted, waiting for result");

            // wait for beginning of the test result
            inputPos = outBuffer.length();
            exchange.expect(contains("Testing"));

            // ... and read it until invitation
            exchange.expect(contains("(config-test)#"));

            LOG.fine("Full session log:");
            LOG.fine(outBuffer.toString());

            return parser.parse(outBuffer.substring(inputPos));
        } catch (IOException e) {
            throw new TelekitException(t(LINETEST_ERROR_DATA_TRANSFER_FAILED), e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (sshClient != null) {
                LOG.fine("Disconnecting");
                sshClient.disconnect();
                sshClient.close();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public Duration getAverageTestDuration() {
        return Duration.of(20, ChronoUnit.SECONDS);
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }
}
