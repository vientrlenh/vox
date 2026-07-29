package com.sep.vox.application.port.input.usecase.examappeal;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;

@Service
public class ViewMyExamAppealDetailUseCase implements IUseCase<UUID, AppealDetailInfo> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final ExamAppealAccessService examAppealAccessService;

    public ViewMyExamAppealDetailUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            ExamAppealAccessService examAppealAccessService) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.examAppealAccessService = examAppealAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public AppealDetailInfo execute(UUID appealId) {
        var currentUserId = examAppealAccessService.requireActiveUserId();
        return examAppealQueryRepository.findMyDetailById(appealId, currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn phúc khảo."));
    }
}
