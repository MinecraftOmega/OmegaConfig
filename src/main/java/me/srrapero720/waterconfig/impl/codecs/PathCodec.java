package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.nio.file.Path;

public class PathCodec implements ICodec<Path> {
    @Override
    public String encode(Path instance) {
        return instance.toAbsolutePath().toString();
    }

    @Override
    public Path decode(String value) {
        return Path.of(value);
    }

    @Override
    public Class<Path> type() {
        return Path.class;
    }
}
