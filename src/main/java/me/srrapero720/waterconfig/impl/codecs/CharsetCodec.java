package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.nio.charset.Charset;

public class CharsetCodec implements ICodec<Charset> {
    @Override
    public String encode(Charset instance) {
        return instance.name();
    }

    @Override
    public Charset decode(String value) {
        try {
            return Charset.forName(value.trim());
        } catch (IllegalArgumentException e) {
            // COVERS BOTH IllegalCharsetNameException AND UnsupportedCharsetException
            return null;
        }
    }

    @Override
    public Class<Charset> type() {
        return Charset.class;
    }
}
