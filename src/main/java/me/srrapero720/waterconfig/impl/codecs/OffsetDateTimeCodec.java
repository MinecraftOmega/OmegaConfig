package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public class OffsetDateTimeCodec implements ICodec<OffsetDateTime> {
    @Override
    public String encode(OffsetDateTime instance) {
        return instance.toString();
    }

    @Override
    public OffsetDateTime decode(String value) {
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Class<OffsetDateTime> type() {
        return OffsetDateTime.class;
    }
}
