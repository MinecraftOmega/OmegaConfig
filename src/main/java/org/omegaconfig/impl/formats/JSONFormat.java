package org.omegaconfig.impl.formats;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.Tools;
import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;
import org.omegaconfig.api.formats.IFormatCodec;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        return new FormatWriter(filePath);
    }

    public static class FormatWriter implements IFormatWriter {
        private final Stack<String> group = new Stack<>();
        private final BufferedWriter writer;
        private final StringBuilder buffer = new StringBuilder();
        private boolean beginned = false;

        public FormatWriter(Path path) throws IOException {
            // TODO: safe maker
            if (!path.toFile().getParentFile().exists() && !path.toFile().getParentFile().mkdirs()) {
                throw new IOException("Failed to create parent directories for " + path);
            }
            this.writer = new BufferedWriter(new FileWriter(path.toFile(), StandardCharsets.UTF_8));
            this.buffer.append(JSON_OBJECT_START);
            this.buffer.append("\n");
        }

        @Override
        public void write(String comment) {
            // NO-OP
        }

        @Override
        public void write(String fieldName, String value, Class<?> type, Class<?> subType) {
            if (this.beginned) {
                this.buffer.append(JSON_CONTINUE);
                this.buffer.append("\n");
            } else {
                this.beginned = true;
            }

            System.out.println("Writing field: " + fieldName + " with value: " + value + " of type: " + type.getName() + " and subtype: " + (subType != null ? subType.getName() : "null"));
            boolean isString = !(Number.class.isAssignableFrom(type)) || Boolean.class.isAssignableFrom(type);

            // WRITE SPACES
            this.buffer.append("\t".repeat(this.group.size() + 1));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(fieldName);
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(JSON_ENTRY_SPLIT);
            this.buffer.append(" ");
            if (isString) {
                this.buffer.append(JSON_STRING_LINE);
            }
            this.buffer.append(value);
            if (isString) {
                this.buffer.append(JSON_STRING_LINE);
            }

        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {
            if (this.beginned) {
                this.buffer.append(JSON_CONTINUE);
                this.buffer.append("\n");
            } else {
                this.beginned = true;
            }

            boolean isString = (!subType.isAssignableFrom(Number.class) || !subType.isAssignableFrom(Boolean.class));

            this.buffer.append("\t".repeat(this.group.size() + 1));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(fieldName);
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(JSON_ENTRY_SPLIT);
            this.buffer.append(" ");
            this.buffer.append(JSON_ARRAY_START);
            this.buffer.append("\n");
            Iterator<String> it = List.of(values).iterator();

            while (it.hasNext()) {
                String value = it.next();
                this.buffer.append("\t".repeat(this.group.size() + 2));
                if (isString) {
                    this.buffer.append(JSON_STRING_LINE);
                }
                this.buffer.append(value);
                if (isString) {
                    this.buffer.append(JSON_STRING_LINE);
                }
                if (it.hasNext()) {
                    this.buffer.append(JSON_CONTINUE);
                }
                this.buffer.append("\n");
            }
            this.buffer.append("\t".repeat(this.group.size() + 1));
            this.buffer.append(JSON_ARRAY_END);
        }

        @Override
        public void push(String groupName) {
            if (this.beginned) {
                this.buffer.append(JSON_CONTINUE);
                this.buffer.append("\n");
            } else {
                this.beginned = true;
            }
            this.buffer.append("\t".repeat(this.group.size() + 1));
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(groupName);
            this.buffer.append(JSON_STRING_LINE);
            this.buffer.append(JSON_ENTRY_SPLIT);
            this.buffer.append(" ");
            this.buffer.append(JSON_OBJECT_START);
            this.buffer.append("\n");
            this.group.push(groupName);
            this.beginned = false;
        }

        @Override
        public void pop() {
            this.group.pop();
            this.buffer.append('\n');
            this.buffer.append("\t".repeat(this.group.size() + 1));
            this.buffer.append(JSON_OBJECT_END);
            this.buffer.append("\n");
        }

        @Override
        public void close() throws IOException {
            this.buffer.append(JSON_OBJECT_END);
            this.writer.write(this.buffer.toString());
            this.writer.flush();
            this.writer.close();
        }
    }

    public static class FormatReader implements IFormatReader {
        public static final char[] START_CHARS = new char[] { JSON_OBJECT_START };
        public static final char[] KEY_OR_END = new char[] { JSON_STRING_LINE, JSON_OBJECT_END };
        public static final char[] CONTINUE_OR_END = new char[] { JSON_CONTINUE, JSON_OBJECT_END };
        public static final char[] CONTINUE_OR_ARRAY_END = new char[] { JSON_CONTINUE, JSON_ARRAY_END };
        public static final char[] SPLIT = new char[] { JSON_ENTRY_SPLIT };

        public static final int NONE = 0;
        public static final int KEY = 1;
        public static final int VALUE = 2;
        public static final int VALUE_STRING = 3;
        public static final int ARRAY = 4;
        public static final int ARRAY_STRING = 5;
        public final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        public final Stack<String> group = new Stack<>();
        public final StringBuilder key = new StringBuilder();
        public final StringBuilder value = new StringBuilder();
        public final List<String> arrayValues = new ArrayList<>();
        public boolean escaped;
        public boolean finished = false;

        public FormatReader(Path path) throws IOException {
            // READ JSON STRING
            char[] data = new String(Tools.readAllBytes(path), StandardCharsets.UTF_8).toCharArray();

            // READING STATES
            char[] nexts = START_CHARS;
            int capturing = NONE;

            for (char c: data) {
                boolean whitespace = Character.isWhitespace(c);

                // SKIP WHITESPACE PROCESING WHEN IS NOT CAPTURING NON-STRING-VALUES
                if (whitespace && ((capturing != VALUE_STRING && capturing != KEY && capturing != ARRAY_STRING) || (capturing == ARRAY_STRING && nexts != null)))
                    continue;

                // THROW WHEN JSON SPEC IS FINISHED BUT STILL CONTAINS DATA (WTF)
                if (finished) {
                    throw new EOFException("Reached end of JSON spec but file still contains data");
                }

                // VALIDATE CHARS
                if (nexts != null && !Tools.contains(c, nexts)) {
                    throw new IllegalStateException("Expected char(s) " + Arrays.toString(nexts) + " but received " + c);
                }

                // CHAR DETECTION
                switch (c) {
                    case JSON_OBJECT_START -> {
                        if (capturing == VALUE) {
                            this.pushGroup();
                            capturing = NONE;
                        }

                        nexts = KEY_OR_END;
                        continue;
                    }

                    case JSON_ARRAY_START -> {
                        if (capturing == KEY || capturing == VALUE_STRING) {
                            break;
                        }
                        capturing = ARRAY;
                        continue;
                    }

                    case JSON_STRING_LINE -> {
                        // IF IS CAPTURING AND KEY IS ESCAPED THEN SKIP PROCESING
                        if (this.escaped && (capturing == KEY || capturing == VALUE_STRING)) {
                            break;
                        }

                        if (capturing == ARRAY_STRING) {
                            this.putArrayValue();
                            nexts = CONTINUE_OR_ARRAY_END;
                            continue;
                        }

                        if (capturing == ARRAY) {
                            capturing = ARRAY_STRING;
                            nexts = null;
                            continue;
                        }

                        // FINISHED CAPTURING
                        if (capturing == VALUE_STRING) {
                            capturing = NONE;
                            this.putEntry(false);
                            nexts = CONTINUE_OR_END;
                            continue;
                        }

                        // IS NOT A STRING VALUE
                        if (capturing == VALUE) {
                            capturing = VALUE_STRING;
                            nexts = null;
                            continue;
                        }

                        // KEY CAPTURING FINISHED
                        if (capturing == KEY) {
                            capturing = NONE;
                            nexts = SPLIT;
                            continue;
                        }

                        capturing = KEY;
                        nexts = null;
                        continue;
                    }

                    case JSON_ESCAPED -> {
                        escaped = true;
                        continue;
                    }

                    case JSON_ENTRY_SPLIT -> {
                        if (capturing == KEY || capturing == VALUE_STRING) {
                            break;
                        }
                        capturing = VALUE;
                        nexts = null;
                        continue;
                    }

                    case JSON_CONTINUE -> {
                        // DO NOT CAPTURE WHEN IS CAPTURING A KEY
                        if (capturing == KEY || capturing == VALUE_STRING) {
                            break;
                        }
                        if (capturing == VALUE) {
                            this.putEntry(false);
                            capturing = NONE;
                            this.clear();
                        }

                        if (capturing == ARRAY_STRING) {
                            capturing = ARRAY;
                            nexts = new char[] { JSON_STRING_LINE, JSON_ARRAY_END };
                            continue;
                        }

                        if (capturing == ARRAY) {
                            this.putArrayValue();
                            nexts = null;
                            continue;
                        }

                        nexts = new char[] { JSON_STRING_LINE, JSON_OBJECT_END };
                        continue;
                    }

                    case JSON_OBJECT_END -> {
                        this.popGroup(); // POP GROUP fixme move capturing to popGroup
                        if (group.isEmpty()) {
                            capturing = NONE;
                            nexts = null;
                            continue;
                        }
                        group.removeLast();
                        nexts = CONTINUE_OR_END;
                        continue;
                    }

                    case JSON_ARRAY_END -> {
                        if (capturing == ARRAY || capturing == ARRAY_STRING) {
                            capturing = NONE;
                            nexts = CONTINUE_OR_END;
                            this.putArrayValue();
                            this.putEntry(true);
                            continue;
                        }
                        throw new IllegalStateException("JSON array end detected but not capturing an array");
                    }
                }

                switch (capturing) {
                    case KEY -> this.appendKey(c);
                    case VALUE_STRING, VALUE, ARRAY_STRING, ARRAY -> this.appendValue(c);
                    case NONE -> throw new IllegalStateException("Not capturing values");
                }
            }
        }

        @Override
        public String read(String fieldName) {
            var value = values.get(Tools.concat("", (!group.isEmpty() ? "." : "") + fieldName, '.', group));
            if (value instanceof String s) {
                return s;
            }
            return null;
        }

        @Override
        public String[] readArray(String fieldName) {
            var value = values.get(Tools.concat("", (!group.isEmpty() ? "." : "") + fieldName, '.', group));
            if (value instanceof String[] s) {
                return s;
            }
            return null;
        }

        @Override
        public void push(String group) {
            this.group.push(group);
        }

        @Override
        public void pop() {
            this.group.pop();
        }

        @Override
        public void close() {
            this.values.clear();
            this.group.clear();
            this.key.setLength(0);
            this.value.setLength(0);
            this.arrayValues.clear();
        }

        private void appendKey(char c) {
            if (escaped) {
                key.append("\\");
            }
            key.append(c);
            escaped = false;
        }

        private void appendValue(char c) {
            if (escaped) {
                value.append("\\");
            }
            value.append(c);
            escaped = false;
        }

        private void putEntry(boolean array) {
            values.put(Tools.concat("", (!group.isEmpty() ? "." : "") + key, '.', group), array ? Arrays.toString(arrayValues.toArray()) :value.toString());
            this.clear();
            this.escaped = false;
        }

        private void putArrayValue() {
            arrayValues.add(value.toString());
            value.setLength(0);
            escaped = false;
        }

        private void putValue() {
            value.append(value.toString());
            value.setLength(0);
            escaped = false;
        }

        private void pushGroup() {
            group.push(key.toString());
            key.setLength(0);
        }

        private void popGroup() {
            if (group.isEmpty()) {
                this.finished = true;
                return;
            }
            group.pop();
        }

        private void clear() {
            key.setLength(0);
            value.setLength(0);
            arrayValues.clear();
        }
    }
}
