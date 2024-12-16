package net.omegaloader.config.core.formats.lazy;

import net.omegaloader.config.ConfigSpec;
import net.omegaloader.config.core.formats.IConfigFormat;

import java.util.regex.Pattern;

public class LazyPROPFormat implements IConfigFormat {
    public static final String EXTENSION = ".properties";
    public static final char FORMAT_KEY_DEF_SPLIT = '=';
    public static final char FORMAT_KEY_GROUP_SPLIT = '.';
    public static final char FORMAT_KEY_LINE_SPLIT = '\n';
    public static final char FORMAT_KEY_COMMENT_LINE = '#';
    public static final char FORMAT_EMPTY = ' ';

    private static final Pattern KEY_READER = Pattern.compile("(\\w+)=(\\w+)"); // KEY READER
    

    @Override public String id() { return "properties"; }
    @Override public String name() { return "Properties La<y"; }

    @Override
    public void serialize(ConfigSpec spec) {

    }

    @Override
    public boolean deserialize(ConfigSpec spec) {
        return false;
    }
}
