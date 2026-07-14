package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class LocalDateTimeCodec implements ICodec<LocalDateTime> {
    @Override
    public String encode(LocalDateTime instance) {
        return instance.toString();
    }

    @Override
    public LocalDateTime decode(String value) {
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Class<LocalDateTime> type() {
        return LocalDateTime.class;
    }
}
