package com.sep.vox.interfaces.shared;

import org.springframework.graphql.data.ArgumentValue;

public class GraphQLInputChecker {
    
    private GraphQLInputChecker() {}

    // Strict check, where the string come from the input must exist and contain non-blank value
    public static boolean checkStringIfBlank(ArgumentValue<String> stringValue) {
        if (stringValue.isOmitted() || stringValue.isEmpty() || stringValue.value().isBlank()) {
            return false;
        }
        return true;
    }

    // Lenient check, where the string come from the input must exist and contain value (allow blank value)
    public static boolean checkStringIfNull(ArgumentValue<String> stringValue) {
        if (stringValue.isOmitted() || stringValue.isEmpty()) {
            return false;
        }
        return true;
    }
}
