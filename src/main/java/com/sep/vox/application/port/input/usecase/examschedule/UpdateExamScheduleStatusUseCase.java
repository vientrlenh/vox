package com.sep.vox.application.port.input.usecase.examschedule;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamScheduleStatusCommand;
import com.sep.vox.application.port.input.service.ExamScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.mapper.ExamScheduleDtoMapper;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamSchedule;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Service
public class UpdateExamScheduleStatusUseCase implements IUseCase<UpdateExamScheduleStatusCommand, ExamScheduleDto> {

    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamScheduleProctorConflictValidator examScheduleProctorConflictValidator;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public UpdateExamScheduleStatusUseCase(
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamScheduleProctorConflictValidator examScheduleProctorConflictValidator,
            ExamCandidateRepository examCandidateRepository,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            UserContextPort userContextPort) {
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examScheduleProctorConflictValidator = examScheduleProctorConflictValidator;
        this.examCandidateRepository = examCandidateRepository;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public ExamScheduleDto execute(UpdateExamScheduleStatusCommand input) {
        var schedule = examScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getExamId().equals(input.examId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        var exam = examRepository.findById(schedule.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);

        var now = Instant.now();
        var action = input.action() == null ? "" : input.action().trim().toUpperCase();
        switch (action) {
            case "PUBLISH" -> publish(schedule);
            // Dời ca là thao tác xếp lịch nên bị khoá khi kỳ thi đã bắt đầu; publish/complete/cancel
            // là thao tác vận hành trong lúc thi nên vẫn phải chạy được.
            case "MOVE" -> {
                requireExamNotStarted(exam);
                move(schedule, input.targetScheduleId(), currentUserId, now);
            }
            case "COMPLETE" -> complete(schedule);
            case "CANCEL" -> cancel(schedule);
            default -> throw new IllegalArgumentException("Hành động không hợp lệ");
        }

        schedule.setUpdatedAt(now);
        schedule.setUpdatedBy(currentUserId);
        return ExamScheduleDtoMapper.toDto(examScheduleRepository.save(schedule));
    }

    private void requireExamNotStarted(Exam exam) {
        if (exam.isLockedForEditing()) {
            throw new IllegalStateException("Không thể thay đổi lịch thi khi kỳ thi đã bắt đầu");
        }
    }

    private void publish(ExamSchedule schedule) {
        if (schedule.getStatus() != ExamScheduleStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể công bố ca thi ở trạng thái nháp");
        }
        if (examScheduleProctorRepository.countByScheduleId(schedule.getId()) < 1) {
            throw new IllegalStateException("Cần ít nhất 1 giám thị trước khi công bố ca thi");
        }
        requireEveryCandidateHasPaper(schedule);
        schedule.setStatus(ExamScheduleStatus.PUBLISHED);
    }

    /**
     * Ca đã công bố là ca học sinh và giám thị nhìn thấy và sẽ vào thi thật, nên phải chốt ngay ở
     * đây: thiếu đề thì mãi tới lúc vào phòng mới nổ ({@code VerifyExamScheduleOtpUseCase}). Thí sinh
     * đã miễn thi hoặc đã huỷ không vào phòng nên không cần đề, và cũng không làm ca "có người" --
     * dùng chung cách phân loại với {@link ExamCandidateStatus}.
     */
    private void requireEveryCandidateHasPaper(ExamSchedule schedule) {
        var candidates = examCandidateRepository.findByScheduleId(schedule.getId()).stream()
            .filter(candidate -> !ExamCandidateStatus.isNonScorable(candidate.getStatus()))
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
     * giữ người -- và màn điểm danh vẫn join ra ca nguồn qua exam_schedule_proctors.
     * Không kiểm sức chứa phòng vì school_rooms không có cột capacity.
     */
    private void move(ExamSchedule schedule, UUID targetScheduleId, UUID currentUserId, Instant now) {
        if (schedule.getStatus() != ExamScheduleStatus.DRAFT && schedule.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể chuyển ca thi ở trạng thái nháp hoặc đã công bố");
        }
        if (targetScheduleId == null) {
            throw new IllegalArgumentException("Ca thi đích là bắt buộc khi chuyển");
        }
        if (targetScheduleId.equals(schedule.getId())) {
            throw new IllegalStateException("Ca thi đích phải khác ca thi hiện tại");
        }
        // Khoá hàng ca đích để không chạy song song với thao tác xếp thí sinh vào đúng ca đó.
        var target = examScheduleRepository.findByIdForUpdate(targetScheduleId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi đích"));
        if (!target.getExamId().equals(schedule.getExamId())) {
            throw new IllegalStateException("Ca thi đích không thuộc cùng bài kiểm tra");
        }
        if (target.getStatus() != ExamScheduleStatus.DRAFT && target.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Ca thi đích không ở trạng thái hợp lệ");
        }

        moveCandidates(schedule.getId(), target.getId(), currentUserId, now);
        moveProctors(schedule.getId(), target);

        schedule.setStatus(ExamScheduleStatus.MOVED);
        schedule.setMovedToScheduleId(targetScheduleId);
    }

    private void moveCandidates(UUID sourceScheduleId, UUID targetScheduleId, UUID currentUserId, Instant now) {
        var candidates = examCandidateRepository.findByScheduleId(sourceScheduleId);
        if (candidates.isEmpty()) {
            return;
        }
        candidates.forEach(candidate -> candidate.assignToSchedule(targetScheduleId, now, currentUserId));
        examCandidateRepository.saveAll(candidates);
    }

    private void moveProctors(UUID sourceScheduleId, ExamSchedule target) {
        for (var proctor : examScheduleProctorRepository.findByScheduleId(sourceScheduleId)) {
            // Bảng có unique (schedule_id, teacher_id) nên bỏ qua giám thị đã có sẵn ở ca đích,
            // nhưng vẫn phải xoá dòng cũ để ca nguồn không còn giám thị nào.
            if (!examScheduleProctorRepository.existsByScheduleIdAndTeacherId(target.getId(), proctor.getTeacherId())) {
                // Ca đích có khung giờ khác ca nguồn nên phải soát lại: giám thị đi theo có thể đâm
                // vào một ca thứ ba. Loại ca nguồn khỏi phép kiểm tra vì nó sắp thành MOVED.
                examScheduleProctorConflictValidator.requireTeacherFree(
                    proctor.getTeacherId(), target.getStartDate(), target.getEndDate(), sourceScheduleId);
                examScheduleProctorRepository.save(new ExamScheduleProctor(target.getId(), proctor.getTeacherId()));
            }
            examScheduleProctorRepository.deleteById(proctor.getId());
        }
    }

    private void complete(ExamSchedule schedule) {
        if (schedule.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể hoàn thành ca thi đã công bố");
        }
        schedule.setStatus(ExamScheduleStatus.COMPLETED);
    }

    private void cancel(ExamSchedule schedule) {
        if (schedule.getStatus() != ExamScheduleStatus.DRAFT && schedule.getStatus() != ExamScheduleStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể huỷ ca thi ở trạng thái nháp hoặc đã công bố");
        }
        schedule.setStatus(ExamScheduleStatus.CANCELLED);
    }

    private UUID authorize(Exam exam) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin && currentSchoolId != null && currentSchoolId.equals(exam.getSchoolId())) {
            return currentUserId;
        }
        if (examMemberRepository.existsByExamIdAndUserIdAndRole(exam.getId(), currentUserId, ExamMemberRole.CHAIR)) {
            return currentUserId;
        }
        throw new ForbiddenException("Quyền truy cập bị từ chối");
    }
}
