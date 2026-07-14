package me.srrapero720.waterconfig.impl.formats;

import me.srrapero720.waterconfig.Tools;
import me.srrapero720.waterconfig.api.formats.IFormatCodec;
import me.srrapero720.waterconfig.api.formats.IFormatReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared base for every format codec: the backslash escape/unescape helpers used by the
 * JSON, JSON5, CFG and TOML family, and the {@link BaseFormatReader} that pre-parses a whole
 * file into a dotted-path map so reads resolve against a group stack.
 */
abstract class BaseFormat implements IFormatCodec {

    // ════════════════════════════════════════════════════════════
    //  ESCAPING
    // ════════════════════════════════════════════════════════════

    // CACHED INDENT STRINGS PER DEPTH: WRITERS INDENT ON EVERY LINE
    private static final String[] TABS = new String[16];
    private static final String[] PADS = new String[16];
    static {
        TABS[0] = "";
        PADS[0] = "";
        for (int i = 1; i < 16; i++) {
            TABS[i] = TABS[i - 1] + "\t";
            PADS[i] = PADS[i - 1] + "  ";
        }
    }

    static String tabs(int depth) {
        return depth < 16 ? TABS[depth] : "\t".repeat(depth);
    }

    static String pads(int depth) {
        return depth < 16 ? PADS[depth] : "  ".repeat(depth);
    }

    // ESCAPES BACKSLASHES, DOUBLE QUOTES AND C0 CONTROL CHARS FOR A DOUBLE-QUOTED STRING;
    // RETURNS THE SAME INSTANCE WHEN NOTHING NEEDS ESCAPING
    static String escape(String s) {
        int first = firstEscapable(s);
        if (first == -1) {
            return s;
        }

        StringBuilder out = new StringBuilder(s.length() + 8);
        out.append(s, 0, first);
        for (int i = first; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    // FAST PATH: MOST STRINGS NEED NO ESCAPING AT ALL
    private static int firstEscapable(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"' || c < 0x20) {
                return i;
            }
        }
        return -1;
    }

    // DECODES ONE ESCAPE SEQUENCE: data[i] IS THE CHAR RIGHT AFTER THE BACKSLASH. APPENDS THE
    // DECODED CHAR(S) TO out AND RETURNS THE INDEX AFTER THE SEQUENCE. UNKNOWN ESCAPES DECODE
    // LENIENTLY TO THE RAW CHAR (COVERS \\ \" \' \/). bigU ALSO ACCEPTS TOML'S \UXXXXXXXX FORM
    static int unescape(char[] data, int i, StringBuilder out, boolean bigU) {
        char c = data[i];
        switch (c) {
            case 'n' -> out.append('\n');
            case 'r' -> out.append('\r');
            case 't' -> out.append('\t');
            case 'b' -> out.append('\b');
            case 'f' -> out.append('\f');
            case 'u' -> { return hex(data, i + 1, 4, out); }
            case 'U' -> {
                if (bigU) {
                    return hex(data, i + 1, 8, out);
                }
                out.append(c);
            }
            default -> out.append(c);
        }
        return i + 1;
    }

    // DECODES len HEX DIGITS INTO A CODE POINT; MALFORMED SEQUENCES KEEP THE RAW ESCAPE CHAR
    private static int hex(char[] data, int start, int len, StringBuilder out) {
        if (start + len > data.length) {
            out.append(data[start - 1]);
            return start;
        }
        int cp = 0;
        for (int k = 0; k < len; k++) {
            int d = Character.digit(data[start + k], 16);
            if (d < 0) {
                out.append(data[start - 1]);
                return start;
            }
            cp = (cp << 4) | d;
        }
        out.appendCodePoint(cp);
        return start + len;
    }

    // ════════════════════════════════════════════════════════════
    //  PRE-PARSED READER
    // ════════════════════════════════════════════════════════════

    /**
     * Base for pre-parsed readers: the whole file is parsed once into a dotted-path map,
     * reads then resolve against the current group stack.
     */
    abstract static class BaseFormatReader implements IFormatReader {
        protected final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        protected final LinkedHashMap<String, List<String>> comments = new LinkedHashMap<>();
        protected final Deque<String> group = new ArrayDeque<>();
        protected final List<RepairEntry> report = new ArrayList<>();

        @Override
        public Map<String, Object> entries() {
            return Collections.unmodifiableMap(this.values);
        }

        @Override
        public Map<String, List<String>> comments() {
            return Collections.unmodifiableMap(this.comments);
        }

        @Override
        public List<RepairEntry> report() {
            return this.report;
        }

        // READS THE WHOLE FILE AS UTF-8, STRIPPING THE BOM WINDOWS EDITORS LOVE TO PREPEND
        static char[] read(Path path) throws IOException {
            String content = new String(Tools.readAllBytes(path), StandardCharsets.UTF_8);
            if (!content.isEmpty() && content.charAt(0) == 0xFEFF) {
                content = content.substring(1);
            }
            return content.toCharArray();
        }

        // 1-BASED LINE NUMBER OF A POSITION, FOR REPAIR ENTRIES
        static int lineOf(char[] data, int pos) {
            int line = 1;
            int end = Math.min(pos, data.length);
            for (int k = 0; k < end; k++) {
                if (data[k] == '\n') line++;
            }
            return line;
        }

        @Override
        public String read(String fieldName) {
            return this.values.get(this.key(fieldName)) instanceof String s ? s : null;
        }

        @Override
        public String[] readArray(String fieldName) {
            return this.values.get(this.key(fieldName)) instanceof String[] s ? s : null;
        }

        @Override
        public void push(String group) {
            this.group.addLast(group);
        }

        @Override
        public void pop() {
            this.group.pollLast();
        }

        @Override
        public void close() {
            this.values.clear();
            this.comments.clear();
            this.group.clear();
        }

        // BUILDS THE DOTTED LOOKUP KEY FROM THE GROUP STACK AND THE FIELD NAME
        private String key(String fieldName) {
            if (this.group.isEmpty()) {
                return fieldName;
            }
            StringBuilder sb = new StringBuilder();
            for (String g: this.group) {
                sb.append(g).append('.');
            }
            return sb.append(fieldName).toString();
        }
    }
}
