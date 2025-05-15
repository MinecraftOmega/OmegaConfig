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
        return new AnotherFormatReader(filePath);
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
        ARRAY_PRIMITIVE_VALUE,
        ARRAY_STRING_VALUE;

        public boolean value() {
            return this == PRIMITIVE_VALUE || this == STRING_VALUE;
        }

        public boolean string() {
            return this == STRING_VALUE || this == KEY;
        }
    }

    public static class AnotherFormatReader implements IFormatReader {
        public final LinkedHashMap<String, String> values = new LinkedHashMap<>();

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

        public AnotherFormatReader(Path path) throws IOException {
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
                // SKIP WHITESPACE PROCESING WHEN IS NOT CAPTURING NON-STRING-VALUES
                if (Character.isWhitespace(c) && (capturing != CapturingMode.STRING_VALUE)) continue;

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
                        capturing = CapturingMode.ARRAY_PRIMITIVE_VALUE;
                        continue;
                    }

                    case JSON_STRING_LINE -> {
                        // IF IS CAPTURING AND KEY IS ESCAPED THEN SKIP PROCESING
                        if (escaped && (capturing == CapturingMode.KEY || capturing == CapturingMode.STRING_VALUE)) {
                            break;
                        }

                        if (capturing == CapturingMode.ARRAY_PRIMITIVE_VALUE) {
                            capturing = CapturingMode.ARRAY_STRING_VALUE;
                            continue;
                        }

                        // FINISHED CAPTURING
                        if (capturing == CapturingMode.STRING_VALUE) {
                            capturing = CapturingMode.NONE;
                            values.put(Tools.concat("", key.toString(), '.', group), value.toString());
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
                            values.put(Tools.concat("", key.toString(), '.', group), value.toString());
                            capturing = CapturingMode.NONE;
                            key = new StringBuilder();
                            value = new StringBuilder();
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
                }

                // OR BY DEFAULT, CAPTURE
                switch (capturing) {
                    case KEY -> key.append(c);
                    case STRING_VALUE, PRIMITIVE_VALUE -> value.append(c);
                    case NONE -> throw new IllegalStateException("Not capturing values");
                }

                escaped = false;
            }
        }
    }


    public static class FormatReader implements IFormatReader {
        public final LinkedHashMap<String, String> values = new LinkedHashMap<>();

        public FormatReader(Path path) throws IOException {
            // READ JSON STRING
            var in = new FileInputStream(path.toFile());
            char[] data = new String(in.readAllBytes()).toCharArray();
            in.close();

            char[] expected = new char[] { JSON_OBJECT_START };
            char[] permissive = new char[0];
            boolean escaped = false;
            boolean keyCapture = false;
            boolean valueCapture = false;
            boolean valueWhiteCapture = false;
            boolean finished = false;
            final LinkedHashSet<String> group = new LinkedHashSet<>();
            StringBuilder key = new StringBuilder();
            StringBuilder value =new StringBuilder();

            for (char c: data) {
                if (Character.isWhitespace(c) && !keyCapture && !valueCapture) continue;

                if (finished && group.isEmpty())
                    throw new IllegalStateException("JSON object finished reading but still contains data");

                if (!Tools.contains(c, expected) && !valueCapture && !keyCapture) {
                    if (Tools.contains(c, permissive)) {
                        throw new IllegalArgumentException("Expected char(s) " + Arrays.toString(expected) + " but received " + c);
                    }
                    throw new IllegalArgumentException("Expected char(s) " + Arrays.toString(expected) + " but received " + c);
                }

                switch (c) {
                    case JSON_OBJECT_START -> {
                        if (!key.isEmpty() && valueCapture) {
                            group.add(key.toString());
                            key = new StringBuilder();
                        }
                        expected = new char[]{JSON_STRING_LINE, JSON_OBJECT_END}; // key start or object end
                        continue;
                    }

                    case JSON_STRING_LINE -> {
                        if (escaped) {
                            escaped = false;
                            break; // BREAKS THE SWITCH AND JUMP TO STORAGE
                        }

                        if (keyCapture) {
                            keyCapture = false;
                            valueCapture = false;
                            expected = new char[]{JSON_ENTRY_SPLIT};
                            continue;
                        }

                        // FIXME: must accept empty values
                        if (valueCapture && !value.isEmpty()) {
                            valueCapture = false;
                            keyCapture = false;
                            expected = new char[]{JSON_CONTINUE, JSON_OBJECT_END};
                            values.put(Tools.concat("", key.toString(), '.', group), value.toString());
                            key = new StringBuilder();
                            value = new StringBuilder();
                            continue;
                        }

                        if (valueCapture) {
                            continue; // skip char storage
                        }

                        keyCapture = true;
                        valueCapture = false;
                        expected = new char[0];
                        continue;
                    }

                    case JSON_ENTRY_SPLIT -> {
                        if (keyCapture || valueCapture) {
                            continue; // IF WE ARE ALREADY CAPTURING THEN ITS PART OF THE VALUE
                        }
                        valueCapture = false;
                        keyCapture = false;
                        expected = new char[]{JSON_STRING_LINE, JSON_OBJECT_START, ' '};
                        continue;
                    }

                    case JSON_CONTINUE -> {
                        // TODO: here we are not supposted to accept empty values
                        if (valueCapture && !value.isEmpty()) {
                            valueCapture = false;
                            keyCapture = false;
                            expected = new char[]{JSON_CONTINUE, JSON_OBJECT_END};
                            values.put(Tools.concat("", key.toString(), '.', group), value.toString());
                            key = new StringBuilder();
                            value = new StringBuilder();
                            continue;
                        }
                        expected = new char[]{JSON_STRING_LINE};
                        permissive = new char[]{JSON_OBJECT_END};
                        continue;
                    }

                    case JSON_ESCAPED -> {
                        if (valueCapture || keyCapture) {
                            escaped = true;
                            continue;
                        }

                        throw new IllegalStateException("You cannot escape " + c);
                    }

                    case JSON_OBJECT_END -> {
                        if (group.isEmpty()) {
                            finished = true;
                        }
                        continue;
                    }
                }

                if (keyCapture && valueCapture)
                    throw new IllegalStateException("Cannot capture key and value at the same time");

                if (keyCapture) {
                    key.append(c);
                    continue;
                }

                if (valueCapture) {
                    value.append(c);
                }
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
