package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.mapper.AssessmentScheduleDtoMapper;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateAssessmentScheduleStatusUseCase implements IUseCase<UpdateAssessmentScheduleStatusCommand, AssessmentScheduleDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator;
    private final AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateAssessmentScheduleStatusUseCase(
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentScheduleProctorRepository assessmentScheduleProctorRepository,
            AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator,
            AssessmentScheduleCandidateConflictValidator assessmentScheduleCandidateConflictValidator,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentScheduleProctorConflictValidator = assessmentScheduleProctorConflictValidator;
        this.assessmentScheduleCandidateConflictValidator = assessmentScheduleCandidateConflictValidator;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentScheduleDto execute(UpdateAssessmentScheduleStatusCommand input) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getAssessmentId().equals(input.assessmentId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        Assessment assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        UUID currentUserId = authorize(assessment);

        Instant now = Instant.now();
        String action = input.action() == null ? "" : input.action().trim().toUpperCase();
        switch (action) {
            case "PUBLISH" -> publish(schedule);
            // Dời ca là thao tác xếp lịch nên bị khoá khi kỳ thi đã bắt đầu; publish/complete/cancel
            // là thao tác vận hành trong lúc thi nên vẫn phải chạy được.
            case "MOVE" -> {
                requireAssessmentNotStarted(assessment);
                move(schedule, input.targetScheduleId(), currentUserId, now);
            }
            case "COMPLETE" -> complete(schedule);
            case "CANCEL" -> cancel(schedule);
            default -> throw new IllegalArgumentException("Hành động không hợp lệ");
        }

        return AssessmentScheduleDtoMapper.toDto(assessmentScheduleRepository.save(schedule));
    }

    private void requireAssessmentNotStarted(Assessment assessment) {
        if (assessment.isLockedForEditing()) {
            throw new IllegalStateException("Không thể thay đổi lịch thi khi kỳ thi đã bắt đầu");
        }
    }

    private void publish(AssessmentSchedule schedule) {
        if (schedule.getStatus() != AssessmentScheduleStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể công bố ca thi ở trạng thái nháp");
        }
        if (assessmentScheduleProctorRepository.countByScheduleId(schedule.getId()) < 1) {
            throw new IllegalStateException("Cần ít nhất 1 giám thị trước khi công bố ca thi");
        }
        requireEveryCandidateHasPaper(schedule);
        schedule.setStatus(AssessmentScheduleStatus.PUBLISHED);
    }

    /**
     * A published session is one that students and proctors see and will really enter, so it has to be locked down right
     * here: a missing paper would only blow up when entering the room ({@code VerifyAssessmentScheduleOtpUseCase}). A candidate
     * who is exempted or cancelled does not enter the room so needs no paper, and does not make the session "occupied" either --
     * sharing the classification with {@link AssessmentCandidateStatus}.
     */
    private void requireEveryCandidateHasPaper(AssessmentSchedule schedule) {
        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByScheduleId(schedule.getId()).stream()
            .filter(candidate -> !AssessmentCandidateStatus.isNonScorable(candidate.getStatus()))
            .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Ca thi chưa có thí sinh nào, không thể công bố");
        }
        long withoutPaper = candidates.stream()
            .filter(candidate -> candidate.getAssignedPaperId() == null)
            .count();
        if (withoutPaper > 0) {
            throw new IllegalStateException(
                "Còn " + withoutPaper + " học sinh chưa được gán đề, không thể công bố ca thi");
        }
    }

    /**
     * Moving a session: the source session must become completely empty, with every candidate and proctor transferred to the
     * target session. If it were only marked MOVED without moving the people, the target session would be empty while the source session
     * still held them -- and the attendance screen would still join out the source session through assessment_schedule_proctors.
     * Room capacity is not checked because school_rooms has no capacity column.
     */
    private void move(AssessmentSchedule schedule, UUID targetScheduleId, UUID currentUserId, Instant now) {
        if (schedule.getStatus() != AssessmentScheduleStatus.DRAFT && schedule.getStatus() != AssessmentScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể chuyển ca thi ở trạng thái nháp hoặc đã công bố");
        }
        if (targetScheduleId == null) {
            throw new IllegalArgumentException("Ca thi đích là bắt buộc khi chuyển");
        }
        if (targetScheduleId.equals(schedule.getId())) {
            throw new IllegalStateException("Ca thi đích phải khác ca thi hiện tại");
        }
        // Khoá hàng ca đích để không chạy song song với thao tác xếp thí sinh vào đúng ca đó.
        AssessmentSchedule target = assessmentScheduleRepository.findByIdForUpdate(targetScheduleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi đích"));
        if (!target.getAssessmentId().equals(schedule.getAssessmentId())) {
            throw new IllegalStateException("Ca thi đích không thuộc cùng bài kiểm tra");
        }
        if (target.getStatus() != AssessmentScheduleStatus.DRAFT && target.getStatus() != AssessmentScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Ca thi đích không ở trạng thái hợp lệ");
        }

        moveCandidates(schedule.getId(), target, currentUserId, now);
        moveProctors(schedule.getId(), target);

        schedule.setStatus(AssessmentScheduleStatus.MOVED);
        schedule.setMovedToScheduleId(targetScheduleId);
    }

    private void moveCandidates(UUID sourceScheduleId, AssessmentSchedule target, UUID currentUserId, Instant now) {
        List<AssessmentCandidate> candidates = assessmentCandidateRepository.findByScheduleId(sourceScheduleId);
        if (candidates.isEmpty()) {
            return;
        }
        // Ca đích có khung giờ khác ca nguồn nên phải soát lại, đối xứng với moveProctors: thí sinh
        // đi theo có thể đâm vào một ca thứ ba ở kỳ thi khác. Ca nguồn tự bị loại (nó là ca hiện tại
        // của mọi thí sinh ở đây) và cũng sắp thành MOVED. Soát TRỌN lô trước khi đụng vào ai để
        // không để lại nửa vời trong bộ nhớ khi người thứ 40 mới là người vướng.
        assessmentScheduleCandidateConflictValidator.requireCandidatesFree(
            candidates, target.getStartDate(), target.getEndDate());

        candidates.forEach(candidate -> candidate.assignToSchedule(target.getId(), now, currentUserId));
        assessmentCandidateRepository.saveAll(candidates);
    }

    private void moveProctors(UUID sourceScheduleId, AssessmentSchedule target) {
        for (AssessmentScheduleProctor proctor : assessmentScheduleProctorRepository.findByScheduleId(sourceScheduleId)) {
            // Bảng có unique (schedule_id, teacher_id) nên bỏ qua giám thị đã có sẵn ở ca đích,
            // nhưng vẫn phải xoá dòng cũ để ca nguồn không còn giám thị nào.
            if (!assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(target.getId(), proctor.getTeacherId())) {
                // Ca đích có khung giờ khác ca nguồn nên phải soát lại: giám thị đi theo có thể đâm
                // vào một ca thứ ba. Loại ca nguồn khỏi phép kiểm tra vì nó sắp thành MOVED.
                assessmentScheduleProctorConflictValidator.requireTeacherFree(
                    proctor.getTeacherId(), target.getStartDate(), target.getEndDate(), sourceScheduleId);
                assessmentScheduleProctorRepository.save(AssessmentScheduleProctor.builder()
                        .scheduleId(target.getId())
                        .teacherId(proctor.getTeacherId())
                        .build());
            }
            assessmentScheduleProctorRepository.deleteById(proctor.getId());
        }
    }

    private void complete(AssessmentSchedule schedule) {
        if (schedule.getStatus() != AssessmentScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể hoàn thành ca thi đã công bố");
        }
        schedule.setStatus(AssessmentScheduleStatus.COMPLETED);
    }

    private void cancel(AssessmentSchedule schedule) {
        if (schedule.getStatus() != AssessmentScheduleStatus.DRAFT && schedule.getStatus() != AssessmentScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể huỷ ca thi ở trạng thái nháp hoặc đã công bố");
        }
        schedule.setStatus(AssessmentScheduleStatus.CANCELLED);
    }

    private UUID authorize(Assessment assessment) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return currentUserId;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
