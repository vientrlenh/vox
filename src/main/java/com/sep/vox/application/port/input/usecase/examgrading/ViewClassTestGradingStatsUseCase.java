package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/**
 * Thẻ số đầu màn chấm bài trên lớp của giáo viên tạo bài.
 *
 * <p>Tách khỏi {@link ViewGradingStatsUseCase} thay vì nới nó: cái kia nhận
 * {@code examId} rỗng nghĩa là "toàn trường", nên nới quyền ở đó là để một giáo viên
 * đọc được tiến độ chấm của cả trường. Ở đây {@code examId} bắt buộc và phải là bài
 * mà người gọi làm CHAIR.
 */
@Service
public class ViewClassTestGradingStatsUseCase implements IUseCase<UUID, GradingStatsInfo> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewClassTestGradingStatsUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public GradingStatsInfo execute(UUID examId) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        examGradingAccessService.authorizeClassTestChair(examId, currentUserId);
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        return examGradingQueryRepository.stats(schoolId, examId, null);
    }
}
