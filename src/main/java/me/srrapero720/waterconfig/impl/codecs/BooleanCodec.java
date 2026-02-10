package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class BooleanCodec implements ICodec<Boolean> {
    @Override
    public String encode(Boolean instance) {
        return instance.toString();
    }

    @Override
    public Boolean decode(String value) {
        return Boolean.parseBoolean(value);
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }
}