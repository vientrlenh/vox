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
    @Transactional // Bắt buộc để Lock và Delete hoạt động
    public SchoolGradeResponse execute(DeleteSchoolGradeCommand command) {

        // 1. Lock dữ liệu để an toàn khi cập nhật
        SchoolGrade grade = schoolGradeRepository.findByIdForDelete(command.id())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khối lớp/năm học."));

        // 2. Validate User và Quyền sở hữu trường học (Controller đã lo check Role)
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản."));

        if (currentUser.getSchoolId() != null && !currentUser.getSchoolId().equals(grade.getSchoolId())) {
            throw new ForbiddenException("Bạn không có quyền thực hiện hành động này trên khối lớp của trường khác.");
        }

        // 3. Logic chặn xóa
        if (grade.getStatus() == SchoolGradeStatus.ARCHIVED) {
            throw new IllegalStateException("Khối lớp/năm học đã được lưu trữ từ trước.");
        }

        boolean isUsed = schoolClassRepository.existsBySchoolGradeId(grade.getId());
        if (isUsed) {
            throw new IllegalStateException("Không thể xóa vì khối lớp/năm học đang có lớp học sử dụng.");
        }

        // 4. XỬ LÝ NHÁNH DELETE (Soft vs Hard)
        if (grade.getStatus() == SchoolGradeStatus.ACTIVE) {
            // Đang ACTIVE -> Xóa mềm (Chuyển thành ARCHIVED)
            grade.setStatus(SchoolGradeStatus.ARCHIVED);
            grade.setUpdatedAt(OffsetDateTime.now());
            grade.setUpdatedBy(currentUserId);

            // Hàm save() sẽ giúp Hibernate tự update lại xuống DB
            schoolGradeRepository.save(grade);

        } else if (grade.getStatus() == SchoolGradeStatus.INACTIVE) {
            // Đang INACTIVE -> Xóa cứng bay màu khỏi DB
            schoolGradeRepository.deleteById(grade.getId());
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