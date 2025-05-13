package org.omegaconfig.impl.codecs;

import org.omegaconfig.api.IComplexCodec;

import java.util.ArrayList;
import java.util.List;

public class ListCodec implements IComplexCodec<List, Object> {

    @Override
    public List decode(String value, Class<Object> subType) {
        if (!value.startsWith("[") || !value.endsWith("]")) return null;

        boolean expectChar = Character.class.equals(subType);
        boolean expectString = String.class.equals(subType);
        boolean expectNumber = subType.isAssignableFrom(Number.class);

        boolean openned = false;
        List<String> items = new ArrayList<>();
        StringBuilder item = new StringBuilder();

        for (char c: value.toCharArray()) {
            if (c == '[' || c == ']') continue;
            if ((c == ' ' || c == '\n') && !openned) continue;
            if (expectNumber) {
                openned = true;
                if (c == ',') {
                    items.add(item.toString());
                    item = new StringBuilder();
                }
            }

            if (expectString && c == '"') {
                if (openned) {
                    openned = false;
                    items.add(item.toString());
                    item = new StringBuilder();
                } else {
                    openned = true;
                }
            }

            if (expectChar && (c == '\'' || c == '"')) {
                if (openned) {
                    openned = false;
                    items.add(item.toString());
                    item = new StringBuilder();
                } else {
                    openned = true;
                }
            }
            if (openned) {
                item.append(c);
            }
        }

        // TODO: you need to parse "items" into subtype list

        return List.of();
    }

    @Override
    public String encode(List instance, Class<?> subType) {
        return "";
    }

    @Override
    public Class<List> type() {
        return List.class;
    }

    private static String toSerializedArray(List<String> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < items.size(); i++) {
            builder.append(items.get(i));
            if (i < items.size() - 1) {
                builder.append(",\n");
            }
        }
        builder.append("]");
        return builder.toString();
    }
}
