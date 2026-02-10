package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class IntCodec implements ICodec<Integer> {
    @Override
    public String encode(Integer instance) {
        return instance.toString();
    }

    @Override
    public Integer decode(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Class<Integer> type() {
        return Integer.class;
    }
}
