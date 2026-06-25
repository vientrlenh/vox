package com.sep.vox.application.port.input.command;

public record CreateSchoolDirectoryCommand(
    String code, 
    String name, 
    String provinceCode, 
    String provinceName, 
    String districtName, 
    String domain, 
    String address
) {
    
}
