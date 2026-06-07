package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UpdateSchoolGradeUseCase implements IUseCase<UpdateSchoolGradeCommand, UUID> {
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserContextPort userContextPort;

    public UpdateSchoolGradeUseCase(SchoolGradeRepository schoolGradeRepository, SchoolRepository schoolRepository, UserContextPort userContextPort) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSchoolGradeCommand command) {
        // Lock + check ownership
        SchoolGrade grade = schoolGradeRepository.findByIdForUpdate(command.schoolGradeId(), command.schoolId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy năm học/khối lớp."));


        // 2. Validate nghiệp vụ ngày tháng
        if (command.startDate() != null && command.endDate() != null) {
            if (!command.startDate().isBefore(command.endDate())) {
                throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
            }
        }

        // 3. Update từng phần (Partial Update)
        if (command.name() != null) grade.setName(StringNormalization.trimAndCollapseSpaces(command.name()));
        if (command.description() != null)
            grade.setDescription(StringNormalization.trimAndCollapseSpaces(command.description()));
        if (command.startDate() != null) grade.setStartDate(command.startDate());
        if (command.endDate() != null) grade.setEndDate(command.endDate());

        grade.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        grade.setUpdatedAt(OffsetDateTime.now());

        // 4. Hibernate tự động phát hiện thay đổi (Dirty Checking) và update
        // Bạn không cần bắn SQL thủ công, cách này an toàn nhất cho partial update
        schoolGradeRepository.save(grade);

        return grade.getId();
    }
}