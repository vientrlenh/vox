package com.sep.vox.application.port.input.usecase.practicesession;

import com.sep.vox.application.port.input.command.StartPracticeSessionCommand;
import com.sep.vox.application.port.input.service.StartPracticeSessionPersistenceService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.PracticeAgentAvailabilityPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.PracticeSession;
import org.springframework.stereotype.Service;

/**
 * KHÔNG @Transactional ở tầng này: agentAvailabilityPort.requireReady() gọi ra ngoài (Python
 * readiness check, timeout cấu hình tới 12s) -- giữ connection DB mở suốt lúc đó từng gây
 * HikariCP cạn pool dưới tải (cùng lớp bug với BuildPracticePaperUseCase, sửa cùng đợt). Phần
 * ghi DB thật (đọc đề đã giữ chỗ + tạo session + đổi trạng thái đề) nằm trong
 * StartPracticeSessionPersistenceService, transaction riêng, chạy SAU khi readiness-check xong.
 */
@Service
public class StartPracticeSessionUseCase implements IUseCase<StartPracticeSessionCommand, PracticeSession> {

    private final PracticeAgentAvailabilityPort agentAvailabilityPort;
    private final StartPracticeSessionPersistenceService persistenceService;
    private final UserContextPort userContextPort;

    public StartPracticeSessionUseCase(
            PracticeAgentAvailabilityPort agentAvailabilityPort,
            StartPracticeSessionPersistenceService persistenceService,
            UserContextPort userContextPort) {
        this.agentAvailabilityPort = agentAvailabilityPort;
        this.persistenceService = persistenceService;
        this.userContextPort = userContextPort;
    }

    @Override
    public PracticeSession execute(StartPracticeSessionCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        agentAvailabilityPort.requireReady();
        return persistenceService.persist(studentId, input.paperId());
    }
}
