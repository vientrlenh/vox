package com.sep.vox.interfaces.rest.dto.response;

import java.util.Map;

public record ValidationErrorResponse(Map<String, String> errors, String message) {
    
}
