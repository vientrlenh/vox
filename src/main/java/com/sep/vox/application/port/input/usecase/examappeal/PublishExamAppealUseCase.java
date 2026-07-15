package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamAppealPublishedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.PublishExamAppealCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * Công bố kết quả phúc khảo.
 *
 * <p>Admin quyết <em>điểm cho part</em> chứ không quyết điểm tổng: bản HUMAN/FINALIZED
 * được ghi đè lên part đang phúc khảo, bản AI của part đó chuyển SUPERSEDED, rồi
 * {@link UpsertExamCandidateResultUseCase} tính lại tổng và result band từ toàn bộ
 * item. Nhờ vậy tổng luôn bằng hàm của các item, thay vì phụ thuộc admin nhập đúng.
 */
@Service
public class PublishExamAppealUseCase implements IUseCase<PublishExamAppealCommand, UUID> {

    private static final String HUMAN_GRADER = "HUMAN";

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;
    private final ExamAppealAccessService examAppealAccessService;
    private final EventPublisherPort eventPublisherPort;

    public PublishExamAppealUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            RubricVersionRepository rubricVersionRepository,
            UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase,
            ExamAppealAccessService examAppealAccessService,
            EventPublisherPort eventPublisherPort) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
        this.examAppealAccessService = examAppealAccessService;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public UUID execute(PublishExamAppealCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        examAppealAccessService.authorizeSchoolAdmin(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != ExamAppealStatus.COMPARING) {
            throw new IllegalStateException(
                "Chỉ có thể công bố khi tất cả giám khảo đã nộp báo cáo chấm lại.");
        }
        if (command.partScore() == null) {
            throw new IllegalArgumentException("Phải nhập điểm cho phần thi được phúc khảo.");
        }

        var rubricVersion = rubricVersionRepository.findById(context.candidateResult().getRubricVersionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản rubric của bài thi."));
        if (command.partScore().compareTo(rubricVersion.getScoringScaleMin()) < 0
                || command.partScore().compareTo(rubricVersion.getScoringScaleMax()) > 0) {
            throw new IllegalArgumentException("Điểm công bố phải nằm trong khoảng "
                + rubricVersion.getScoringScaleMin() + " - " + rubricVersion.getScoringScaleMax() + ".");
        }

        var now = OffsetDateTime.now();
        var scoreBefore = context.candidateResult().getTotalScore();

        // Bản AI và toàn bộ báo cáo giám khảo đều lùi về SUPERSEDED, để chỉ còn đúng
        // một bản FINALIZED là nguồn điểm của part này.
        var existing = examItemEvaluationRepository.findByResponseIdIn(List.of(appeal.getResponseId()));
        for (var evaluation : existing) {
            if (evaluation.getStatus() != ExamItemEvaluationStatus.SUPERSEDED) {
                evaluation.setStatus(ExamItemEvaluationStatus.SUPERSEDED);
                examItemEvaluationRepository.save(evaluation);
            }
        }

        var finalEvaluation = new ExamItemEvaluation(
            appeal.getResponseId(),
            appeal.getPaperItemId(),
            ExamEvaluationEngineType.HUMAN,
            HUMAN_GRADER,
            null,
            currentUserId,
            command.partScore(),
            command.partScore(),
            null,
            false,
            null,
            false,
            false,
            null,
            null,
            command.decisionNote(),
            null,
            null,
            ExamItemEvaluationStatus.FINALIZED,
            now
        );
        examItemEvaluationRepository.save(finalEvaluation);

        // Tổng + result band được dẫn xuất lại từ items, không do admin nhập.
        var recalculated = upsertExamCandidateResultUseCase.execute(
            context.candidateResult().getSessionId(), ExamCandidateResultStatus.FINAL);

        appeal.setStatus(ExamAppealStatus.PUBLISHED);
        appeal.setScoreAfter(recalculated.getTotalScore());
        appeal.setDecisionNote(command.decisionNote());
        appeal.setResolvedBy(currentUserId);
        appeal.setResolvedAt(now);
        examResultAppealRepository.save(appeal);

        eventPublisherPort.publish(new ExamAppealPublishedEvent(
            appeal.getId(),
            context.studentId(),
            context.examName(),
            scoreBefore,
            recalculated.getTotalScore()
        ));

        return appeal.getId();
    }
}
