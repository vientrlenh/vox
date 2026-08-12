package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicy;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
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
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
public class ZeroScoreExamResultService {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(ZeroScoreExamResultService.class);

    /**
     * Chỉ còn là mức lùi khi không tra được thang của rubric -- KHÔNG còn là "điểm của bài trống".
     *
     * <p>Sửa 2026-08-11: điểm bài trống nay là SÀN CỦA THANG rubric (xem {@code floorScore}). Số 0
     * chỉ tình cờ đúng với thang bắt đầu từ 0; trường khai thang 4-10 thì 0 nằm dưới sàn và không
     * dải xếp loại nào chứa nó -- đúng những bài đáng ra phải được ghi nhận lại là những bài mất
     * xếp loại.
     */
    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamPaperRepository examPaperRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;

    public ZeroScoreExamResultService(
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamPaperRepository examPaperRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository) {
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examPaperRepository = examPaperRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
    }

    @Transactional
    public ExamCandidateResult releaseZeroForEmptySession(UUID sessionId) {
        var session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        return releaseZeroForSession(session, Instant.now());
    }

    @Transactional
    public void ensureZeroResultsForMissingOrEmptyAttempts(UUID examId) {
        var exam = examRepository.findById(examId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var policy = resolvePolicy(exam);
        var now = Instant.now();
        var fallbackPaperId = examPaperRepository.findByExamId(examId).stream()
            .findFirst()
            .map(paper -> paper.getId())
            .orElse(null);

        for (var candidate : examCandidateRepository.findByExamId(examId)) {
            if (ExamCandidateStatus.isNonScorable(candidate.getStatus())) {
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

    private ExamCandidateResult releaseZeroForSession(ExamSession session, Instant now) {
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        return releaseZeroForSession(session, resolvePolicy(exam), now);
    }

    private ExamCandidateResult releaseZeroForSession(ExamSession session, AssessmentPolicy policy, Instant now) {
        var existing = examCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        var result = existing == null ? new ExamCandidateResult() : existing;
        var floorScore = floorScore(policy);
        var rubricBand = resolveRubricBandForZero(policy, floorScore);

        result.setExamId(session.getExamId());
        result.setCandidateId(session.getCandidateId());
        result.setSessionId(session.getId());
        result.setAssessmentPolicyId(policy.getId());
        result.setPolicyVersion(policy.getVersion());
        result.setRubricVersionId(policy.getRubricVersionId());
        result.setFrameworkVersionId(policy.getFrameworkVersionId());
        result.setTargetFrameworkBandId(policy.getTargetFrameworkBandId());
        result.setRubricResultBandId(rubricBand == null ? null : rubricBand.getId());
        result.setTotalScore(floorScore);
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

    private BigDecimal floorScore(AssessmentPolicy policy) {
        if (policy.getRubricVersionId() == null) {
            return ZERO_SCORE;
        }
        return rubricVersionRepository.findById(policy.getRubricVersionId())
            .map(version -> version.getScoringScaleMin())
            .map(min -> min.setScale(2, RoundingMode.HALF_UP))
            .orElse(ZERO_SCORE);
    }

    private RubricResultBand resolveRubricBandForZero(AssessmentPolicy policy, BigDecimal floorScore) {
        if (policy.getRubricVersionId() == null) {
            return null;
        }
        var matchingBands = rubricResultBandRepository.findByRubricVersionId(policy.getRubricVersionId()).stream()
            .sorted(Comparator.comparingInt(band -> band.getOrder()))
            .filter(band -> band.getScoreMin() != null
                && band.getScoreMax() != null
                && floorScore.compareTo(band.getScoreMin()) >= 0
                && floorScore.compareTo(band.getScoreMax()) <= 0)
            .toList();
        if (matchingBands.isEmpty()) {
            LOGGER.info(
                "Không có dải điểm kết quả nào chứa {} (rubricVersionId={}) -- lưu kết quả điểm sàn không kèm xếp loại.",
                floorScore, policy.getRubricVersionId()
            );
            return null;
        }
        if (matchingBands.size() > 1) {
            LOGGER.error(
                "Cấu hình dải điểm CHỒNG LẤN tại {}: khớp {} dải của rubricVersionId={}."
                    + " Lấy dải order nhỏ nhất ({}). Quản trị cần sửa lại dải.",
                floorScore, matchingBands.size(), policy.getRubricVersionId(),
                matchingBands.get(0).getCode()
            );
        }
        return matchingBands.get(0);
    }
}
