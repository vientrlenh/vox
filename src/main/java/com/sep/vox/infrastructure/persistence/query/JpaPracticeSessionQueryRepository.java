package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.CriterionFrameworkInfo;
import com.sep.vox.application.query.dto.SessionRowInfo;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeSessionRepository;

@Repository
public class JpaPracticeSessionQueryRepository implements PracticeSessionQueryRepository {

    private final SpringDataPracticeSessionRepository repository;

    public JpaPracticeSessionQueryRepository(SpringDataPracticeSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SessionRowInfo> findSessionRow(UUID sessionId, UUID studentId) {
        return repository.findSessionRow(sessionId, studentId);
    }

    @Override
    public Optional<SessionRowInfo> findSessionRowById(UUID sessionId) {
        return repository.findSessionRowById(sessionId);
    }

    @Override
    public List<SessionRowInfo> findHistory(UUID studentId, int limit) {
        return repository.findHistory(studentId, limit);
    }

    @Override
    public boolean canTeacherReadSession(UUID teacherId, UUID sessionId) {
        return repository.canTeacherReadSession(teacherId, sessionId);
    }

    @Override
    public List<CriterionFrameworkInfo> findCriteriaFrameworks(UUID sessionId) {
        return repository.findCriteriaFrameworks(sessionId);
    }

    @Override
    public com.sep.vox.application.query.dto.PracticeDashboardCountsInfo findDashboardCounts(UUID studentId) {
        return repository.findDashboardCounts(studentId);
    }

    @Override
    public List<java.time.LocalDate> findCompletedSessionDatesDesc(UUID studentId) {
        return repository.findCompletedSessionDatesDesc(studentId);
    }
}
