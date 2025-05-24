package org.omegaconfig.impl.formats;

import org.omegaconfig.OmegaConfig;
import org.omegaconfig.Tools;
import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.api.formats.IFormatWriter;
import org.omegaconfig.api.formats.IFormatCodec;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class PROPFormat implements IFormatCodec {
    public static final String FORMAT_KEY_DEF_SPLIT = "=";
    public static final String FORMAT_KEY_BREAKLINE = "\n";
    public static final String FORMAT_KEY_COMMENT_LINE = "#";
    public static final char FORMAT_KEY_GROUP_SPLIT = '.';

    @Override
    public String id() {
        return OmegaConfig.FORMAT_PROPERTIES;
    }

    @Override
    public String extension() {
        return "." + id();
    }

    @Override
    public String mimeType() {
        return "text/x-java-properties";
    }

    @Override
    public IFormatReader createReader(Path filePath) throws IOException {
        return new FormatReader(filePath);
    }

    @Override
    public IFormatWriter createWritter(Path filePath) throws IOException {
        return new FormatWriter(filePath);
    }

    private static class FormatReader implements IFormatReader {
        private final LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        private final LinkedHashSet<String> groups = new LinkedHashSet<>();

        public FormatReader(Path filePath) throws IOException {
            // TODO: safe maker
            final BufferedReader in = new BufferedReader(new FileReader(filePath.toFile()));

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(FORMAT_KEY_COMMENT_LINE)) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split(FORMAT_KEY_DEF_SPLIT, 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    fields.put(key, value);
                }
            }

            in.close();
        }

        @Override
        public String read(String fieldName) {
            // CONCAT THE GROUPS
            StringBuilder group = new StringBuilder();
            for (String g : this.groups) {
                group.append(g).append(FORMAT_KEY_GROUP_SPLIT);
            }
            return fields.get(group.append(fieldName).toString());
        }

        @Override
        public void push(String group) {
            this.groups.add(group);
        }

        @Override
        public void pop() {
            this.groups.removeLast();
        }

        @Override
        public void close() {
            this.fields.clear();
        }
    }

    private static class FormatWriter implements IFormatWriter {
        private final LinkedHashSet<String> groups = new LinkedHashSet<>();
        private final BufferedWriter out;
        private final StringBuilder data = new StringBuilder();

        public FormatWriter(Path filePath) throws IOException {
            // TODO: safe maker
            this.out = new BufferedWriter(new FileWriter(filePath.toFile()));
        }

        @Override
        public void write(String comment) {
            this.data.append(FORMAT_KEY_COMMENT_LINE + " ")
                    .append(comment)
                    .append(FORMAT_KEY_BREAKLINE);
        }

        @Override
        public void write(String fieldName, String value, Class<?> type, Class<?> subType) {
            this.data.append(Tools.concat("", "", FORMAT_KEY_GROUP_SPLIT, groups))
                    .append(FORMAT_KEY_DEF_SPLIT)
                    .append(value)
                    .append(FORMAT_KEY_BREAKLINE);
        }

        @Override
        public void write(String fieldName, String[] values, Class<?> type, Class<?> subType) {

        }

        @Override
        // TODO: implement a check for re-pushing a wrote group
        public void push(String groupName) {
            this.groups.add(groupName);
            this.data.append(Tools.concat("", "", FORMAT_KEY_GROUP_SPLIT, groups));
        }

        @Override
        public void pop() {
            this.groups.removeLast();
            this.data.append(FORMAT_KEY_BREAKLINE.repeat(2));
        }

        @Override
        public void close() throws IOException {
            String data = this.data.toString();
            if (data.endsWith(FORMAT_KEY_BREAKLINE.repeat(2))) {
                data = data.substring(0, data.length() - FORMAT_KEY_BREAKLINE.length() * 2);
            }
            this.out.write(data);
            this.out.close();
        }
    }
}
