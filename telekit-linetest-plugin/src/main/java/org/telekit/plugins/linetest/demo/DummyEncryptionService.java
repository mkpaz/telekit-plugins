package org.telekit.plugins.linetest.demo;

import org.telekit.base.service.crypto.EncryptionService;

public class DummyEncryptionService implements EncryptionService {

    @Override
    public byte[] encrypt(byte[] input) {
        return input.clone();
    }

    @Override
    public byte[] decrypt(byte[] input) {
        return input.clone();
    }
}
