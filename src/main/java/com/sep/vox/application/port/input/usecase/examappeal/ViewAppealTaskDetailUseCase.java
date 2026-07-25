package com.sep.vox.application.port.input.usecase.examappeal;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealTaskDetailInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;

@Service
public class ViewAppealTaskDetailUseCase implements IUseCase<UUID, AppealTaskDetailInfo> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public ViewAppealTaskDetailUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public AppealTaskDetailInfo execute(UUID appealId) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        // Query đã ràng buộc theo reviewerId, nên không tìm thấy đồng nghĩa với
        // "giáo viên này không được phân công vào đơn" -> 403, không phải 404.
        return examAppealQueryRepository.findTaskDetail(appealId, currentUserId)
            .orElseThrow(() -> new ForbiddenException("Bạn không được phân công chấm lại đơn phúc khảo này."));
    }
}
