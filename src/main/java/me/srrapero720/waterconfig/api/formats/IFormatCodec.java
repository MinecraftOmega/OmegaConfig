package me.srrapero720.waterconfig.api.formats;

import java.io.IOException;
import java.nio.file.Path;

public interface IFormatCodec {
    /**
     * Get the format id, usually is the extension of the file without the dot.
     * @return format id
     */
    String id();

    /**
     * Get the format extension
     * @return extension of the file, usually is the format id with a dot at the start
     */
    String extension();

    /**
     * Get the mime type of the format
     * @return mimetype in the form of "type/subtype", lowercase and starts with "text/"
     */
    String mimeType();

    /**
     * creates a new reader for the given filepath
     * reading is pre-made instead of a sequential read
     *
     * @param filePath path of the file to read
     * @return a new reader of the current format
     */
    IFormatReader createReader(Path filePath) throws IOException;

    /**
     * creates a new writer for the given filepath
     * writing is sequential.
     *
     * @param filePath path of the file to write
     * @return a new writer of the current format
     */
    IFormatWriter createWriter(Path filePath) throws IOException;
}
