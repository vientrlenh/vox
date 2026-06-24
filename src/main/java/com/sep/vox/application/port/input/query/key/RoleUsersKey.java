package com.sep.vox.application.port.input.query.key;

import java.util.UUID;

public record RoleUsersKey(
    UUID roleId, 
    int page, 
    int size
) {
    
}
