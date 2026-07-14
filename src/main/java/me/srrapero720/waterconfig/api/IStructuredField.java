package me.srrapero720.waterconfig.api;

import me.srrapero720.waterconfig.api.formats.IFormatReader;
import me.srrapero720.waterconfig.api.formats.IFormatWriter;

/**
 * A field that serializes itself against the format instead of going through a single codec
 * value — e.g. a color rendered as a {@code { r, g, b }} group. {@code ConfigSpec} delegates to
 * these methods and skips the normal scalar/array paths.
 */
public interface IStructuredField {
    /**
     * Writes this field (name and value) using the writer's primitives.
     */
    void writeSelf(IFormatWriter writer);

    /**
     * Reads this field back. Should be tolerant: accept any supported representation present in
     * the file so the spec's preferred form can change without breaking old files.
     */
    void readSelf(IFormatReader reader);
}
