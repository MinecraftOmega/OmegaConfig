package org.omegaconfig.impl.formats;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.Tools;
import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;
import org.omegaconfig.api.formats.IFormatCodec;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class JSONFormat implements IFormatCodec {
    private static final char JSON_OBJECT_START = '{';
    private static final char JSON_OBJECT_END = '}';
    private static final char JSON_CONTINUE = ',';
    private static final char JSON_STRING_LINE = '"';
    private static final char JSON_ENTRY_SPLIT = ':';
    private static final char JSON_ARRAY_START = '[';
    private static final char JSON_ARRAY_END = ']';
    private static final char JSON_ESCAPED = '\\';

    @Override public String id() { return OmegaConfig.FORMAT_JSON; }
    @Override public String extension() { return "." + id(); }
    @Override public String mimeType() { return "application/json"; }

    @Override
    public IFormatReader createReader(Path filePath) throws IOException {
        return new FormatReader(filePath);
    }

    @Override
    public IFormatWriter createWritter(Path filePath) throws IOException {
        return null;
    }

    private record NextChar(char[] expected, char[] allowed) {

        public boolean isExpected(char c) {
            return Tools.contains(c, expected);
        }

        public boolean isAllowed(char c) {
            if (allowed == null) return false;
            return Tools.contains(c, allowed);
        }
    }

    public enum CapturingMode {
        NONE,
        KEY,
        PRIMITIVE_VALUE,
        STRING_VALUE,
        ARRAY,
        ARRAY_STRING_VALUE;

        public boolean value() {
            return this == PRIMITIVE_VALUE || this == STRING_VALUE;
        }

        public boolean string() {
            return this == STRING_VALUE || this == KEY;
        }
    }

    public static class FormatReader implements IFormatReader {
        public final LinkedHashMap<String, String> values = new LinkedHashMap<>();

        public FormatReader(Path path) throws IOException {
            // READ JSON STRING
            var in = new FileInputStream(path.toFile());
            char[] data = new String(in.readAllBytes()).toCharArray();
            in.close();

            NextChar nextChars = new NextChar(new char[] { JSON_OBJECT_START }, null);
            CapturingMode capturing = CapturingMode.NONE;
            final LinkedHashSet<String> group = new LinkedHashSet<>();
            StringBuilder key = new StringBuilder();
            StringBuilder value = new StringBuilder();
            List<String> valueArray = new ArrayList<>();
            boolean escaped = false;
            boolean finished = false;

            for (char c: data) {
                boolean whitespace = Character.isWhitespace(c);

                // SKIP WHITESPACE PROCESING WHEN IS NOT CAPTURING NON-STRING-VALUES
                if (whitespace && (capturing != CapturingMode.STRING_VALUE && capturing != CapturingMode.KEY && capturing != CapturingMode.ARRAY_STRING_VALUE) || (capturing == CapturingMode.ARRAY_STRING_VALUE && nextChars != null))
                    continue;

                // THROW WHEN JSON SPEC IS FINISHED BUT STILL CONTAINS DATA (WTF)
                if (finished) {
                    throw new EOFException("Reached end of JSON spec but file still contains data");
                }

                // VALIDATE CHARS
                if (nextChars != null && !nextChars.isExpected(c)) {
                    if (nextChars.isAllowed(c)) {
                        // TODO: handle permisive chars
                    }

                    throw new IllegalStateException("Expected char(s) " + Arrays.toString(nextChars.expected) + " but received " + c);
                }

                // CHAR DETECTION
                switch (c) {
                    case JSON_OBJECT_START -> {
                        if (capturing == CapturingMode.PRIMITIVE_VALUE) {
                            group.add(key.toString());
                            key = new StringBuilder();
                            capturing = CapturingMode.NONE;
                        }

                        nextChars = new NextChar(new char[] { JSON_STRING_LINE, JSON_OBJECT_END }, null);
                        continue;
                    }

                    case JSON_ARRAY_START -> {
                        if (capturing.string()) {
                            break;
                        }
                        capturing = CapturingMode.ARRAY;
                        continue;
                    }

                    case JSON_STRING_LINE -> {
                        // IF IS CAPTURING AND KEY IS ESCAPED THEN SKIP PROCESING
                        if (escaped && (capturing == CapturingMode.KEY || capturing == CapturingMode.STRING_VALUE)) {
                            break;
                        }

                        if (capturing == CapturingMode.ARRAY_STRING_VALUE) {
                            valueArray.add(value.toString());
                            value = new StringBuilder();
                            nextChars = new NextChar(new char[] { JSON_CONTINUE, JSON_ARRAY_END }, null);
                            continue;
                        }

                        if (capturing == CapturingMode.ARRAY) {
                            capturing = CapturingMode.ARRAY_STRING_VALUE;
                            nextChars = null;
                            continue;
                        }

                        // FINISHED CAPTURING
                        if (capturing == CapturingMode.STRING_VALUE) {
                            capturing = CapturingMode.NONE;
                            values.put(Tools.concat("", (!group.isEmpty() ? "." : "") + key, '.', group), value.toString());
                            key = new StringBuilder();
                            value = new StringBuilder();

                            nextChars = new NextChar(new char[] { JSON_CONTINUE, JSON_OBJECT_END }, null);
                            continue;
                        }

                        // IS NOT A STRING VALUE
                        if (capturing == CapturingMode.PRIMITIVE_VALUE) {
                            capturing = CapturingMode.STRING_VALUE;
                            nextChars = null;
                            continue;
                        }

                        // KEY CAPTURING FINISHED
                        if (capturing == CapturingMode.KEY) {
                            capturing = CapturingMode.NONE;
                            nextChars = new NextChar(new char[] { JSON_ENTRY_SPLIT }, null);
                            continue;
                        }

                        capturing = CapturingMode.KEY;
                        nextChars = null;
                        continue;
                    }

                    case JSON_ESCAPED -> {
                        escaped = true;
                        continue;
                    }

                    case JSON_ENTRY_SPLIT -> {
                        if (capturing.string()) {
                            break;
                        }
                        capturing = CapturingMode.PRIMITIVE_VALUE;
                        nextChars = null;
                        continue;
                    }

                    case JSON_CONTINUE -> {
                        // DO NOT CAPTURE WHEN IS CAPTURING A KEY
                        if (capturing.string()) {
                            break;
                        }
                        if (capturing == CapturingMode.PRIMITIVE_VALUE) {
                            values.put(Tools.concat("", (!group.isEmpty() ? "." : "") + key.toString(), '.', group), value.toString());
                            capturing = CapturingMode.NONE;
                            key = new StringBuilder();
                            value = new StringBuilder();
                        }

                        if (capturing == CapturingMode.ARRAY_STRING_VALUE) {
                            capturing = CapturingMode.ARRAY;
                            nextChars = new NextChar(new char[] { JSON_STRING_LINE, JSON_ARRAY_END }, null);
                            continue;
                        }

                        if (capturing == CapturingMode.ARRAY) {
                            valueArray.add(value.toString());
                            value = new StringBuilder();
                            nextChars = null;
                            continue;
                        }

                        nextChars = new NextChar(new char[] { JSON_STRING_LINE, JSON_OBJECT_END }, null);
                        continue;
                    }

                    case JSON_OBJECT_END -> {
                        if (group.isEmpty()) {
                            finished = true;
                            capturing = CapturingMode.NONE;
                            nextChars = null;
                            continue;
                        }
                        group.removeLast();
                        nextChars = new NextChar(new char[]{ JSON_CONTINUE, JSON_OBJECT_END }, null);
                        continue;
                    }

                    case JSON_ARRAY_END -> {
                        if (capturing == CapturingMode.ARRAY || capturing == CapturingMode.ARRAY_STRING_VALUE) {
                            capturing = CapturingMode.NONE;
                            nextChars = new NextChar(new char[]{ JSON_CONTINUE, JSON_OBJECT_END }, null);
                            valueArray.add(value.toString());
                            values.put(Tools.concat("", (!group.isEmpty() ? "." : "") + key, '.', group), Arrays.toString(valueArray.toArray(new String[0])));
                            valueArray.clear();
                            value = new StringBuilder();
                            key = new StringBuilder();

                            continue;
                        }
                        throw new IllegalStateException("JSON array end detected but not capturing an array");
                    }
                }

                StringBuilder appender = switch (capturing) {
                    case KEY -> key;
                    case STRING_VALUE, PRIMITIVE_VALUE -> value;
                    case ARRAY_STRING_VALUE, ARRAY -> value;
                    case NONE -> throw new IllegalStateException("Not capturing values");
                };

                if (escaped) {
                    appender.append("\\");
                }

                // OR BY DEFAULT, CAPTURE
                appender.append(c);

                escaped = false;
            }
        }

        @Override
        public String read(String fieldName) {
            return "";
        }

        @Override
        public void push(String group) {

        }

        @Override
        public void pop() {

        }

        @Override
        public void close() throws IOException {

        }
    }
}
