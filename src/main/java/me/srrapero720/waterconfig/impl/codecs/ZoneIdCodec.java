package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.DateTimeException;
import java.time.ZoneId;

public class ZoneIdCodec implements ICodec<ZoneId> {
    @Override
    public String encode(ZoneId instance) {
        return instance.getId();
    }

    @Override
    public ZoneId decode(String value) {
        try {
            return ZoneId.of(value.trim());
        } catch (DateTimeException e) {
            return null;
        }
    }

    @Override
    public Class<ZoneId> type() {
        return ZoneId.class;
    }
}
