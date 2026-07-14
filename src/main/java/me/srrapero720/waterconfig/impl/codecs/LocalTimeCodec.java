package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class LocalTimeCodec implements ICodec<LocalTime> {
    @Override
    public String encode(LocalTime instance) {
        return instance.toString();
    }

    @Override
    public LocalTime decode(String value) {
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Class<LocalTime> type() {
        return LocalTime.class;
    }
}
