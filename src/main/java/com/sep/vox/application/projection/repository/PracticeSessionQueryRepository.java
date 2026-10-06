package com.sep.vox.application.projection.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.application.projection.dto.CriterionFrameworkInfo;
import com.sep.vox.application.projection.dto.PracticeDashboardCountsInfo;
import com.sep.vox.application.projection.dto.SessionRowInfo;

public interface PracticeSessionQueryRepository {

    Optional<SessionRowInfo> findSessionRow(UUID sessionId, UUID studentId);

    Optional<SessionRowInfo> findSessionRowById(UUID sessionId);

    List<SessionRowInfo> findHistory(UUID studentId, int limit);

    boolean canTeacherReadSession(UUID teacherId, UUID sessionId);

    List<CriterionFrameworkInfo> findCriteriaFrameworks(UUID sessionId);

    PracticeDashboardCountsInfo findDashboardCounts(UUID studentId);

    List<java.time.LocalDate> findCompletedSessionDatesDesc(UUID studentId);
}
