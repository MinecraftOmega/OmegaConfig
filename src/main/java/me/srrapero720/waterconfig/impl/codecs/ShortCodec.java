package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class ShortCodec implements ICodec<Short> {
    @Override
    public String encode(Short instance) {
        return instance.toString();
    }

    @Override
    public Short decode(String value) {
        value = value.trim(); // QUOTED NUMBERS WITH ACCIDENTAL WHITESPACE ARE A COMMON USER ERROR
        try {
            return Short.valueOf(value);
        } catch (NumberFormatException e) {
            // JSON5-STYLE HEX LITERALS (0xFF, -0x1A)
            try {
                return Short.decode(value);
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    @Override
    public Class<Short> type() {
        return Short.class;
    }
}