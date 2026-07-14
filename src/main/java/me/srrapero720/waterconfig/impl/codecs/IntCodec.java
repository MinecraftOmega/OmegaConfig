package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class IntCodec implements ICodec<Integer> {
    @Override
    public String encode(Integer instance) {
        return instance.toString();
    }

    @Override
    public Integer decode(String value) {
        value = value.trim(); // QUOTED NUMBERS WITH ACCIDENTAL WHITESPACE ARE A COMMON USER ERROR
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            // JSON5-STYLE HEX LITERALS (0xFF, -0x1A)
            try {
                return Integer.decode(value);
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    @Override
    public Class<Integer> type() {
        return Integer.class;
    }
}
