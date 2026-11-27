package com.sep.vox.application.response.input.schoolclassuser;

import java.util.UUID;

/** A user who could not be added to the class, with the reason to show to the end user. */
public record BulkCreateSchoolClassUserFailure(UUID userId, String reason) {

}
