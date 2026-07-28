package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.ExamCandidateStatusSupport;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.exam.ExamCandidate;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;

@Service
public class ZeroScoreExamResultService {

    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamPaperRepository examPaperRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricResultBandRepository rubricResultBandRepository;

    public ZeroScoreExamResultService(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamPaperRepository examPaperRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricResultBandRepository rubricResultBandRepository) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examPaperRepository = examPaperRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
    }

    @Transactional
    public ExamCandidateResult releaseZeroForEmptySession(UUID sessionId) {
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        return releaseZeroForSession(session, OffsetDateTime.now());
    }

    @Transactional
    public void ensureZeroResultsForMissingOrEmptyAttempts(UUID examId) {
        var exam = examRepository.findById(examId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var policy = resolvePolicy(exam);
        var now = OffsetDateTime.now();
        var fallbackPaperId = examPaperRepository.findByExamId(examId).stream()
            .findFirst()
            .map(paper -> paper.getId())
            .orElse(null);

        for (var candidate : examCandidateRepository.findByExamId(examId)) {
            if (ExamCandidateStatusSupport.isNonScorable(candidate.getStatus())) {
                continue;
            }
            var sessions = examSessionRepository.findAllByCandidateId(candidate.getId()).stream()
                .filter(session -> examId.equals(session.getExamId()))
                .toList();
            if (sessions.isEmpty()) {
                var paperId = candidate.getAssignedPaperId() == null ? fallbackPaperId : candidate.getAssignedPaperId();
                if (paperId == null) {
                    continue;
                }
                var session = examSessionRepository.save(new ExamSession(
                    examId,
                    candidate.getId(),
                    paperId,
                    now,
                    now,
                    ExamSessionStatus.GRADED,
                    false,
                    null
                ));
                releaseZeroForSession(session, policy, now);
                continue;
            }

            for (var session : sessions) {
                if (examCandidateResultRepository.findBySessionId(session.getId()).isPresent()) {
                    continue;
                }
                if (!examItemResponseRepository.findBySessionId(session.getId()).isEmpty()) {
                    continue;
                }
                session.setStatus(ExamSessionStatus.GRADED);
                if (session.getSubmittedAt() == null) {
                    session.setSubmittedAt(now);
                }
                var saved = examSessionRepository.save(session);
                releaseZeroForSession(saved, policy, now);
            }
        }
    }

    private ExamCandidateResult releaseZeroForSession(ExamSession session, OffsetDateTime now) {
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        return releaseZeroForSession(session, resolvePolicy(exam), now);
    }

    private ExamCandidateResult releaseZeroForSession(ExamSession session, AssessmentPolicy policy, OffsetDateTime now) {
        var existing = examCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        var result = existing == null ? new ExamCandidateResult() : existing;
        var rubricBand = resolveRubricBandForZero(policy);

        result.setExamId(session.getExamId());
        result.setCandidateId(session.getCandidateId());
        result.setSessionId(session.getId());
        result.setAssessmentPolicyId(policy.getId());
        result.setPolicyVersion(policy.getVersion());
        result.setRubricVersionId(policy.getRubricVersionId());
        result.setFrameworkVersionId(policy.getFrameworkVersionId());
        result.setTargetFrameworkBandId(policy.getTargetFrameworkBandId());
        result.setRubricResultBandId(rubricBand == null ? null : rubricBand.getId());
        result.setTotalScore(ZERO_SCORE);
        result.setStatus(ExamCandidateResultStatus.RELEASED);
        if (result.getReleasedAt() == null) {
            result.setReleasedAt(now);
        }
        if (existing == null) {
            result.setCreatedAt(now);
            result.setCreatedBy(null);
        }
        result.setUpdatedAt(now);
        result.setUpdatedBy(null);
        return examCandidateResultRepository.save(result);
    }

    private AssessmentPolicy resolvePolicy(com.sep.vox.domain.model.exam.Exam exam) {
        if (exam.getAssessmentPolicyId() == null) {
            throw new NotFoundException("Bài kiểm tra chưa gắn assessment policy");
        }
        return assessmentPolicyRepository.findById(exam.getAssessmentPolicyId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy assessment policy"));
    }

    private RubricResultBand resolveRubricBandForZero(AssessmentPolicy policy) {
        if (policy.getRubricVersionId() == null) {
            return null;
        }
        var matchingBands = rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
            .sorted(Comparator.comparingInt(RubricResultBand::getOrder))
            .filter(band -> band.getScoreMin() != null
                && band.getScoreMax() != null
                && ZERO_SCORE.compareTo(band.getScoreMin()) >= 0
                && ZERO_SCORE.compareTo(band.getScoreMax()) <= 0)
            .toList();
        if (matchingBands.size() != 1) {
            throw new IllegalStateException("Điểm 0.00 phải thuộc chính xác một dải điểm kết quả, nhưng tìm thấy "
                + matchingBands.size() + " dải.");
        }
        return matchingBands.get(0);
    }
}
