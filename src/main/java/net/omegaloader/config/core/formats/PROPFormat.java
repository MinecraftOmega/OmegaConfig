package net.omegaloader.config.core.formats;

import net.omegaloader.config.ConfigSpec;

import java.util.ArrayList;
import java.util.LinkedList;

public class PROPFormat implements IConfigFormat {
    public static final String EXTENSION = ".properties";
    public static final char FORMAT_KEY_DEF_SPLIT = '=';
    public static final char FORMAT_KEY_GROUP_SPLIT = '.';
    public static final char FORMAT_KEY_LINE_SPLIT = '\n';
    public static final char FORMAT_KEY_COMMENT_LINE = '#';
    public static final char FORMAT_EMPTY = ' ';

    @Override
    public String id() {
        return "properties";
    }

    @Override
    public String name() {
        return "Properties";
    }

    @Override
    public void serialize(ConfigSpec spec) {
    }

    @Override
    public boolean deserialize(ConfigSpec spec) {
        try {
            final var random = spec.file;
            final var comments = new ArrayList<String>();

            // STORAGE
            final var groups = new LinkedList<String>();
            var comment = new StringBuilder();
            var key = new StringBuilder();
            var value = new StringBuilder();

            // POS
            long pos = 0;
            long lenght = random.length();

            // STATE
            int bait;
            boolean comment_read = false;
            boolean value_read = false;

            // WHILE WE DIDN'T REACH THE END
            while ((bait = random.read()) != -1) {

                // NOTE: READ RETURNS A UNSIGNED BYTE (0 ~ 255)
                // SIGNED BYTE IS -128 ~ 127
                // FORMATS MUST WRITE IN UTF-8 AND READ/CONVERT TO UTF-16
                char c = (char) (random.read() & 0xFF); // NORMALIZE, THIS IS DANGEROUS BUT FUCK IT

                switch (c) {
                    case FORMAT_KEY_LINE_SPLIT:// DISPATCH
                        if (comment_read) {
                            comments.add(comment.toString());
                            break; // nothing to do
                        }

                        if (value_read) {

                        } else { // key-read
                            throw new RuntimeException("Broken");
                        }

                        // CLEANUP
                        comment_read = false;
                        value_read = false;
                        groups.clear();
                        comments.clear();
                        key = new StringBuilder();
                        value = new StringBuilder();
                        break;
                    case FORMAT_KEY_GROUP_SPLIT: // SKIP TO NEXT GROUP
                        if (!key.isEmpty() && !value_read && !comment_read) {
                            groups.add(key.toString());
                            key = new StringBuilder();
                            break;
                        }
                    case FORMAT_KEY_DEF_SPLIT:
                        if (!comment_read && !value_read && !key.isEmpty()) {
                            value_read = true;
                            break;
                        }
                    case FORMAT_KEY_COMMENT_LINE: // SET READING-COMMENT STATE
                        if (key.isEmpty() && value.isEmpty() && groups.isEmpty()) {
                            comment_read = true;
                            break;
                        }
                    default:
                        if (comment_read) {
                            comment.append(c);
                        }
                        ((value_read ? value : key)).append(c);
                        break;
                }
            }

            return true;
        } catch (Exception e) {

            return false;
        }
    }
}
