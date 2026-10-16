package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSchoolDirectoryCommand(
    String code, 
    String name, 
    UUID wardId, 
    UUID cityId, 
    UUID provinceId, 
    String domain, 
    String streetAddress
) {
    
}
