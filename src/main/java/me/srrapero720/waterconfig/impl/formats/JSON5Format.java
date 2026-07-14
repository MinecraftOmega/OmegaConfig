package me.srrapero720.waterconfig.impl.formats;

import me.srrapero720.waterconfig.WaterConfig;

/**
 * JSON5 codec: keeps comments and NaN/Infinity tokens, and tolerates unquoted keys,
 * single-quoted strings and trailing commas on read.
 */
public class JSON5Format extends JSONXFormat {
    @Override public String id() { return WaterConfig.FORMAT_JSON5; }
    @Override public String extension() { return "." + id(); }
    @Override public String mimeType() { return "application/json5"; }
    @Override protected boolean json5() { return true; }
}
