package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * "Ai được xem bản ghi của phiên thi này" -- một nguồn sự thật cho mọi đường đọc bản ghi.
 *
 * <p>Tách ra vì có hai người dùng với nhu cầu khác hẳn nhau: app giám thị trên desktop chỉ cần
 * danh sách bản ghi, còn màn chấm bài cần thêm link phát. Nếu mỗi bên tự chép lại khối kiểm
 * quyền này thì sớm muộn hai bản sẽ lệch -- và lệch về quyền xem video buổi thi là loại lỗi
 * không ai nhìn thấy cho tới lúc đã muộn.
 */
@Service
public class ExamRecordingAccessService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamRepository examRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final UserContextPort userContextPort;

    public ExamRecordingAccessService(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamMemberRepository examMemberRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            UserContextPort userContextPort) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examMemberRepository = examMemberRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * Ném nếu người gọi không được xem; trả về phiên thi nếu được.
     *
     * <p>Ba tầng, theo đúng thứ tự cũ: phải cùng trường → school admin đi thẳng → giáo viên phải
     * là chủ tịch hội đồng hoặc giám thị của đúng ca thi đó. Vai khác đều bị chặn, kể cả chính
     * thí sinh: học sinh không được xem lại video buổi thi của mình qua đường này.
     */
    public ExamSession requireCanViewRecordings(UUID examSessionId) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();

        var session = examSessionRepository.findById(examSessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));

        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi"));

        if (schoolId == null || !exam.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bạn không có quyền xem bản ghi");
        }

        if (userContextPort.isSchoolAdmin()) {
            return session;
        }

        if (!userContextPort.isTeacher()) {
            throw new ForbiddenException("Vai trò hiện tại của bạn không được phép xem bản ghi thi");
        }

        var isChair = examMemberRepository
            .existsByExamIdAndUserIdAndRole(exam.getId(), userId, ExamMemberRole.CHAIR);
        if (isChair) {
            return session;
        }

        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thi sinh"));
        var scheduleId = candidate.getScheduleId();
        var isProctor = scheduleId != null
            && examScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId);
        if (!isProctor) {
            throw new ForbiddenException("Bạn không được phân công để giám sát ca thi này");
        }
        return session;
    }
}
