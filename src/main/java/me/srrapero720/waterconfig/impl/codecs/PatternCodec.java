package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternCodec implements ICodec<Pattern> {
    @Override
    public String encode(Pattern instance) {
        return instance.pattern();
    }

    @Override
    public Pattern decode(String value) {
        try {
            return Pattern.compile(value);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    @Override
    public Class<Pattern> type() {
        return Pattern.class;
    }
}
