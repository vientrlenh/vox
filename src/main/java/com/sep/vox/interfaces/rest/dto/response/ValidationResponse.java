package com.sep.vox.interfaces.rest.dto.response;

import java.util.Map;

public record ValidationResponse(
    Map<String, String> errors, 
    String message
) {
    
    public static ValidationResponse error(Map<String, String> errors) {
        return new ValidationResponse(errors, "Validation failed");
    }
}
