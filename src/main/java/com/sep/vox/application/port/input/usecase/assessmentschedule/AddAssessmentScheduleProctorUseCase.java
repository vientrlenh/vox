package com.sep.vox.application.port.input.usecase.assessmentschedule;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.AddAssessmentScheduleProctorCommand;
import com.sep.vox.application.port.input.service.AssessmentScheduleManageAccessService;
import com.sep.vox.application.port.input.service.AssessmentScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.AssessmentScheduleProctorDto;
import com.sep.vox.domain.mapper.AssessmentScheduleProctorDtoMapper;
import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.assessment.AssessmentEditingGuard;

@Service
@RequiredArgsConstructor
public class AddAssessmentScheduleProctorUseCase implements IUseCase<AddAssessmentScheduleProctorCommand, AssessmentScheduleProctorDto> {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private final AssessmentScheduleProctorConflictValidator assessmentScheduleProctorConflictValidator;
    private final AssessmentScheduleManageAccessService assessmentScheduleManageAccessService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional
    public AssessmentScheduleProctorDto execute(AddAssessmentScheduleProctorCommand input) {
        AssessmentSchedule schedule = assessmentScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getAssessmentId().equals(input.assessmentId())) {
            throw new ResourceNotFoundException("Không tìm thấy ca thi");
        }
        Assessment assessment = assessmentRepository.findById(schedule.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài kiểm tra"));
        assessmentScheduleManageAccessService.requireCanManage(assessment);
        AssessmentEditingGuard.requireScheduleEditable(assessment);

        // Giám thị phải là school_user vai trò TEACHER cùng trường với bài kiểm tra.
        if (!schoolUserRepository.existsBySchoolIdAndUserId(assessment.getSchoolId(), input.teacherId())) {
            throw new IllegalArgumentException("Giáo viên không thuộc trường của bài kiểm tra");
        }
        boolean isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(input.teacherId()).stream()
            .anyMatch(role -> SchoolRoleCodes.TEACHER.equals(role.roleCode()));
        if (!isTeacher) {
            throw new IllegalArgumentException("Người dùng không phải là giáo viên");
        }

        if (assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(schedule.getId(), input.teacherId())) {
            throw new ResourceDuplicatedException("Giám thị đã được phân công cho ca này");
        }

        // Loại chính ca này khỏi phép kiểm tra: kiểm tra trùng ngay bên trên đã lo trường hợp đó,
        // ở đây chỉ hỏi giáo viên có đang bận ở ca thi nào khác không (kể cả kỳ thi khác).
        assessmentScheduleProctorConflictValidator.requireTeacherFree(
            input.teacherId(), schedule.getStartDate(), schedule.getEndDate(), schedule.getId());

        AssessmentScheduleProctor proctor = AssessmentScheduleProctor.builder()
                .scheduleId(schedule.getId())
                .teacherId(input.teacherId())
                .build();
        return AssessmentScheduleProctorDtoMapper.toDto(assessmentScheduleProctorRepository.save(proctor));
    }
}
