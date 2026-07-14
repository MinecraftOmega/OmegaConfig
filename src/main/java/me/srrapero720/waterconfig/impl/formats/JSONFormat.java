package me.srrapero720.waterconfig.impl.formats;

import me.srrapero720.waterconfig.WaterConfig;

/**
 * Strict JSON codec: no comments, and NaN/Infinity emitted as {@code null}.
 */
public class JSONFormat extends JSONXFormat {
    @Override public String id() { return WaterConfig.FORMAT_JSON; }
    @Override public String extension() { return "." + id(); }
    @Override public String mimeType() { return "application/json"; }
    @Override protected boolean json5() { return false; }
}
