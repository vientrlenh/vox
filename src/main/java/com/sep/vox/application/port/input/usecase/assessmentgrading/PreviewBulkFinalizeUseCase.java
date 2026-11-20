package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.Assessment;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * Previews the consequences of closing the books, writes NOTHING.
 *
 * <p>This step is mandatory because closing the books is a one-way operation on a whole assessment: the admin
 * needs to see how many attempts nobody has graded and how many appeals are still in progress before
 * deciding to publish on the AI score.
 *
 * <p>Wider than who may close the books by ONE level, and that is intentional: the read permission here is
 * {@code authorizeSchoolAdminOrAssessmentChair} (the chair of the assessment, any kind) while
 * {@link BulkFinalizeAssessmentResultsUseCase} still only accepts the school admin or the chair of a class
 * test. The chair of a centralized assessment can press the publish-results button but cannot close the books,
 * so if they could not read exactly this table, whenever a press fails they would only get a number
 * "N results are still not RELEASED" with no way to find out which N attempts those are. The number
 * here answers exactly that question -- and still keeps the scope inside one assessment.
 */
@Service
public class PreviewBulkFinalizeUseCase implements IUseCase<UUID, BulkFinalizePreviewInfo> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public PreviewBulkFinalizeUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentRepository assessmentRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public BulkFinalizePreviewInfo execute(UUID assessmentId) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra."));
        assessmentGradingAccessService.authorizeSchoolAdminOrAssessmentChair(
            assessment.getSchoolId(), assessment.getId(), currentUserId);
        return assessmentGradingQueryRepository.previewBulkFinalize(assessment.getSchoolId(), assessmentId);
    }
}
