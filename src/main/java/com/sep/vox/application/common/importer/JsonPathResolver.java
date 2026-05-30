package com.sep.vox.application.common.importer;

import java.util.List;
import java.util.Map;

public final class JsonPathResolver {

    private JsonPathResolver() {
    }

    public static Object resolve(Map<String, Object> root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        Object current = root;
        for (String token : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            if (token.isBlank()) {
                continue;
            }
            int bracketIndex = token.indexOf('[');
            String key = bracketIndex >= 0 ? token.substring(0, bracketIndex) : token;
            if (current instanceof Map<?, ?> map) {
                current = map.get(key);
            } else {
                return null;
            }
            if (bracketIndex >= 0) {
                while (bracketIndex >= 0) {
                    int end = token.indexOf(']', bracketIndex);
                    if (end < 0) {
                        return null;
                    }
                    String indexText = token.substring(bracketIndex + 1, end);
                    int index;
                    try {
                        index = Integer.parseInt(indexText);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    if (current instanceof List<?> list) {
                        if (index < 0 || index >= list.size()) {
                            return null;
                        }
                        current = list.get(index);
                    } else {
                        return null;
                    }
                    bracketIndex = token.indexOf('[', end + 1);
                }
            }
        }
        return current;
    }
}
