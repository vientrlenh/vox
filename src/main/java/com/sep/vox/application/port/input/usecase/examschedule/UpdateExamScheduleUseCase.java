package com.sep.vox.application.port.input.usecase.examschedule;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateExamScheduleCommand;
import com.sep.vox.application.port.input.service.ExamScheduleCandidateConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.service.ExamScheduleRoomValidator;
import com.sep.vox.application.port.input.service.SubscriptionPeriodGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.exam.ExamEditingGuard;
import com.sep.vox.domain.service.exam.ExamScheduleWindowMessages;

@Service
public class UpdateExamScheduleUseCase implements IUseCase<UpdateExamScheduleCommand, UUID> {

    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleRoomValidator examScheduleRoomValidator;
    private final ExamScheduleProctorConflictValidator examScheduleProctorConflictValidator;
    private final ExamScheduleCandidateConflictValidator examScheduleCandidateConflictValidator;
    private final ExamMemberRepository examMemberRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final SubscriptionPeriodGuardService subscriptionPeriodGuardService;
    private final UserContextPort userContextPort;

    public UpdateExamScheduleUseCase(
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleRoomValidator examScheduleRoomValidator,
            ExamScheduleProctorConflictValidator examScheduleProctorConflictValidator,
            ExamScheduleCandidateConflictValidator examScheduleCandidateConflictValidator,
            ExamMemberRepository examMemberRepository,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            SubscriptionPeriodGuardService subscriptionPeriodGuardService,
            UserContextPort userContextPort) {
        this.subscriptionPeriodGuardService = subscriptionPeriodGuardService;
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleRoomValidator = examScheduleRoomValidator;
        this.examScheduleProctorConflictValidator = examScheduleProctorConflictValidator;
        this.examScheduleCandidateConflictValidator = examScheduleCandidateConflictValidator;
        this.examMemberRepository = examMemberRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateExamScheduleCommand input) {
        // Khoá hàng ca ngay từ đầu: ba luật chống trùng dưới đây đều soát khung giờ MỚI, nên nếu
        // một luồng khác đang xếp người vào chính ca này thì hai bên phải nối đuôi chứ không được
        // cùng đọc một trạng thái cũ.
        var schedule = examScheduleRepository.findByIdForUpdate(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        var exam = examRepository.findById(schedule.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        var currentUserId = authorize(exam);
        ExamEditingGuard.requireScheduleEditable(exam);

        var isClassTest = exam.getKind() == ExamKind.CLASS_TEST;
        if (isClassTest && exam.getStatus() != ExamStatus.DRAFT && exam.getStatus() != ExamStatus.SCHEDULED) {
            throw new IllegalStateException("Chỉ có thể sửa lịch bài kiểm tra trên lớp trước khi bắt đầu");
        }
        if (!isClassTest && !schedule.isModifiable()) {
            throw new IllegalStateException("Chỉ có thể sửa ca thi khi đang ở trạng thái nháp");
        }

        // Giá trị hiệu dụng: dùng giá trị mới nếu được cung cấp, ngược lại giữ giá trị hiện tại.
        UUID effectiveRoomId = input.schoolRoomId() != null ? input.schoolRoomId() : schedule.getSchoolRoomId();
        Instant effectiveStart = input.startDate() != null ? input.startDate() : schedule.getStartDate();
        Instant effectiveEnd = input.endDate() != null ? input.endDate() : schedule.getEndDate();

        if (!effectiveEnd.isAfter(effectiveStart)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (exam.isScheduleWindowShorterThanExamTime(effectiveStart, effectiveEnd)) {
            throw new IllegalArgumentException(ExamScheduleWindowMessages.tooShortForExamTime(exam));
        }
        // Chỉ ràng buộc với kỳ thi thường: ca thi phải nằm trong khung mở/đóng đã định của kỳ thi.
        // CLASS_TEST đi chiều ngược lại -- openAt/closeAt được ghi lại theo ca thi ở dưới, nên so
        // với khung cũ rồi chặn sẽ khiến không dời được lịch bài kiểm tra trên lớp.
        if (!isClassTest && exam.isScheduleWindowOutsideExamWindow(effectiveStart, effectiveEnd)) {
            throw new IllegalArgumentException(ExamScheduleWindowMessages.outsideExamWindow(exam));
        }

        if (input.schoolRoomId() != null) {
            examScheduleRoomValidator.requireRoomOfExamSchool(input.schoolRoomId(), exam);
        }

        examScheduleRoomValidator.requireNoOverlap(
            effectiveRoomId, effectiveStart, effectiveEnd, schedule.getId());

        // Dời giờ ca cũng phải soát giám thị, nếu không thì luật "không gác hai ca trùng giờ" bị
        // lách bằng cách gán lúc hai ca chưa đụng nhau rồi mới kéo chúng chồng lên.
        examScheduleProctorConflictValidator.requireProctorsFreeForNewWindow(
            schedule.getId(), effectiveStart, effectiveEnd);
        // Cùng lý do, đổi trục sang thí sinh: xếp học sinh lúc hai ca chưa đụng nhau rồi kéo giờ
        // cho chúng chồng lên là lối lách y hệt.
        examScheduleCandidateConflictValidator.requireCandidatesFreeForNewWindow(
            schedule.getId(), effectiveStart, effectiveEnd);

        // Với bài kiểm tra trên lớp, ca thi CHÍNH LÀ khung mở/đóng của bài (ghi ngược xuống exam ở
        // dưới), nên sửa ca thi là một đường ghi openAt/closeAt khác -- không soi ở đây thì đổi giờ
        // ca là lối vòng qua mặt ràng buộc hạn gói mà Create/UpdateExamUseCase đang áp.
        if (isClassTest) {
            subscriptionPeriodGuardService.requireWithinSubscriptionPeriod(
                exam.getSchoolId(), effectiveStart, effectiveEnd);
        }

        var now = Instant.now();
        if (isClassTest) {
            schedule.setSchoolRoomId(effectiveRoomId);
            schedule.setStartDate(effectiveStart);
            schedule.setEndDate(effectiveEnd);
            schedule.setUpdatedAt(now);
            schedule.setUpdatedBy(currentUserId);
            examScheduleRepository.save(schedule);
            exam.setOpenAt(effectiveStart);
            exam.setCloseAt(effectiveEnd);
            exam.setUpdatedAt(now);
            exam.setUpdatedBy(currentUserId);
            examRepository.save(exam);
        } else {
            int updated = examScheduleRepository.updateAtomic(
                schedule.getId(),
                input.schoolRoomId(),
                input.startDate(),
                input.endDate(),
                now,
                currentUserId);
            if (updated == 0) {
                throw new IllegalStateException("Không thể cập nhật ca thi (đã bị thay đổi trạng thái)");
            }
        }
        return schedule.getId();
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
