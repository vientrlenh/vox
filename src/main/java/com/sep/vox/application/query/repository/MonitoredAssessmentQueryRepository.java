package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.MonitoredAssessmentSummary;

/**
 * The dedicated read path of the proctoring screen: the assessments that a person HAS PROCTORING PERMISSION for.
 *
 * <p>The permission here is derived from the session itself and not from the council, exactly like
 * {@code ProctorScheduleAccessService}: being assigned to supervise a session means seeing the assessment that contains it. That way
 * the proctoring screen does not have to loosen the permission of {@code AssessmentRepository.findAccessible} -- which is the entry
 * to the assessment MANAGEMENT screen, and loosening it there would open the dashboard to proctors too.
 */
public interface MonitoredAssessmentQueryRepository {

    /**
     * @param assessmentId filter to exactly one assessment; null = every assessment that can be proctored.
     * @param now          the current moment, always needed because {@code liveScheduleCount} counts by it.
     * @param leadUntil    only take sessions that start before this moment and have not ended; null = DROP the time filter.
     *                     Null is used for the read path of one specific assessment: reopening the page of a finished session
     *                     must still show the assessment name, otherwise the top of the page is empty while the session list
     *                     below still lists normally.
     */
    List<MonitoredAssessmentSummary> findMonitorableByTeacher(UUID teacherId, UUID assessmentId, Instant now, Instant leadUntil);

    /** Bản cho school admin: mọi kỳ thi của trường, không cần được phân công ca nào. */
    List<MonitoredAssessmentSummary> findMonitorableBySchool(UUID schoolId, UUID assessmentId, Instant now, Instant leadUntil);
}
