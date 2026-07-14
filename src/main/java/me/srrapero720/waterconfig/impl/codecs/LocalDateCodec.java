package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class LocalDateCodec implements ICodec<LocalDate> {
    @Override
    public String encode(LocalDate instance) {
        return instance.toString();
    }

    @Override
    public LocalDate decode(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Class<LocalDate> type() {
        return LocalDate.class;
    }
}
