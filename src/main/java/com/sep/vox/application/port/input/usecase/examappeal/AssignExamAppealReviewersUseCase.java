package com.sep.vox.application.port.input.usecase.examappeal;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AssignExamAppealReviewersCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamAppealReviewer;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamAppealReviewerRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

@Service
public class AssignExamAppealReviewersUseCase implements IUseCase<AssignExamAppealReviewersCommand, UUID> {

    static final int MIN_REVIEWERS = 1;
    static final int MAX_REVIEWERS = 5;

    private final ExamResultAppealRepository examResultAppealRepository;
    private final ExamAppealReviewerRepository examAppealReviewerRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public AssignExamAppealReviewersUseCase(
            ExamResultAppealRepository examResultAppealRepository,
            ExamAppealReviewerRepository examAppealReviewerRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examResultAppealRepository = examResultAppealRepository;
        this.examAppealReviewerRepository = examAppealReviewerRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional
    public UUID execute(AssignExamAppealReviewersCommand command) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        var context = examAppealAccessService.load(command.appealId());
        examAppealAccessService.authorizeSchoolAdmin(context, currentUserId);

        var appeal = context.appeal();
        if (appeal.getStatus() != ExamAppealStatus.APPROVED) {
            throw new IllegalStateException("Chỉ có thể phân công giám khảo cho đơn phúc khảo đã được duyệt.");
        }

        var reviewerIds = command.reviewerIds() == null ? new ArrayList<UUID>() : command.reviewerIds();
        if (reviewerIds.size() < MIN_REVIEWERS) {
            throw new IllegalArgumentException("Phải phân công ít nhất " + MIN_REVIEWERS + " giám khảo.");
        }
        if (reviewerIds.size() > MAX_REVIEWERS) {
            throw new IllegalArgumentException("Chỉ được phân công tối đa " + MAX_REVIEWERS + " giám khảo.");
        }
        if (new HashSet<>(reviewerIds).size() != reviewerIds.size()) {
            throw new DuplicatedException("Không được phân công trùng giám khảo.");
        }

        var now = OffsetDateTime.now();
        var reviewers = new ArrayList<ExamAppealReviewer>();
        for (var reviewerId : reviewerIds) {
            if (reviewerId.equals(context.studentId())) {
                throw new IllegalArgumentException("Không thể phân công chính thí sinh làm giám khảo.");
            }
            if (!examAppealAccessService.isTeacherOfSchool(reviewerId, context.schoolId())) {
                throw new IllegalArgumentException("Giám khảo phải là giáo viên thuộc cùng trường với bài thi.");
            }
            reviewers.add(new ExamAppealReviewer(
                command.appealId(),
                reviewerId,
                ExamAppealReviewerStatus.ASSIGNED,
                now,
                currentUserId,
                null
            ));
        }
        examAppealReviewerRepository.saveAll(reviewers);

        appeal.setStatus(ExamAppealStatus.GRADING);
        examResultAppealRepository.save(appeal);

        var candidateResult = context.candidateResult();
        candidateResult.setStatus(ExamCandidateResultStatus.RE_GRADING);
        candidateResult.setUpdatedAt(now);
        candidateResult.setUpdatedBy(currentUserId);
        examCandidateResultRepository.save(candidateResult);

        return appeal.getId();
    }
}
