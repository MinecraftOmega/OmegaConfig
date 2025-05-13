package org.omegaconfig.impl.formats;

import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;
import org.omegaconfig.api.formats.IFormatCodec;

import java.nio.file.Path;

public class CFGFormat implements IFormatCodec {
    @Override
    public String id() {
        return "";
    }

    @Override
    public String extension() {
        return "";
    }

    @Override
    public String mimeType() {
        return "";
    }

    @Override
    public IFormatReader createReader(Path filePath) {
        return null;
    }

    @Override
    public IFormatWriter createWritter(Path filePath) {
        return null;
    }
}
