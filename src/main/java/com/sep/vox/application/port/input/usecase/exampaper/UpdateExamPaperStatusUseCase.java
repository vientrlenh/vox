package com.sep.vox.application.port.input.usecase.exampaper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamPaperStatusCommand;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService;
import com.sep.vox.application.port.input.service.ExamPaperAuthoringAccessService.PaperActor;
import com.sep.vox.application.port.input.service.ExamPaperAutoAssigner;
import com.sep.vox.application.port.input.service.ExamTimeQuotaGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaper;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Vòng đời một mã đề. Kỳ thi tập trung có hai đường đi, chọn theo <b>ai soạn ra mã đề đó</b>:
 *
 * <ul>
 *   <li><b>Có người thứ hai</b> — mã đề do người khác soạn: DRAFT → IN_REVIEW → APPROVED → LOCKED,
 *       với {@code requireNotAuthor} chặn người soạn tự duyệt bài của mình. Nhờ vậy trạng thái
 *       {@code APPROVED} luôn có nghĩa "đã qua mắt người thứ hai".</li>
 *   <li><b>Không có người thứ hai</b> — chủ tịch hội đồng hoặc quản trị trường tự soạn mã đề: đi tắt
 *       một bước DRAFT → LOCKED. Bắt họ bấm nộp duyệt rồi tự duyệt chính bài mình chỉ tạo ra một dấu
 *       {@code APPROVED} giả, còn cấm hẳn thì trường ít người không khoá nổi mã đề để phân đề.</li>
 * </ul>
 *
 * <p>Bài trên lớp luôn đi đường tắt: giáo viên tạo bài vừa là CHAIR vừa là người soạn mọi mã đề.
 */
@Service
public class UpdateExamPaperStatusUseCase implements IUseCase<UpdateExamPaperStatusCommand, ExamPaperDto> {

    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamPaperAuthoringAccessService examPaperAuthoringAccessService;
    private final ExamTimeQuotaGuardService examTimeQuotaGuardService;
    private final ExamPaperAutoAssigner examPaperAutoAssigner;
    private final UserContextPort userContextPort;

    public UpdateExamPaperStatusUseCase(
            ExamPaperRepository examPaperRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamPaperAuthoringAccessService examPaperAuthoringAccessService,
            ExamTimeQuotaGuardService examTimeQuotaGuardService,
            ExamPaperAutoAssigner examPaperAutoAssigner,
            UserContextPort userContextPort) {
        this.examPaperRepository = examPaperRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examPaperAuthoringAccessService = examPaperAuthoringAccessService;
        this.examTimeQuotaGuardService = examTimeQuotaGuardService;
        this.examPaperAutoAssigner = examPaperAutoAssigner;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamPaperDto execute(UpdateExamPaperStatusCommand input) {
        var command = new UpdateExamPaperStatusCommand(
            input.paperId(),
            StringNormalization.normalizeCode(input.action()),
            StringNormalization.trimAndCollapseSpaces(input.note())
        );
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var paper = examPaperRepository.findById(command.paperId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đề thi"));
        var exam = examRepository.findById(paper.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var actor = examPaperAuthoringAccessService.resolve(exam, currentUserId);

        switch (command.action()) {
            case "SUBMIT" -> {
                requireCanAuthor(actor);
                requireAllSlotsAssigned(paper, "Đề thi còn ô câu hỏi chưa được gán, không thể nộp duyệt");
                requirePaperWithinPlan(exam, paper);
                requireTransition(paper, ExamPaperStatus.DRAFT, ExamPaperStatus.IN_REVIEW);
            }
            case "APPROVE" -> {
                requireCanReview(actor);
                requireNotAuthor(paper, currentUserId);
                requirePaperWithinPlan(exam, paper);
                requireTransition(paper, ExamPaperStatus.IN_REVIEW, ExamPaperStatus.APPROVED);
            }
            case "REQUEST_REVISION" -> {
                requireCanReview(actor);
                requireNotAuthor(paper, currentUserId);
                if (command.note() == null || command.note().isBlank()) {
                    throw new IllegalStateException("Yêu cầu sửa lại bắt buộc phải có góp ý");
                }
                requireTransition(paper, ExamPaperStatus.IN_REVIEW, ExamPaperStatus.DRAFT);
            }
            case "LOCK" -> {
                requireCanDecide(actor);
                requirePaperWithinPlan(exam, paper);
                if (isOneStepLock(exam, paper, currentUserId)) {
                    requireAllSlotsAssigned(paper, "Mã đề còn ô câu hỏi chưa được gán, không thể khoá");
                    requireTransition(paper, ExamPaperStatus.DRAFT, ExamPaperStatus.LOCKED);
                } else {
                    requireNotAuthor(paper, currentUserId);
                    requireTransition(paper, ExamPaperStatus.APPROVED, ExamPaperStatus.LOCKED);
                }
            }
            case "REOPEN" -> {
                // Không có requireNotAuthor ở đây: người vừa đi đường tắt DRAFT → LOCKED phải mở lại
                // được chính mã đề của mình, nếu không họ tự khoá mình ra ngoài và mã đề kẹt vĩnh viễn.
                requireCanDecide(actor);
                requireTransition(paper, ExamPaperStatus.LOCKED, ExamPaperStatus.DRAFT);
                unassignCandidatesOf(paper, currentUserId);
            }
            default -> throw new IllegalStateException("Action không hợp lệ");
        }

        var now = Instant.now();
        paper.setUpdatedAt(now);
        paper.setUpdatedBy(currentUserId);
        var saved = examPaperRepository.save(paper);

        // Gán bù SAU khi lưu: ExamPaperAutoAssigner đọc lại trạng thái mã đề để chốt "mọi mã đề đã
        // LOCKED", nên trạng thái mới phải nằm trong DB trước đã.
        if ("LOCK".equals(command.action())) {
            examPaperAutoAssigner.backfillExam(exam, now, currentUserId);
        }
        return ExamPaperDtoMapper.toDto(saved);
    }

    /**
     * Mở khoá là để sửa lại mã đề, mà thí sinh đang trỏ vào nó thì bài thi bị đổi dưới chân họ. Trả
     * họ về "chưa phân đề" -- cùng cách xử lý với {@code DeleteExamPaperUseCase}. Khoá lại thì
     * {@link ExamPaperAutoAssigner#backfillExam} gán bù, nên không ai kẹt vĩnh viễn.
     */
    private void unassignCandidatesOf(ExamPaper paper, UUID currentUserId) {
        var affected = examCandidateRepository.findByAssignedPaperId(paper.getId());
        if (affected.isEmpty()) {
            return;
        }
        var now = Instant.now();
        affected.forEach(candidate -> candidate.unassignPaper(now, currentUserId));
        examCandidateRepository.saveAll(affected);
    }

    /**
     * Đi tắt khi không có người thứ hai để duyệt: bài trên lớp (chỉ có một CHAIR), hoặc kỳ thi tập
     * trung mà chính người quyết định là người soạn ra mã đề này.
     */
    private boolean isOneStepLock(Exam exam, ExamPaper paper, UUID currentUserId) {
        return exam.getKind() == ExamKind.CLASS_TEST || currentUserId.equals(paper.getCreatedBy());
    }

    private void requireCanAuthor(PaperActor actor) {
        if (!actor.canAuthor()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireCanReview(PaperActor actor) {
        if (!actor.canReview()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireCanDecide(PaperActor actor) {
        if (!actor.canDecide()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private void requireNotAuthor(ExamPaper paper, UUID currentUserId) {
        if (currentUserId.equals(paper.getCreatedBy())) {
            throw new ForbiddenException("Không được tự duyệt đề thi do chính mình tạo");
        }
    }

    private void requireAllSlotsAssigned(ExamPaper paper, String message) {
        if (examPaperItemRepository.existsUnassignedItemByPaperId(paper.getId())) {
            throw new IllegalStateException(message);
        }
    }

    private void requirePaperWithinPlan(Exam exam, ExamPaper paper) {
        examTimeQuotaGuardService.requireWithinPlan(
            exam.getSchoolId(),
            paper.getTimeDurationSeconds(),
            "Mã đề " + paper.getCode()
        );
    }

    private void requireTransition(ExamPaper paper, ExamPaperStatus from, ExamPaperStatus to) {
        if (paper.getStatus() != from) {
            throw new IllegalStateException("Trạng thái đề thi hiện tại không hợp lệ cho action này");
        }
        paper.setStatus(to);
    }
}
