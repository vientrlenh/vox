package com.sep.vox.application.port.input.usecase.practicesession;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.practicesession.ViewStudentPracticeSessionDetailQuery;
import com.sep.vox.application.port.input.service.PracticeSessionDetailAssemblyService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeSessionQueryRepository;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;

/**
 * A TEACHER views the practice of the students they teach. Students review their own work through another path:
 * {@link ViewMyPracticeSessionDetailUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ViewStudentPracticeSessionDetailUseCase
        implements IUseCase<ViewStudentPracticeSessionDetailQuery, TeacherPracticeSessionDetail> {

    private final PracticeSessionQueryRepository practiceSessionQueryRepository;
    private final PracticeSessionDetailAssemblyService detailAssemblyService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public TeacherPracticeSessionDetail execute(ViewStudentPracticeSessionDetailQuery input) {
        UUID teacherId = userContextPort.getCurrentAuthenticatedUserId();
        if (!practiceSessionQueryRepository.canTeacherReadSession(teacherId, input.sessionId())) {
            throw new ForbiddenException("You are not allowed to view this practice session.");
        }
        return detailAssemblyService.assemble(input.sessionId());
    }
}
