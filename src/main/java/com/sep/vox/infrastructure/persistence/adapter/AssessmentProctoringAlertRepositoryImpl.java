package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;
import com.sep.vox.domain.repository.AssessmentProctoringAlertRepository;
import com.sep.vox.infrastructure.persistence.mapper.AssessmentProctoringAlertMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataAssessmentProctoringAlertRepository;

@Repository
@RequiredArgsConstructor
public class AssessmentProctoringAlertRepositoryImpl implements AssessmentProctoringAlertRepository {

    private final SpringDataAssessmentProctoringAlertRepository springDataAssessmentProctoringAlertRepository;

    /**
     * Two layers of duplicate protection, and the second one is the real one.
     *
     * <p>The existence check first only avoids throwing an exception in the common case (Kafka redelivering
     * a message that was fully processed long ago). But check-then-write has a gap: two consumer instances
     * receiving the same message during a rebalance both pass the check. The unique constraint on
     * {@code event_id} is what closes that gap, so a constraint violation here is NOT an error -- it
     * is the duplicate protection doing its work, and it must be swallowed instead of sending the message to the DLT.
     *
     * <p>{@code saveAndFlush} is used so that the collision surfaces inside this try block, instead of at commit time in
     * another layer where nobody knows what it means any more.
     */
    @Override
    public boolean saveIfAbsent(AssessmentProctoringAlert alert) {
        if (springDataAssessmentProctoringAlertRepository.existsByEventId(alert.getEventId())) {
            return false;
        }
        try {
            springDataAssessmentProctoringAlertRepository.saveAndFlush(AssessmentProctoringAlertMapper.toJpa(alert));
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    @Override
    public List<AssessmentProctoringAlert> findByAssessmentSessionIdOrderByCapturedAt(UUID assessmentSessionId) {
        return springDataAssessmentProctoringAlertRepository.findByAssessmentSessionIdOrderByCapturedAtAsc(assessmentSessionId)
            .stream()
            .map(AssessmentProctoringAlertMapper::toDomain)
            .toList();
    }

    /**
     * The list of types lives in {@link AssessmentProctoringAlert#INTEGRITY_ALERT_TYPES} and is not hard-coded
     * here: it is a business decision ("which alerts make an attempt untrustworthy"), not a storage
     * detail. The adapter only translates it into a query.
     *
     * <p>Deliberately does NOT filter further by {@code level}: with two conditions, old records (MULTIPLE_PERSONS
     * carrying CRITICAL) and new records (carrying WARNING) would fall into two different branches -- exactly the crack
     * being patched.
     */
    @Override
    public boolean hasIntegrityAlert(UUID assessmentSessionId) {
        return springDataAssessmentProctoringAlertRepository
            .existsIntegrityAlert(assessmentSessionId, AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES);
    }

    @Override
    public List<AssessmentProctoringAlert> findByScheduleIdOrderByCapturedAt(UUID scheduleId) {
        return springDataAssessmentProctoringAlertRepository.findByScheduleIdOrderByCapturedAtAsc(scheduleId)
            .stream()
            .map(AssessmentProctoringAlertMapper::toDomain)
            .toList();
    }
}
