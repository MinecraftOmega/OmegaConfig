package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.api.ICodec;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class PathCodec implements ICodec<Path> {
    @Override
    public String encode(Path instance) {
        // KEEP THE PATH AS-IS: FORCING ABSOLUTE WOULD BREAK RUNTIME (RELATIVE) PATH FIELDS
        return instance.toString();
    }

    @Override
    public Path decode(String value) {
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    @Override
    public Class<Path> type() {
        return Path.class;
    }
}
