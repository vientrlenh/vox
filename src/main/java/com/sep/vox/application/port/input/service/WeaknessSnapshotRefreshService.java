package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.WeaknessObservationRepository;
import com.sep.vox.domain.repository.personalization.LearnerWeaknessSnapshotRepository;
import com.sep.vox.domain.repository.personalization.SubAttributePriorityRepository;
import com.sep.vox.domain.repository.personalization.WeaknessScoreObservationViewRepository;

@Service
public class WeaknessSnapshotRefreshService {

    private final WeaknessScoreObservationViewRepository scoreObservationView;
    private final ExamCandidateRepository examCandidateRepository;
    private final WeaknessObservationRepository weaknessObservationRepository;
    private final LearnerWeaknessSnapshotRepository snapshotRepository;
    private final SubAttributePriorityRepository priorityRepository;
    private final WeaknessVectorCalculator calculator;
    private final WeaknessVectorSettings settings;

    public WeaknessSnapshotRefreshService(
            WeaknessScoreObservationViewRepository scoreObservationView,
            ExamCandidateRepository examCandidateRepository,
            WeaknessObservationRepository weaknessObservationRepository,
            LearnerWeaknessSnapshotRepository snapshotRepository,
            SubAttributePriorityRepository priorityRepository,
            WeaknessVectorCalculator calculator,
            WeaknessVectorSettings settings) {
        this.scoreObservationView = scoreObservationView;
        this.examCandidateRepository = examCandidateRepository;
        this.weaknessObservationRepository = weaknessObservationRepository;
        this.snapshotRepository = snapshotRepository;
        this.priorityRepository = priorityRepository;
        this.calculator = calculator;
        this.settings = settings;
    }

    public int refreshStaleBatch(Instant now) {
        var studentIds = scoreObservationView.findStudentsNeedingRefresh(
            now.minus(settings.staleAfter()),
            settings.batchSize()
        );
        refreshStudents(studentIds, now);
        return studentIds.size();
    }

    public int refreshExam(UUID examId, Instant now) {
        var studentIds = examCandidateRepository.findUnblockedStudentIdsByExamId(examId);
        var refreshed = 0;
        for (var start = 0; start < studentIds.size(); start += settings.batchSize()) {
            var end = Math.min(start + settings.batchSize(), studentIds.size());
            var batch = studentIds.subList(start, end);
            refreshStudents(batch, now);
            refreshed += batch.size();
        }
        return refreshed;
    }

    @Transactional
    public void refreshStudents(List<UUID> studentIds, Instant now) {
        if (studentIds.isEmpty()) {
            return;
        }
        var frequencies = weaknessObservationRepository.findWeaknessFrequencies(
            studentIds,
            now.minus(settings.observationWindow()),
            now.minus(settings.recentObservationWindow())
        );
        var result = calculator.calculate(
            scoreObservationView.findAllValidScoreObservations(),
            new HashSet<>(studentIds),
            frequencies,
            now,
            settings
        );
        snapshotRepository.replaceForStudents(studentIds, result.snapshots());
        priorityRepository.replaceForStudents(studentIds, result.priorities());
    }
}
