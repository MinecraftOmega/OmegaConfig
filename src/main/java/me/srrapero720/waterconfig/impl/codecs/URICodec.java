package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.net.URI;
import java.net.URISyntaxException;

public class URICodec implements ICodec<URI> {
    @Override
    public String encode(URI instance) {
        return instance.toString();
    }

    @Override
    public URI decode(String encoded) {
        try {
            return new URI(encoded);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public Class<URI> type() {
        return URI.class;
    }
}
