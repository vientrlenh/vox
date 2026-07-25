package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;

/**
 * Màn chấm của giáo viên.
 *
 * <p>Không tái dùng {@code ViewExamItemResponseEvaluationUseCase}: nó phân quyền
 * qua {@code ExamResultAccessService}, vốn chỉ mở cho chính thí sinh và thành viên
 * kỳ thi — giáo viên được gán chấm không thuộc nhóm nào trong đó nên sẽ bị chặn.
 * Quyền ở đây đến từ dòng phân công, và query trả về empty khi không khớp.
 */
@Service
public class ViewGradingTaskDetailUseCase implements IUseCase<UUID, GradingTaskDetailInfo> {

    private final ExamGradingQueryRepository examGradingQueryRepository;
    private final ExamGradingAccessService examGradingAccessService;

    public ViewGradingTaskDetailUseCase(
            ExamGradingQueryRepository examGradingQueryRepository,
            ExamGradingAccessService examGradingAccessService) {
        this.examGradingQueryRepository = examGradingQueryRepository;
        this.examGradingAccessService = examGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public GradingTaskDetailInfo execute(UUID assignmentId) {
        var currentUserId = examGradingAccessService.requireActiveUserId();
        return examGradingQueryRepository.findTaskDetail(assignmentId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("BẢO MẬT: Bạn không được phân công chấm bài thi này."));
    }
}
