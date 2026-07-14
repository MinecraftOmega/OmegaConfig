package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class LongCodec implements ICodec<Long> {
    @Override
    public String encode(Long instance) {
        return instance.toString();
    }

    @Override
    public Long decode(String value) {
        value = value.trim(); // QUOTED NUMBERS WITH ACCIDENTAL WHITESPACE ARE A COMMON USER ERROR
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            // JSON5-STYLE HEX LITERALS (0xFF, -0x1A)
            try {
                return Long.decode(value);
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    @Override
    public Class<Long> type() {
        return Long.class;
    }
}