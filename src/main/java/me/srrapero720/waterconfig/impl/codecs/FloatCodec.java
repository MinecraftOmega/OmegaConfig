package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class FloatCodec implements ICodec<Float> {
    @Override
    public String encode(Float instance) {
        return instance.toString();
    }

    @Override
    public Float decode(String value) {
        try {
            return Float.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<Float> type() {
        return Float.class;
    }
}
