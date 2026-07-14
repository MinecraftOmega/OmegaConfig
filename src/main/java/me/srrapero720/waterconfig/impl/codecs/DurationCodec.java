package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.Duration;
import java.time.format.DateTimeParseException;

public class DurationCodec implements ICodec<Duration> {
    @Override
    public String encode(Duration instance) {
        return instance.toString();
    }

    @Override
    public Duration decode(String value) {
        try {
            return Duration.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Class<Duration> type() {
        return Duration.class;
    }
}
