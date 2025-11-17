package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.ICodec;

import java.util.UUID;

public class UUIDCodec implements ICodec<UUID> {
    @Override
    public String encode(UUID instance) {
        return instance.toString();
    }

    @Override
    public UUID decode(String value) {
        return UUID.fromString(value);
    }

    @Override
    public Class<UUID> type() {
        return UUID.class;
    }
}
