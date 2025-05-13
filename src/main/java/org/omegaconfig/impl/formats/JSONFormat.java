package org.omegaconfig.impl.formats;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;
import org.omegaconfig.api.formats.IFormatCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JSONFormat implements IFormatCodec {
    public JSONFormat() {

    }

    @Override
    public String id() {
        return OmegaConfig.FORMAT_JSON;
    }

    @Override
    public String extension() {
        return "." + id();
    }

    @Override
    public String mimeType() {
        return "application/json";
    }

    @Override
    public IFormatReader createReader(Path filePath) throws IOException {

    }

    @Override
    public IFormatWriter createWritter(Path filePath) throws IOException {
        return null;
    }

    private static class CharSpect {
    }

    private static class FormatReader implements IFormatReader {
        private final Map<String, String> entries = new HashMap<>();
        private final Set<String> groups = new LinkedHashSet<>();
        private static final char GROUP_SEPARATOR = '.';

        public FormatReader(Path path) throws IOException {

        }

        private void addEntry(String key, String value) {
            key = removeQuotes(key);
            value = removeQuotes(value);

            // Handle JSON primitive types
            if (value.equals("true") || value.equals("false")) {
                entries.put(key, value);
            } else if (value.equals("null")) {
                entries.put(key, null);
            } else if (value.matches("-?\\d+(\\.\\d+)?")) { // Numbers
                entries.put(key, value);
            } else {
                entries.put(key, value);
            }
        }

        private String removeQuotes(String str) {
            if (str.startsWith("\"") && str.endsWith("\"")) {
                return str.substring(1, str.length() - 1);
            }
            return str;
        }

        @Override
        public String read(String fieldName) {
            StringBuilder groupPath = new StringBuilder();
            for (String group : groups) {
                groupPath.append(group).append(GROUP_SEPARATOR);
            }
            groupPath.append(fieldName);
            return entries.get(groupPath.toString());
        }

        @Override
        public void push(String group) {
            groups.add(group);
        }

        @Override
        public void pop() {
            if (!groups.isEmpty()) {
                String[] array = groups.toArray(new String[0]);
                groups.remove(array[array.length - 1]);
            }
        }

        @Override
        public void close() {
            this.entries.clear();
            this.groups.clear();
        }
    }
}
