package com.sep.vox.application.port.input.usecase.examappeal;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;

// Dùng chung type AppealDetailInfo với school admin theo quyết định của chủ dự án -- field
// reviewer vẫn tồn tại trên type này, nhưng FE (web/mobile) không được hỏi/hiện field đó cho
// học sinh (xem useStudentAppealQueries.ts / appeal_api.dart).
@Service
public class ViewMyAppealDetailUseCase implements IUseCase<UUID, AppealDetailInfo> {

    private final ExamAppealQueryRepository examAppealQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyAppealDetailUseCase(
            ExamAppealQueryRepository examAppealQueryRepository,
            UserContextPort userContextPort) {
        this.examAppealQueryRepository = examAppealQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public AppealDetailInfo execute(UUID appealId) {
        return examAppealQueryRepository.findStudentDetailById(
                appealId, userContextPort.getCurrentAuthenticatedUserId())
            .orElseThrow(() -> new ForbiddenException("Bạn không được xem đơn phúc khảo này."));
    }
}
