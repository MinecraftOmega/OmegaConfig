package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public class ZonedDateTimeCodec implements ICodec<ZonedDateTime> {
    @Override
    public String encode(ZonedDateTime instance) {
        return instance.toString();
    }

    @Override
    public ZonedDateTime decode(String value) {
        try {
            return ZonedDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Class<ZonedDateTime> type() {
        return ZonedDateTime.class;
    }
}
