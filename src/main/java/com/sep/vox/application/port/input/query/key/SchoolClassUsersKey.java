package com.sep.vox.application.port.input.query.key;

import java.util.UUID;

public record SchoolClassUsersKey(
    UUID schoolClassId, 
    int page, 
    int size
) {
}
