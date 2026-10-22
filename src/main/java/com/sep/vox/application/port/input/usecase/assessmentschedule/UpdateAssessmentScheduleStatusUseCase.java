package com.sep.vox.application.port.input.usecase.assessmentschedule;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.UpdateAssessmentScheduleStatusCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.assessment.AssessmentScheduleDto;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.assessment.AssessmentScheduleRepository;

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
            UserContextPort userContextPort) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentScheduleRepository = assessmentScheduleRepository;
        this.assessmentScheduleProctorRepository = assessmentScheduleProctorRepository;
        this.assessmentScheduleProctorConflictValidator = assessmentScheduleProctorConflictValidator;
        this.assessmentScheduleCandidateConflictValidator = assessmentScheduleCandidateConflictValidator;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AssessmentScheduleDto execute(UpdateAssessmentScheduleStatusCommand input) {
        var schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getAssessmentId().equals(input.assessmentId())) {
            throw new ResourceNotFoundException("Không tìm thấy ca thi");
        }
        var assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(assessment);

        var now = Instant.now();
        var action = input.action() == null ? "" : input.action().trim().toUpperCase();
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

        schedule.setUpdatedAt(now);
        schedule.setUpdatedBy(currentUserId);
        return AssessmentScheduleDto.toDto(assessmentScheduleRepository.save(schedule));
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
     * Ca đã công bố là ca học sinh và giám thị nhìn thấy và sẽ vào thi thật, nên phải chốt ngay ở
     * đây: thiếu đề thì mãi tới lúc vào phòng mới nổ ({@code VerifyAssessmentScheduleOtpUseCase}). Thí sinh
     * đã miễn thi hoặc đã huỷ không vào phòng nên không cần đề, và cũng không làm ca "có người" --
     * dùng chung cách phân loại với {@link AssessmentCandidateStatus}.
     */
    private void requireEveryCandidateHasPaper(AssessmentSchedule schedule) {
        var candidates = assessmentCandidateRepository.findByScheduleId(schedule.getId()).stream()
            .filter(candidate -> !AssessmentCandidateStatus.isNonScorable(candidate.getStatus()))
            .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Ca thi chưa có thí sinh nào, không thể công bố");
        }
        var withoutPaper = candidates.stream()
            .filter(candidate -> candidate.getAssignedPaperId() == null)
            .count();
        if (withoutPaper > 0) {
            throw new IllegalStateException(
                "Còn " + withoutPaper + " học sinh chưa được gán đề, không thể công bố ca thi");
        }
    }

    /**
     * Dời ca: ca nguồn phải trở nên trống hoàn toàn, mọi thí sinh và giám thị được chuyển sang ca
     * đích. Nếu chỉ đánh dấu MOVED mà không chuyển người thì ca đích trống trơn còn ca nguồn vẫn
     * giữ người -- và màn điểm danh vẫn join ra ca nguồn qua assessment_schedule_proctors.
     * Không kiểm sức chứa phòng vì school_rooms không có cột capacity.
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
        var target = assessmentScheduleRepository.findByIdForUpdate(targetScheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi đích"));
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
        var candidates = assessmentCandidateRepository.findByScheduleId(sourceScheduleId);
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
        for (var proctor : assessmentScheduleProctorRepository.findByScheduleId(sourceScheduleId)) {
            // Bảng có unique (schedule_id, teacher_id) nên bỏ qua giám thị đã có sẵn ở ca đích,
            // nhưng vẫn phải xoá dòng cũ để ca nguồn không còn giám thị nào.
            if (!assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(target.getId(), proctor.getTeacherId())) {
                // Ca đích có khung giờ khác ca nguồn nên phải soát lại: giám thị đi theo có thể đâm
                // vào một ca thứ ba. Loại ca nguồn khỏi phép kiểm tra vì nó sắp thành MOVED.
                assessmentScheduleProctorConflictValidator.requireTeacherFree(
                    proctor.getTeacherId(), target.getStartDate(), target.getEndDate(), sourceScheduleId);
                assessmentScheduleProctorRepository.save(new AssessmentScheduleProctor(target.getId(), proctor.getTeacherId()));
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
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userContextPort.isSchoolAdmin();
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(assessment.getSchoolId())) {
            return currentUserId;
        }
        if (assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
