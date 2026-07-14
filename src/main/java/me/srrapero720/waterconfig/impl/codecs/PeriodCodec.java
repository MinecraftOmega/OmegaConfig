package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.time.Period;
import java.time.format.DateTimeParseException;

public class PeriodCodec implements ICodec<Period> {
    @Override
    public String encode(Period instance) {
        return instance.toString();
    }

    @Override
    public Period decode(String value) {
        try {
            return Period.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Class<Period> type() {
        return Period.class;
    }
}
