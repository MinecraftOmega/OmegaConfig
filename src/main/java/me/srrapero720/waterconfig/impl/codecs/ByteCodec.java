package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class ByteCodec implements ICodec<Byte> {
    @Override
    public String encode(Byte instance) {
        return instance.toString();
    }

    @Override
    public Byte decode(String value) {
        try {
            return Byte.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<Byte> type() {
        return Byte.class;
    }
}