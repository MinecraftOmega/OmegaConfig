package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class CharCodec implements ICodec<Character> {
    @Override
    public String encode(Character instance) {
        return instance.toString();
    }

    @Override
    public Character decode(String value) {
        return value.length() == 1 ? value.charAt(0) : null;
    }

    @Override
    public Class<Character> type() {
        return Character.class;
    }
}