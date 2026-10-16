package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.GradingStatsInfo;
import com.sep.vox.application.projection.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Thẻ số đầu màn chấm bài trên lớp của giáo viên tạo bài.
 *
 * <p>Tách khỏi {@link ViewGradingStatsUseCase} thay vì nới nó: cái kia nhận
 * {@code assessmentId} rỗng nghĩa là "toàn trường", nên nới quyền ở đó là để một giáo viên
 * đọc được tiến độ chấm của cả trường. Ở đây {@code assessmentId} bắt buộc và phải là bài
 * mà người gọi làm CHAIR.
 *
 * <p>Đếm theo BÀI THI chứ không theo học sinh: một em thi hai lượt là hai bài phải
 * chấm, nên tổng lớn hơn sĩ số lớp là đúng, không phải lỗi trùng dòng.
 */
@Service
public class ViewClassTestGradingStatsUseCase implements IUseCase<UUID, GradingStatsInfo> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewClassTestGradingStatsUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public GradingStatsInfo execute(UUID assessmentId) {
        var currentUserId = assessmentGradingAccessService.requireActiveUserId();
        assessmentGradingAccessService.authorizeClassTestChair(assessmentId, currentUserId);
        var schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        return assessmentGradingQueryRepository.stats(schoolId, assessmentId, null, AssessmentKind.CLASS_TEST.name());
    }
}
