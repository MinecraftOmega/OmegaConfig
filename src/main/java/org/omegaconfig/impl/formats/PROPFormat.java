package org.omegaconfig.impl.formats;

import org.omegaconfig.ConfigGroup;
import org.omegaconfig.ConfigSpec;
import org.omegaconfig.OmegaConfig;
import org.omegaconfig.Tools;
import org.omegaconfig.api.IConfigField;
import org.omegaconfig.api.IFormat;
import org.omegaconfig.impl.fields.NumberField;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.*;

public class PROPFormat implements IFormat {
    public static final char FORMAT_KEY_DEF_SPLIT = '=';
    public static final char FORMAT_KEY_GROUP_SPLIT = '.';
    public static final char FORMAT_KEY_LINE_SPLIT = '\n';
    public static final char FORMAT_KEY_LINE_TAB = '\t';
    public static final char FORMAT_KEY_COMMENT_LINE = '#';
    public static final char FORMAT_EMPTY = ' ';

    public static final HashMap<Path, RandomAccessFile> OPEN_FILES = new HashMap<>();

    @Override
    public String id() {
        return OmegaConfig.FORMAT_PROPERTIES;
    }

    @Override
    public boolean serialize(ConfigSpec spec) {
        try {
            RandomAccessFile file = OPEN_FILES.computeIfAbsent(spec.path(), path1 -> {
                try {
                    path1.toFile().getParentFile().mkdirs();
                    return new RandomAccessFile(path1.toFile(), "rws");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("Failed to open config file '" + path1 + "'", e);
                }
            });

            file.setLength(0);
            StringBuilder builder = new StringBuilder();

            serialize$put(spec, builder);

            file.write(builder.toString().getBytes());
            System.out.println("Serialization finished");
            return true;
        } catch (Exception e) {
            System.out.println(e.getCause());
            return false;
        }
    }

    private static void serialize$put(ConfigGroup group, StringBuilder builder) {
        for (IConfigField<?, ?> field: group.getFields()) {
            if (field instanceof ConfigGroup g) {
                serialize$put(g, builder);
                continue;
            }

            for (String c: field.comments()) {
                builder.append(FORMAT_KEY_COMMENT_LINE).append(c).append(FORMAT_KEY_LINE_SPLIT);
            }
            if (field instanceof NumberField<?> numberField) {
                if (numberField.math()) {
                    builder.append(FORMAT_KEY_COMMENT_LINE).append("Accepts mathemathical operations").append(FORMAT_KEY_LINE_SPLIT);
                }
            }

            builder.append(group.name())
                    .append(FORMAT_KEY_GROUP_SPLIT)
                    .append(field.name())
                    .append(FORMAT_KEY_DEF_SPLIT)
                    .append(field.get())
                    .append(FORMAT_KEY_LINE_SPLIT);
        }
    }

    @Override
    public boolean deserialize(ConfigSpec spec, Path path) {
        try {
            final RandomAccessFile file = OPEN_FILES.computeIfAbsent(path, path1 -> {
                try {
                    return new RandomAccessFile(path1.toFile(), "rws");
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("Failed to open config file '" + path1 + "'", e);
                }
            });

            final var comments = new ArrayList<String>();

            // STORAGE
            final var id = new LinkedList<String>();
            var comment = new StringBuilder();
            var key = new StringBuilder();
            var value = new StringBuilder();

            // POS
            long pos = 0;
            long lenght = file.length();

            // STATE
            int bait;
            boolean comment_read = false;
            boolean value_read = false;

            // WHILE WE DIDN'T REACH THE END
            // NOTE: READ RETURNS A UNSIGNED BYTE (0 ~ 255)
            // SIGNED BYTE IS -128 ~ 127
            // FORMATS MUST WRITE IN UTF-8 AND READ/CONVERT TO UTF-16
            while ((bait = file.read()) != -1) {

                switch (bait) {
                    case FORMAT_KEY_LINE_SPLIT:// DISPATCH
                        if (comment_read) {
                            comments.add(comment.toString());
                            break; // nothing to do
                        }

                        if (value_read) {
                            // APPEND KEY TO ID
                            id.add(key.toString());

                            // TODO: not efficient, make new methods in spec to make it way efficient
                            IConfigField<?, ?> configField = spec.findField(Tools.concat(spec.name() + ":", ".", id.toArray(new String[0])));

                            if (configField != null) {
                                String v = value.toString();
                                Object o = OmegaConfig.tryParse(v, configField.type(), configField.subType());

                                configField.set0(o);
                            } else {
                                // TODO: must keep the line (if the dev wants)
                            }
                        } else { // key-read
                            throw new RuntimeException("Broken");
                        }

                        // CLEANUP
                        comment_read = false;
                        value_read = false;
                        id.clear();
                        comments.clear();
                        key = new StringBuilder();
                        value = new StringBuilder();
                        break;
                    case FORMAT_KEY_GROUP_SPLIT: // SKIP TO NEXT GROUP
                        if (!key.isEmpty() && !value_read && !comment_read) {
                            id.add(key.toString());
                            key = new StringBuilder();
                            break;
                        }
                    case FORMAT_KEY_DEF_SPLIT:
                        if (!comment_read && !value_read && !key.isEmpty()) {
                            value_read = true;
                            break;
                        }
                    case FORMAT_KEY_COMMENT_LINE: // SET READING-COMMENT STATE
                        if (key.isEmpty() && value.isEmpty() && id.isEmpty()) {
                            comment_read = true;
                            break;
                        }
                    default:
                        if (comment_read) {
                            comment.append(bait);
                        }
                        ((value_read ? value : key)).append(bait);
                        break;
                }
            }

            return true;
        } catch (Exception e) {

            return false;
        }
    }

    @Override
    public void release() {
        OPEN_FILES.forEach((path, randomAccessFile) -> Tools.closeQuietly(randomAccessFile));
        OPEN_FILES.clear();
    }
}
