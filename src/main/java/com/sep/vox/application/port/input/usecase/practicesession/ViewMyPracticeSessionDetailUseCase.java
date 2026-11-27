package com.sep.vox.application.port.input.usecase.practicesession;

import com.sep.vox.domain.model.personalization.PracticeSession;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.PracticeSessionDetailAssemblyService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;
import com.sep.vox.domain.repository.PracticeSessionRepository;

/**
 * A STUDENT reviews their own practice -- the summary screen right after the session ends, and the review from the
 * practice history.
 *
 * The summary screen used to call {@code studentPracticeSessionDetail} by mistake, which is the endpoint for a
 * TEACHER viewing the work of the students they teach ({@code @PreAuthorize("hasRole('TEACHER')")}), so students
 * who tapped "Done" hit Access Denied. Same content but different permission rules, so they have to be two separate
 * paths.
 */
@Service
@RequiredArgsConstructor
public class ViewMyPracticeSessionDetailUseCase {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSessionDetailAssemblyService detailAssemblyService;
    private final UserContextPort userContextPort;

    @Transactional(readOnly = true)
    public TeacherPracticeSessionDetail execute(UUID sessionId) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        PracticeSession session = practiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Practice session not found."));
        // hasRole('STUDENT') only says the caller IS a student, not whose session this is -- without the step below any
        // student could read a classmate's work by id.
        if (!session.getStudentId().equals(studentId)) {
            throw new ForbiddenException("You are not allowed to view this practice session.");
        }
        return detailAssemblyService.assemble(sessionId);
    }
}
