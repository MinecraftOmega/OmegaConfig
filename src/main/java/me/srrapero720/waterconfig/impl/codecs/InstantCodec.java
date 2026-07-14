package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class InstantCodec implements ICodec<Instant> {
    @Override
    public String encode(Instant instance) {
        return instance.toString();
    }

    @Override
    public Instant decode(String value) {
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Class<Instant> type() {
        return Instant.class;
    }
}
