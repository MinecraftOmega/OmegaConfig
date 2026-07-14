package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.net.URI;
import java.net.URL;

public class URLCodec implements ICodec<URL> {
    @Override
    public String encode(URL instance) {
        return instance.toString();
    }

    @Override
    public URL decode(String value) {
        // VIA URI TO AVOID THE DEPRECATED URL(String) CONSTRUCTOR; NEEDS AN ABSOLUTE URL
        try {
            return URI.create(value.trim()).toURL();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Class<URL> type() {
        return URL.class;
    }
}
