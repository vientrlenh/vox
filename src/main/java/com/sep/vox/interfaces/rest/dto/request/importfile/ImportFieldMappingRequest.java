package com.sep.vox.interfaces.rest.dto.request.importfile;

import java.util.List;

import jakarta.validation.constraints.Size;

public record ImportFieldMappingRequest(
    @Size(max = 255, message = "Column name must not exceed 255 characters")
    String column,

    Integer index,

    @Size(max = 50, message = "Alias list must not exceed 50 elements")
    List<@Size(max = 255, message = "Alias column name must not exceed 255 characters") String> aliases,

    @Size(max = 255, message = "Path must not exceed 255 characters")
    String path,

    @Size(max = 50, message = "Date format must not exceed 50 characters")
    String dateFormat
) {
}
