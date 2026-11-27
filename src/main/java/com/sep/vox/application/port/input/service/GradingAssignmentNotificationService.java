package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.user.User;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.GradingAssignmentOpenedPayloadV1;
import com.sep.vox.application.mapper.assessmentgrading.GradingResultCode;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Notifies teachers that they have just been given a grading round.
 *
 * <p>Called from the three paths that HAND OUT work: manual assignment ({@code AssignGradingUseCase}), automatic assignment
 * ({@code AutoAssignGradingUseCase}), and grader transfer ({@code ReassignGradingUseCase}).
 * NOT called from {@code ClaimClassTestGradingUseCase} -- there the teacher clicks to take the assessment themselves, and
 * reporting back what they just did is pure noise.
 *
 * <p>Every query is by BATCH: automatic assignment spreads a whole class in one call, so asking per assignment
 * one at a time is N+1 in the middle of a transaction holding a lock.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GradingAssignmentNotificationService {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    /**
     * {@code REQUIRED}: all three callers are already inside the use case's transaction, so the outbox row and
     * the assignment row live or die together -- if the assignment is made and the notification is lost, the teacher
     * does not know they have work until the next time they open the app.
     */
    @Transactional
    public void publishAssigned(List<AssessmentGradingAssignment> assignments, Instant now) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        Map<UUID, AssessmentCandidateResult> resultsById = loadResultsById(assignments);
        Map<UUID, Assessment> assessmentsById = loadAssessmentsById(resultsById.values());
        Map<UUID, String> studentNamesByResultId = studentNames(resultsById.values(), assessmentsById);

        for (AssessmentGradingAssignment assignment : assignments) {
            AssessmentCandidateResult result = resultsById.get(assignment.getCandidateResultId());
            Assessment assessment = result == null ? null : assessmentsById.get(result.getAssessmentId());
            if (result == null || assessment == null || assignment.getTeacherId() == null) {
                log.warn("Skipping the grading-assignment notification, missing data: assignmentId={}", assignment.getId());
                continue;
            }
            saveAssignmentOpenedEvent(
                assignment, assessment, candidateLabel(assessment, result, studentNamesByResultId), now);
        }

        log.info("Notified about the grading assignment for {} assignments", assignments.size());
    }

    private Map<UUID, AssessmentCandidateResult> loadResultsById(List<AssessmentGradingAssignment> assignments) {
        return byId(
            assessmentCandidateResultRepository.findByIdIn(assignments.stream()
                .map(assignment -> assignment.getCandidateResultId())
                .filter(id -> id != null)
                .distinct()
                .toList()),
            result -> result.getId());
    }

    private Map<UUID, Assessment> loadAssessmentsById(Collection<AssessmentCandidateResult> results) {
        return byId(
            assessmentRepository.findByIdIn(results.stream()
                .map(result -> result.getAssessmentId())
                .filter(id -> id != null)
                .distinct()
                .toList()),
            assessment -> assessment.getId());
    }

    private void saveAssignmentOpenedEvent(
            AssessmentGradingAssignment assignment, Assessment assessment, String candidateLabel, Instant now) {
        String payload = jsonSerializationPort.toJson(new GradingAssignmentOpenedPayloadV1(
            assignment.getId(),
            assignment.getTeacherId(),
            assessment.getId(),
            assessment.getName(),
            assessment.getKind(),
            assignment.getRoundType() == null ? null : assignment.getRoundType().name(),
            candidateLabel,
            assignment.getDeadlineAt()
        ));

        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_GRADING_ASSIGNMENT, assignment.getId(),
            EventTypeConstant.GRADING_ASSIGNMENT_OPENED, payload, now
        ));
    }

    /**
     * What the teacher uses to recognize an attempt, chosen by EXACTLY the rule of the grading screen.
     *
     * <p>A centralized assessment is graded blind: the student's name never leaves the data layer, not even into
     * a notification. The 8-character attempt code is the only thing that identifies an attempt on the anonymous screen -- exactly
     * what {@code JpaAssessmentGradingQueryRepository} returns for the grading queue.
     */
    private String candidateLabel(
            Assessment assessment, AssessmentCandidateResult result, Map<UUID, String> studentNamesByResultId) {
        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            return GradingResultCode.of(result.getId());
        }
        String studentName = studentNamesByResultId.get(result.getId());
        return studentName != null ? studentName : GradingResultCode.of(result.getId());
    }

    /**
     * Looks names up only for class tests. Not to save queries but to NEVER load the names of a centralized
     * assessment into memory in this flow -- what is never loaded cannot leak into the payload, even if
     * someone later edits {@link #candidateLabel} by mistake.
     */
    private Map<UUID, String> studentNames(
            Collection<AssessmentCandidateResult> results, Map<UUID, Assessment> assessmentsById) {
        List<AssessmentCandidateResult> classTestResults = results.stream()
            .filter(result -> {
                Assessment assessment = assessmentsById.get(result.getAssessmentId());
                return assessment != null && assessment.getKind() == AssessmentKind.CLASS_TEST;
            })
            .filter(result -> result.getCandidateId() != null)
            .toList();

        if (classTestResults.isEmpty()) {
            return Map.of();
        }
        return studentNamesOfClassTestResults(classTestResults);
    }

    private Map<UUID, String> studentNamesOfClassTestResults(List<AssessmentCandidateResult> classTestResults) {
        Map<UUID, AssessmentCandidate> candidatesById = byId(
            assessmentCandidateRepository.findByIdIn(classTestResults.stream()
                .map(result -> result.getCandidateId())
                .distinct()
                .toList()),
            candidate -> candidate.getId());

        Map<UUID, User> usersById = byId(
            userRepository.findByIdIn(candidatesById.values().stream()
                .map(candidate -> candidate.getStudentId())
                .filter(id -> id != null)
                .distinct()
                .toList()),
            user -> user.getId());

        return namesByResultId(classTestResults, candidatesById, usersById);
    }

    private Map<UUID, String> namesByResultId(
            List<AssessmentCandidateResult> classTestResults,
            Map<UUID, AssessmentCandidate> candidatesById,
            Map<UUID, User> usersById) {
        Map<UUID, String> names = new java.util.HashMap<UUID, String>();
        for (AssessmentCandidateResult result : classTestResults) {
            AssessmentCandidate candidate = candidatesById.get(result.getCandidateId());
            if (candidate == null || candidate.getStudentId() == null) {
                continue;
            }
            User user = usersById.get(candidate.getStudentId());
            if (user == null || user.getFullName() == null) {
                continue;
            }
            names.put(result.getId(), user.getFullName().value());
        }
        return names;
    }

    private <T> Map<UUID, T> byId(List<T> items, Function<T, UUID> idOf) {
        return items.stream().collect(Collectors.toMap(idOf, Function.identity(), (left, right) -> left));
    }
}
