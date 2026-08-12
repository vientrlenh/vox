package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolRubricCommand(UUID schoolId, UUID rubricId) {}
