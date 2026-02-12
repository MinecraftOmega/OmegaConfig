package me.srrapero720.waterconfig.impl.fields;

import me.srrapero720.waterconfig.ConfigGroup;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class StringField extends BaseConfigField<String, Void> {
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    public final String startsWith;
    public final String endsWith;
    public final String condition;
    public final boolean allowEmpty;
    public final int regexFlags;
    public final Mode mode;
    private final Pattern compiledPattern;

    public StringField(String name, ConfigGroup group, Set<String> comments, String startsWith, String endsWith, boolean allowEmpty, String condition, int regexFlags, Mode mode, Field field, Object context) {
        super(name, group, comments, field, context);
        this.startsWith = startsWith;
        this.endsWith = endsWith;
        this.allowEmpty = allowEmpty;
        this.condition = condition;
        this.regexFlags = regexFlags;
        this.mode = mode;
        this.compiledPattern = compilePattern(condition, regexFlags, mode);
    }

    public StringField(String name, ConfigGroup group, Set<String> comments, String startsWith, String endsWith, boolean allowEmpty, String condition, int regexFlags, Mode mode, String defaultValue) {
        super(name, group, comments, defaultValue);
        this.startsWith = startsWith;
        this.endsWith = endsWith;
        this.allowEmpty = allowEmpty;
        this.condition = condition;
        this.regexFlags = regexFlags;
        this.mode = mode;
        this.compiledPattern = compilePattern(condition, regexFlags, mode);
    }

    private static Pattern compilePattern(String condition, int regexFlags, Mode mode) {
        if (condition == null || condition.isEmpty() || (mode != Mode.REGEX && mode != Mode.NOT_REGEX)) return null;
        String cacheKey = condition + ":" + regexFlags;
        return PATTERN_CACHE.computeIfAbsent(cacheKey, k -> Pattern.compile(condition, regexFlags));
    }

    @Override
    public void validate() {
        String value = this.get();

        if (value == null) {
            this.reset();
            return;
        }

        if (value.isEmpty() && !this.allowEmpty) {
            this.reset();
            return;
        }

        if (this.startsWith != null && !value.startsWith(this.startsWith)) {
            this.reset();
            return;
        }

        if (this.endsWith != null && !value.endsWith(this.endsWith)) {
            this.reset();
            return;
        }

        if (this.condition != null && !this.condition.isEmpty()) {
            boolean conditionCheck = switch (this.mode) {
                case CONTAINS -> value.contains(this.condition);
                case EQUALS -> value.equals(this.condition);
                case REGEX -> this.compiledPattern.matcher(value).matches();
                case NOT_CONTAINS -> !value.contains(this.condition);
                case NOT_EQUALS -> !value.equals(this.condition);
                case NOT_REGEX -> !this.compiledPattern.matcher(value).matches();
            };
            if (!conditionCheck) {
                this.reset();
            }
        }

    }

    @Override
    public Class<String> type() {
        return String.class;
    }

    public enum Mode {
        CONTAINS,
        EQUALS,
        REGEX,
        NOT_CONTAINS,
        NOT_EQUALS,
        NOT_REGEX
    }
}
