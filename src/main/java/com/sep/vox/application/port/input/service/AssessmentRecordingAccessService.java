package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * "Ai được xem bản ghi của phiên thi này" -- một nguồn sự thật cho mọi đường đọc bản ghi.
 *
 * <p>Tách ra vì có hai người dùng với nhu cầu khác hẳn nhau: app giám thị trên desktop chỉ cần
 * danh sách bản ghi, còn màn chấm bài cần thêm link phát. Nếu mỗi bên tự chép lại khối kiểm
 * quyền này thì sớm muộn hai bản sẽ lệch -- và lệch về quyền xem video buổi thi là loại lỗi
 * không ai nhìn thấy cho tới lúc đã muộn.
 */
@Service
@RequiredArgsConstructor
public class AssessmentRecordingAccessService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private final UserContextPort userContextPort;

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
    public AssessmentSession requireCanViewRecordings(UUID assessmentSessionId) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = userContextPort.getCurrentSchoolId();

        AssessmentSession session = assessmentSessionRepository.findById(assessmentSessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));

        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi"));

        if (schoolId == null || !assessment.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bạn không có quyền xem bản ghi");
        }

        if (userContextPort.isSchoolAdmin()) {
            return session;
        }

        if (!userContextPort.isTeacher()) {
            throw new ForbiddenException("Vai trò hiện tại của bạn không được phép xem bản ghi thi");
        }

        boolean isChair = assessmentMemberRepository
            .existsByAssessmentIdAndUserIdAndRole(assessment.getId(), userId, AssessmentMemberRole.CHAIR);
        if (isChair) {
            return session;
        }

        // The person assigned to grade this very attempt. Placed BEFORE the proctor branch because on the grading screen this
        // is the most common case, and letting it fall below costs two extra queries every time an attempt is opened.
        //
        // It also counts closed assignment rows: for an appealed attempt, the grader of the previous round is the one
        // being asked "why did you grade it this way", and by then their row is already COMPLETED. This is the permission to READ evidence,
        // not the permission to write scores -- every grading action still goes through
        // AssessmentGradingAccessService.authorizeAssignedTeacher on the row that is actually open.
        Boolean isAssignedGrader = assessmentCandidateResultRepository.findBySessionId(session.getId())
            .map(result -> assessmentGradingAssignmentRepository
                .existsByCandidateResultIdAndTeacherId(result.getId(), userId))
            .orElse(false);
        if (isAssignedGrader) {
            return session;
        }

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thi sinh"));
        UUID scheduleId = candidate.getScheduleId();
        boolean isProctor = scheduleId != null
            && assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId);
        if (!isProctor) {
            throw new ForbiddenException("Bạn không được phân công để giám sát ca thi này");
        }
        return session;
    }
}
