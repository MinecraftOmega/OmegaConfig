package me.srrapero720.waterconfig.impl.codecs;

import me.srrapero720.waterconfig.WaterConfig;
import me.srrapero720.waterconfig.api.ICodec;
import me.srrapero720.waterconfig.api.IComplexCodec;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Codec for record types. Components are encoded with their own codecs and joined with
 * {@code "; "}; decoding goes through the canonical constructor, so the record's own
 * constructor checks act as validation. Malformed input decodes to null.
 */
public class RecordCodec implements IComplexCodec<Record, Record> {

    @Override
    public String encode(Record instance, Class<?> subType) {
        RecordComponent[] components = instance.getClass().getRecordComponents();
        StringBuilder out = new StringBuilder();
        try {
            for (int i = 0; i < components.length; i++) {
                if (i > 0) {
                    out.append("; ");
                }
                var accessor = components[i].getAccessor();
                accessor.setAccessible(true);
                String encoded = encodeComponent(accessor.invoke(instance));
                // ESCAPE THE COMPONENT SEPARATOR SO VALUES ROUND-TRIP
                out.append(encoded.replace("\\", "\\\\").replace(";", "\\;"));
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to encode record '" + instance.getClass().getName() + "'", e);
        }
        return out.toString();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Record decode(String value, Class<Record> subType) {
        RecordComponent[] components = (subType == null) ? null : subType.getRecordComponents();
        if (components == null) {
            return null;
        }

        List<String> parts = split(value);
        if (parts.size() != components.length) {
            return null;
        }

        try {
            Class<?>[] types = new Class[components.length];
            Object[] args = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                args[i] = decodeComponent(parts.get(i), types[i]);
                if (args[i] == null && types[i].isPrimitive()) {
                    return null;
                }
            }

            Constructor<?> canonical = subType.getDeclaredConstructor(types);
            canonical.setAccessible(true);
            // THE CANONICAL CONSTRUCTOR'S OWN CHECKS ACT AS VALIDATION
            return (Record) canonical.newInstance(args);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Class<Record> type() {
        return Record.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String encodeComponent(Object value) {
        if (value instanceof String s) {
            return s;
        }
        ICodec<Object> codec = (ICodec<Object>) WaterConfig.codecOf(value.getClass());
        if (codec == null) {
            throw new IllegalStateException("Codec for record component type '" + value.getClass().getName() + "' was not founded");
        }
        return codec instanceof IComplexCodec complex ? complex.encode(value, value.getClass()) : codec.encode(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object decodeComponent(String part, Class<?> type) {
        if (type == String.class) {
            return part;
        }
        ICodec<?> codec = WaterConfig.codecOf(type);
        if (codec == null) {
            return null;
        }
        return codec instanceof IComplexCodec complex ? complex.decode(part, type) : codec.decode(part);
    }

    // SPLITS ON UNESCAPED ';' AND DECODES THE \; AND \\ ESCAPES
    private static List<String> split(String value) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                current.append(value.charAt(++i));
                continue;
            }
            if (c == ';') {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        parts.add(current.toString().trim());
        return parts;
    }
}
