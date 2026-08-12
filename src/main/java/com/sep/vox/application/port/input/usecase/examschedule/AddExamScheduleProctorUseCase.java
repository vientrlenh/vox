package com.sep.vox.application.port.input.usecase.examschedule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AddExamScheduleProctorCommand;
import com.sep.vox.application.port.input.service.ExamScheduleManageAccessService;
import com.sep.vox.application.port.input.service.ExamScheduleProctorConflictValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.ExamScheduleProctorDto;
import com.sep.vox.domain.mapper.ExamScheduleProctorDtoMapper;
import com.sep.vox.domain.model.exam.ExamScheduleProctor;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleProctorRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.exam.ExamEditingGuard;

@Service
public class AddExamScheduleProctorUseCase implements IUseCase<AddExamScheduleProctorCommand, ExamScheduleProctorDto> {

    private final ExamRepository examRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamScheduleProctorRepository examScheduleProctorRepository;
    private final ExamScheduleProctorConflictValidator examScheduleProctorConflictValidator;
    private final ExamScheduleManageAccessService examScheduleManageAccessService;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public AddExamScheduleProctorUseCase(
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamScheduleProctorRepository examScheduleProctorRepository,
            ExamScheduleProctorConflictValidator examScheduleProctorConflictValidator,
            ExamScheduleManageAccessService examScheduleManageAccessService,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.examRepository = examRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examScheduleProctorRepository = examScheduleProctorRepository;
        this.examScheduleProctorConflictValidator = examScheduleProctorConflictValidator;
        this.examScheduleManageAccessService = examScheduleManageAccessService;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @Override
    @Transactional
    public ExamScheduleProctorDto execute(AddExamScheduleProctorCommand input) {
        var schedule = examScheduleRepository.findById(input.scheduleId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ca thi"));
        if (!schedule.getExamId().equals(input.examId())) {
            throw new NotFoundException("Không tìm thấy ca thi");
        }
        var exam = examRepository.findById(schedule.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bài kiểm tra"));
        examScheduleManageAccessService.requireCanManage(exam);
        ExamEditingGuard.requireScheduleEditable(exam);

        // Giám thị phải là school_user vai trò TEACHER cùng trường với bài kiểm tra.
        if (!schoolUserRepository.existsBySchoolIdAndUserId(exam.getSchoolId(), input.teacherId())) {
            throw new IllegalArgumentException("Giáo viên không thuộc trường của bài kiểm tra");
        }
        var isTeacher = userRoleQueryRepository.findByUserIdWithRoleInfo(input.teacherId()).stream()
            .anyMatch(role -> SchoolRoleCodes.TEACHER.equals(role.roleCode()));
        if (!isTeacher) {
            throw new IllegalArgumentException("Người dùng không phải là giáo viên");
        }

        if (examScheduleProctorRepository.existsByScheduleIdAndTeacherId(schedule.getId(), input.teacherId())) {
            throw new DuplicatedException("Giám thị đã được phân công cho ca này");
        }

        // Loại chính ca này khỏi phép kiểm tra: kiểm tra trùng ngay bên trên đã lo trường hợp đó,
        // ở đây chỉ hỏi giáo viên có đang bận ở ca thi nào khác không (kể cả kỳ thi khác).
        examScheduleProctorConflictValidator.requireTeacherFree(
            input.teacherId(), schedule.getStartDate(), schedule.getEndDate(), schedule.getId());

        var proctor = new ExamScheduleProctor(schedule.getId(), input.teacherId());
        return ExamScheduleProctorDtoMapper.toDto(examScheduleProctorRepository.save(proctor));
    }
}
