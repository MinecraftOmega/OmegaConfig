package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class ByteCodec implements ICodec<Byte> {
    @Override
    public String encode(Byte instance) {
        return instance.toString();
    }

    @Override
    public Byte decode(String value) {
        value = value.trim(); // QUOTED NUMBERS WITH ACCIDENTAL WHITESPACE ARE A COMMON USER ERROR
        try {
            return Byte.valueOf(value);
        } catch (NumberFormatException e) {
            // JSON5-STYLE HEX LITERALS (0xFF, -0x1A)
            try {
                return Byte.decode(value);
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    @Override
    public Class<Byte> type() {
        return Byte.class;
    }
}