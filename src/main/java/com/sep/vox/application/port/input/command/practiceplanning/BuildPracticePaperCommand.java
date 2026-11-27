package com.sep.vox.application.port.input.command.practiceplanning;

import java.util.List;
import java.util.UUID;

/**
 * @param targetFrameworkBandId the band the student CHOOSES for this session. Before, the system derived the
 *     band itself and forced the student to practice at it; now it is the learner's choice, and the band only means
 *     "the difficulty I want today". Required -- no default is right on the student's behalf.
 */
public record BuildPracticePaperCommand(
    UUID topicId,
    UUID targetFrameworkBandId,
    String origin,
    String fromSubAttribute,
    List<UUID> offeredTopicIds,
    List<UUID> previousOfferedTopicIds
) {
}
