package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
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
     * Throws if the caller may not view; returns the session if they may.
     *
     * <p>Four tiers: must be in the same school → school admin goes straight through → a teacher must be the
     * council chair, OR the person assigned to grade that very attempt, OR a proctor of the exact exam slot. Every other role
     * is blocked, including the candidate themselves: a student may not replay the video of their own exam through
     * this path.
     *
     * <p>The "grader" tier was added later, and it is NOT a loosening for convenience: the {@code SPOT_CHECK} and
     * {@code APPEAL} rounds deliberately hand the attempt to teachers who never took part in the exam slot
     * (a post-check that hands it back to the very proctor is no post-check at all). If recording access
     * followed only the proctor role, the exact people who must listen to the answer to grade it would be
     * the ones blocked -- and the frontend swallows the 403 as "this slot has no recording", so the fault
     * stays silent.
     */
    public AssessmentSession requireCanViewRecordings(UUID assessmentSessionId) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = userContextPort.getCurrentSchoolId();

        AssessmentSession session = assessmentSessionRepository.findById(assessmentSessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));

        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi"));

        if (schoolId == null || !assessment.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("Bạn không có quyền xem bản ghi");
        }

        if (userContextPort.isSchoolAdmin()) {
            return session;
        }

        if (!userContextPort.isTeacher()) {
            throw new ForbiddenException("Vai trò hiện tại của bạn không được phép xem bản ghi thi");
        }

        requireChairGraderOrProctor(session, assessment, userId);
        return session;
    }

    private void requireChairGraderOrProctor(AssessmentSession session, Assessment assessment, UUID userId) {
        boolean isChair = assessmentMemberRepository
            .existsByAssessmentIdAndUserIdAndRole(assessment.getId(), userId, AssessmentMemberRole.CHAIR);
        if (isChair || isAssignedGrader(session, userId)) {
            return;
        }
        requireProctorOfSession(session, userId);
    }

    /**
     * The person assigned to grade this very attempt. Checked BEFORE the proctor branch because on the grading screen this
     * is the most common case, and letting it fall below costs two extra queries every time an attempt is opened.
     *
     * <p>It also counts closed assignment rows: for an appealed attempt, the grader of the previous round is the one
     * being asked "why did you grade it this way", and by then their row is already COMPLETED. This is the permission to READ evidence,
     * not the permission to write scores -- every grading action still goes through
     * AssessmentGradingAccessService.authorizeAssignedTeacher on the row that is actually open.
     */
    private boolean isAssignedGrader(AssessmentSession session, UUID userId) {
        return assessmentCandidateResultRepository.findBySessionId(session.getId())
            .map(result -> assessmentGradingAssignmentRepository
                .existsByCandidateResultIdAndTeacherId(result.getId(), userId))
            .orElse(false);
    }

    private void requireProctorOfSession(AssessmentSession session, UUID userId) {
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thi sinh"));
        UUID scheduleId = candidate.getScheduleId();
        boolean isProctor = scheduleId != null
            && assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId);
        if (!isProctor) {
            throw new ForbiddenException("Bạn không được phân công để giám sát ca thi này");
        }
    }
}
