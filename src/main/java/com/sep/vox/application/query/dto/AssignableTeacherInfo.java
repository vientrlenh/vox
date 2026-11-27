package com.sep.vox.application.query.dto;

import java.util.UUID;

/** A teacher who can take submissions, with their current load (number of ASSIGNED submissions held). */
public record AssignableTeacherInfo(
    UUID id,
    String name,
    long load
) {
}
