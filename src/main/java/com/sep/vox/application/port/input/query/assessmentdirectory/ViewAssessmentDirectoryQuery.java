package com.sep.vox.application.port.input.query.assessmentdirectory;

import java.util.List;
import java.util.UUID;

/**
 * The common input of the four assessment directory queries (classes / school years / students / proctors) --
 * all four take exactly this set of parameters so they share one record instead of four copies.
 *
 * <p>`page` is 1-based.
 *
 * <p>{@code excludeUserIds}: the people the CALLER ALREADY HAS (the assessment's candidates, the schedule's proctors) and
 * does not want to see again. It must be filtered inside the SQL and not left for the client to drop after receiving the page: filtering on the
 * client makes `content` shorter while `totalElements`/`totalPages` still count the dropped people -- after
 * importing one class the picker shows an empty page with a non-zero count. The classes / school years queries ignore this
 * field.
 */
public record ViewAssessmentDirectoryQuery(
    UUID assessmentId,
    String search,
    int page,
    int size,
    List<UUID> excludeUserIds
) {
    public ViewAssessmentDirectoryQuery {
        excludeUserIds = excludeUserIds == null ? List.of() : List.copyOf(excludeUserIds);
    }
}
