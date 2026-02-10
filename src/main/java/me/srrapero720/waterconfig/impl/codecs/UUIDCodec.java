package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

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
