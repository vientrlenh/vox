package com.sep.vox.application.port.input.usecase.practicesession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewStudentPracticeSessionDetailQuery;
import com.sep.vox.application.port.input.service.PracticeSessionDetailAssemblyService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;

/**
 * GIÁO VIÊN xem bài luyện của học sinh mình dạy. Học sinh tự xem lại bài của chính mình đi
 * đường khác: {@link ViewMyPracticeSessionDetailUseCase}.
 */
@Service
public class ViewStudentPracticeSessionDetailUseCase
        implements IUseCase<ViewStudentPracticeSessionDetailQuery, TeacherPracticeSessionDetail> {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeSessionDetailAssemblyService detailAssemblyService;
    private final UserContextPort userContextPort;

    public ViewStudentPracticeSessionDetailUseCase(
            PracticeSessionQueryRepository practiceSessionQueryRepository,
            PracticeSessionDetailAssemblyService detailAssemblyService,
            UserContextPort userContextPort) {
        this.practiceSessionQueryRepository = practiceSessionQueryRepository;
        this.detailAssemblyService = detailAssemblyService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherPracticeSessionDetail execute(ViewStudentPracticeSessionDetailQuery input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        if (!practiceSessionQueryRepository.canTeacherReadSession(teacherId, input.sessionId())) {
            throw new ForbiddenException("Bạn không được xem phiên luyện này.");
        }
        return detailAssemblyService.assemble(input.sessionId());
    }
}
