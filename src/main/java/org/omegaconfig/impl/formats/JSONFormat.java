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
