package org.omegaconfig.impl.formats;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;
import org.omegaconfig.api.formats.IFormatCodec;

import java.io.IOException;
import java.nio.file.Path;

public class TOMLFormat implements IFormatCodec {
    @Override
    public String id() {
        return OmegaConfig.FORMAT_TOML;
    }

    @Override
    public String extension() {
        return "." + this.id();
    }

    @Override
    public String mimeType() {
        return "text/toml";
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
