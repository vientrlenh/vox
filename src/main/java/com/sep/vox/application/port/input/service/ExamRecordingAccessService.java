package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
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
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private final UserContextPort userContextPort;

    public ExamRecordingAccessService(
            ExamSessionRepository examSessionRepository,
            ExamRepository examRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamMemberRepository examMemberRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository,
            UserContextPort userContextPort) {
        this.examSessionRepository = examSessionRepository;
        this.examRepository = examRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examMemberRepository = examMemberRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
        this.userContextPort = userContextPort;
    }

    /**
     * Ném nếu người gọi không được xem; trả về phiên thi nếu được.
     *
     * <p>Bốn tầng: phải cùng trường → school admin đi thẳng → giáo viên phải là chủ tịch hội
     * đồng, HOẶC người được phân công chấm chính bài đó, HOẶC giám thị của đúng ca thi. Vai khác
     * đều bị chặn, kể cả chính thí sinh: học sinh không được xem lại video buổi thi của mình qua
     * đường này.
     *
     * <p>Tầng "người chấm" là tầng thêm sau, và nó KHÔNG phải nới lỏng cho tiện: hai vòng
     * {@code SPOT_CHECK} và {@code APPEAL} cố tình giao bài cho giáo viên chưa từng dính tới ca
     * thi (hậu kiểm mà giao lại đúng người coi thi thì còn gì là hậu kiểm). Nếu quyền xem bản ghi
     * chỉ đi theo vai coi thi thì đúng những người bắt buộc phải nghe lại bài mới chấm được lại
     * là những người bị chặn -- mà FE thì nuốt lỗi 403 thành "ca thi không có bản ghi", nên sự cố
     * này im lặng suốt.
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

        // Người được phân công chấm chính bài này. Đặt TRƯỚC nhánh giám thị vì trên màn chấm đây
        // là ca phổ biến nhất, để nó rơi xuống dưới là mỗi lần mở bài thêm hai query thừa.
        //
        // Tính cả dòng phân công đã đóng: bài bị phúc khảo thì người chấm vòng trước là người bị
        // hỏi lại "vì sao chấm thế", mà lúc đó dòng của họ đã COMPLETED. Đây là quyền ĐỌC bằng
        // chứng, không phải quyền ghi điểm -- mọi hành động chấm vẫn qua
        // ExamGradingAccessService.authorizeAssignedTeacher trên đúng dòng đang mở.
        var isAssignedGrader = examCandidateResultRepository.findBySessionId(session.getId())
            .map(result -> examGradingAssignmentRepository
                .existsByCandidateResultIdAndTeacherId(result.getId(), userId))
            .orElse(false);
        if (isAssignedGrader) {
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
