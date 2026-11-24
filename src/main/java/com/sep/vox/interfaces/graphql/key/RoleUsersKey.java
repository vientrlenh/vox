package com.sep.vox.interfaces.graphql.key;

import java.util.UUID;

public record RoleUsersKey(
    UUID roleId, 
    int page, 
    int size
) {
    
}
