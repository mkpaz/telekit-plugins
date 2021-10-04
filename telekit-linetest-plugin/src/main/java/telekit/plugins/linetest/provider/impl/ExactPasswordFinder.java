package telekit.plugins.linetest.provider.impl;

import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;

public class ExactPasswordFinder implements PasswordFinder {

    private final char[] password;

    public ExactPasswordFinder(char[] password) {
        this.password = password;
    }

    @Override
    public char[] reqPassword(Resource<?> resource) {
        return password.clone();
    }

    @Override
    public boolean shouldRetry(Resource<?> resource) {
        return false;
    }
}