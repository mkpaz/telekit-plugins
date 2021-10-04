package telekit.plugins.linetest.provider.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;
import telekit.base.domain.security.UsernamePasswordCredentials;
import telekit.base.net.connection.BaseConnectionParams;
import telekit.base.net.connection.ConnectionParams;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static java.lang.System.getenv;
import static telekit.base.net.connection.Scheme.SSH;

public class HuaweiMA5600SSHSessionTest {

    @Test
    public void testLineByPortNumber() {
        UsernamePasswordCredentials cred = UsernamePasswordCredentials.of(
                getenv("TEST_MA5600_USERNAME"),
                getenv("TEST_MA5600_PASSWORD")
        );
        ConnectionParams params = new BaseConnectionParams(SSH, getenv("TEST_MA5600_HOST"), 22, cred);

        try (HuaweiMA5600SSHSession session = new HuaweiMA5600SSHSession(params)) {
            session.connect();
            session.runTest("0/3/0");
        }
    }

    @BeforeAll
    public static void beforeAll() {
        // Unlike other log bridges, jul-to-slf4j requires ALL JUL messages to be sent to the bridge
        // So, we have to set lowest possible priority to all JUL loggers
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
        Logger[] pin = new Logger[]{Logger.getLogger("")};

        for (Logger logger : pin) {
            logger.setLevel(Level.ALL);
        }
    }
}