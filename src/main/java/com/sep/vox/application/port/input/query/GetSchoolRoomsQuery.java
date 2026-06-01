package com.sep.vox.application.port.input.query;

public record GetSchoolRoomsQuery(
        int page,
        int size
) {}