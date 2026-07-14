package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

public class BooleanCodec implements ICodec<Boolean> {
    @Override
    public String encode(Boolean instance) {
        return instance.toString();
    }

    @Override
    public Boolean decode(String value) {
        // STRICT true/false (CASE-INSENSITIVE, TRIMMED): ANYTHING ELSE IS AN UNEXPECTED
        // TYPE AND MUST RESET TO DEFAULT INSTEAD OF SILENTLY BECOMING false
        String v = value.trim();
        if (v.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (v.equalsIgnoreCase("false")) return Boolean.FALSE;
        return null;
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }
}