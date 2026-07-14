package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.util.Locale;

public class LocaleCodec implements ICodec<Locale> {
    @Override
    public String encode(Locale instance) {
        return instance.toLanguageTag();
    }

    @Override
    public Locale decode(String value) {
        // BCP-47 LANGUAGE TAG (en, en-US, zh-Hans-CN); BLANK IS NOT A LOCALE
        String v = value.trim();
        if (v.isEmpty()) {
            return null;
        }
        return Locale.forLanguageTag(v);
    }

    @Override
    public Class<Locale> type() {
        return Locale.class;
    }
}
