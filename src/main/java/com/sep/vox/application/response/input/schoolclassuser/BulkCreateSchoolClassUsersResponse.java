package com.sep.vox.application.response.input.schoolclassuser;

import java.util.List;
import java.util.UUID;

public record BulkCreateSchoolClassUsersResponse(
    List<UUID> addedUserIds,
    List<BulkCreateSchoolClassUserFailure> failed
) {

}
