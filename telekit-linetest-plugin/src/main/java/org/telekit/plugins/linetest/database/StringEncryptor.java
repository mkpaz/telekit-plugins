package org.telekit.plugins.linetest.database;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import org.telekit.base.service.crypto.EncryptionService;

import java.util.Base64;
import java.util.Objects;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StringEncryptor {

    private static final Logger LOG = Logger.getLogger(StringEncryptor.class.getName());
    private static final String ENCRYPTION_MARK = "$$$";

    private final EncryptionService encryptionService;

    public StringEncryptor(EncryptionService encryptionService) {
        this.encryptionService = Objects.requireNonNull(encryptionService);
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public @Nullable String encrypt(char[] sequence) {
        if (sequence == null || sequence.length == 0) { return null; }
        byte[] encData = encryptionService.encrypt(new String(sequence).getBytes(UTF_8));
        return ENCRYPTION_MARK + new String(Base64.getEncoder().encode(encData), UTF_8);
    }

    public @Nullable String decrypt(String s) {
        if (s == null || s.isEmpty()) { return null; }
        if (!s.startsWith(ENCRYPTION_MARK)) {
            LOG.warning("Unable to decrypt specified string, because it doesn't start with encryption mark");
            return s;
        }

        byte[] encData = Base64.getDecoder().decode(s.substring(ENCRYPTION_MARK.length()));
        try {
            byte[] decData = encryptionService.decrypt(encData);
            return new String(decData, UTF_8);
        } catch (Exception e) {
            LOG.severe("Unable to decrypt specified string");
            LOG.severe(ExceptionUtils.getStackTrace(e));
            return null;
        }
    }
}
