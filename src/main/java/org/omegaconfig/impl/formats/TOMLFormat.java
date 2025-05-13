package org.omegaconfig.impl.formats;

import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;
import org.omegaconfig.api.formats.IFormatCodec;

import java.io.IOException;
import java.nio.file.Path;

public class TOMLFormat implements IFormatCodec {
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
    public IFormatReader createReader(Path filePath) throws IOException {
        return null;
    }

    @Override
    public IFormatWriter createWritter(Path filePath) throws IOException {
        return null;
    }
}
