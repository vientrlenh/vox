package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/**
 * Màn chấm cho nhà trường: xem theo candidateResultId, không cần phân công.
 * Nhà trường luôn xem được bất kỳ bài PENDING_REVIEW nào của trường mình, kể cả
 * bài chưa có ai được gán hoặc đang gán cho giáo viên khác.
 */
@Service
public class ViewGradingTaskDetailBySchoolUseCase implements IUseCase<UUID, GradingTaskDetailInfo> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewGradingTaskDetailBySchoolUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public GradingTaskDetailInfo execute(UUID candidateResultId) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        var schoolId = examGradingAccessService.requireCurrentSchoolId(currentUserId);
        return examGradingQueryRepository.findTaskDetailBySchool(candidateResultId, schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi của trường bạn."));
    }
}
