package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.SchoolGradeResponse.SchoolGradeResponse;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DeleteSchoolGradeUseCase implements IUseCase<DeleteSchoolGradeCommand, SchoolGradeResponse> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository; // Thay Role Repo bằng User Repo

    public DeleteSchoolGradeUseCase(
            SchoolGradeRepository schoolGradeRepository,
            SchoolClassRepository schoolClassRepository,
            UserContextPort userContextPort,
            UserRepository userRepository
    ) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public SchoolGradeResponse execute(DeleteSchoolGradeCommand command) {
        // 1. Lock dữ liệu an toàn
        //command.id = grade.id
        SchoolGrade grade = schoolGradeRepository.findByIdForDelete(command.id(), command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khối lớp này."));

        // 2. Validate User & Bảo mật
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản."));

        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(grade.getSchoolId())) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên khối lớp của trường khác.");
        }

        // 3. Logic chặn xóa
        if (grade.getStatus() == SchoolGradeStatus.ARCHIVED) {
            throw new IllegalStateException("Khối lớp này đã được lưu trữ (xóa mềm) từ trước.");
        }

        boolean isUsed = schoolClassRepository.existsBySchoolGradeId(grade.getId());
        if (isUsed) {
            throw new IllegalStateException("Không thể xóa vì khối lớp đang có lớp học sử dụng.");
        }

        // 4. XỬ LÝ NHÁNH DELETE
        if (grade.getStatus() == SchoolGradeStatus.ACTIVE) {
            grade.setStatus(SchoolGradeStatus.ARCHIVED);
            grade.setUpdatedAt(OffsetDateTime.now());
            grade.setUpdatedBy(currentUserId);
            schoolGradeRepository.save(grade);
        } else if (grade.getStatus() == SchoolGradeStatus.INACTIVE) {
            schoolGradeRepository.deleteByIdAndSchoolId(grade.getId(), grade.getSchoolId());
        }

        // 5. Nhả Response
        return toResponse(grade);
    }

    private SchoolGradeResponse toResponse(SchoolGrade grade) {
        return new SchoolGradeResponse(
                grade.getId(),
                grade.getSchoolId(),
                grade.getCode(),
                grade.getName(),
                grade.getDescription(),
                grade.getStartDate(),
                grade.getEndDate(),
                grade.getStatus(),
                grade.getCreatedAt(),
                grade.getUpdatedAt(),
                grade.getCreatedBy(),
                grade.getUpdatedBy()
        );
    }
}