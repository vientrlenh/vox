package com.sep.vox.application.port.input.usecase.practicesession;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.PracticeSessionDetailAssemblyService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.TeacherPracticeSessionDetail;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;

/**
 * HỌC SINH xem lại bài luyện của chính mình -- màn tổng kết ngay sau khi kết thúc phiên, và
 * xem lại từ lịch sử luyện tập.
 *
 * Trước đây màn tổng kết gọi nhầm sang {@code studentPracticeSessionDetail}, vốn là endpoint
 * của GIÁO VIÊN xem bài học sinh mình dạy ({@code @PreAuthorize("hasRole('TEACHER')")}), nên
 * học sinh bấm "Hoàn tất" là dính Access Denied. Cùng nội dung nhưng khác luật quyền, nên phải
 * là hai đường riêng.
 */
@Service
public class ViewMyPracticeSessionDetailUseCase {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeSessionDetailAssemblyService detailAssemblyService;
    private final UserContextPort userContextPort;

    public ViewMyPracticeSessionDetailUseCase(
            PracticeSessionRepository practiceSessionRepository,
            PracticeSessionDetailAssemblyService detailAssemblyService,
            UserContextPort userContextPort) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.detailAssemblyService = detailAssemblyService;
        this.userContextPort = userContextPort;
    }

    @Transactional(readOnly = true)
    public TeacherPracticeSessionDetail execute(UUID sessionId) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var session = practiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        // hasRole('STUDENT') mới chỉ nói người gọi LÀ học sinh, chưa nói phiên này của ai --
        // thiếu bước dưới thì bất kỳ học sinh nào cũng đọc được bài của bạn cùng lớp qua id.
        if (!session.getStudentId().equals(studentId)) {
            throw new ForbiddenException("Bạn không được xem phiên luyện này.");
        }
        return detailAssemblyService.assemble(sessionId);
    }
}
