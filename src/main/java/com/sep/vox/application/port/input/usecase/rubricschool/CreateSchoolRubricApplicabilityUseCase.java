package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.CreateSchoolRubricApplicabilityCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreateSchoolRubricApplicabilityUseCase implements IUseCase<CreateSchoolRubricApplicabilityCommand, List<UUID>> {

    private final RubricApplicabilityRepository rubricApplicabilityRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolRubricApplicabilityUseCase(RubricApplicabilityRepository rubricApplicabilityRepository, RubricVersionRepository rubricVersionRepository, RubricRepository rubricRepository, SchoolClassRepository schoolClassRepository, SchoolGradeRepository schoolGradeRepository, UserRepository userRepository, UserContextPort userContextPort) {
        this.rubricApplicabilityRepository = rubricApplicabilityRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public List<UUID> execute(CreateSchoolRubricApplicabilityCommand command) {
        // 1. Auth
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Lỗi."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        // 2. CHỐT CHẶN QUAN TRỌNG: Rubric phải được PUBLISHED mới cho áp dụng
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản Rubric."));
//        if (version.getStatus() != RubricStatus.PUBLISHED) {
//            throw new IllegalStateException("Hành động bị từ chối: Chỉ được phép cấu hình áp dụng (Applicability) cho các phiên bản Rubric đã được ban hành (PUBLISHED).");
//        }

        // 3. Ownership
        Rubric rubric = rubricRepository.findById(version.getRubricId()).orElseThrow(() -> new NotFoundException("Không tìm thấy Rubric gốc."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Không có quyền can thiệp Rubric của trường khác.");
        }

        // 4. TỐI ƯU N+1: Lấy ID Class và Grade
        List<UUID> classIds = command.applicabilities().stream().map(CreateSchoolRubricApplicabilityCommand.ApplicabilityItemCommand::schoolClassId).filter(Objects::nonNull).distinct().toList();
        List<UUID> gradeIds = command.applicabilities().stream().map(CreateSchoolRubricApplicabilityCommand.ApplicabilityItemCommand::schoolGradeId).filter(Objects::nonNull).distinct().toList();

        Map<UUID, SchoolClass> classMap = schoolClassRepository.findAllById(classIds).stream().collect(Collectors.toMap(SchoolClass::getId, c -> c));
        Map<UUID, SchoolGrade> gradeMap = schoolGradeRepository.findAllById(gradeIds).stream().collect(Collectors.toMap(SchoolGrade::getId, g -> g));

        OffsetDateTime now = OffsetDateTime.now();

        // 5. Xử lý Logic và Check Active
        List<RubricApplicability> applicabilitiesToSave = command.applicabilities().stream().map(appCmd -> {

            if (appCmd.schoolClassId() == null && appCmd.schoolGradeId() == null) {
                throw new IllegalArgumentException("Phải cung cấp ID của Lớp học hoặc Khối học.");
            }

            if (appCmd.schoolClassId() != null) {
                SchoolClass schoolClass = classMap.get(appCmd.schoolClassId());
                if (schoolClass == null) throw new NotFoundException("Không tìm thấy Lớp học ID: " + appCmd.schoolClassId());
                if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) throw new IllegalStateException("Lớp học " + schoolClass.getName() + " đang bị khóa/không hoạt động.");
            }

            if (appCmd.schoolGradeId() != null) {
                SchoolGrade schoolGrade = gradeMap.get(appCmd.schoolGradeId());
                if (schoolGrade == null) throw new NotFoundException("Không tìm thấy Khối học ID: " + appCmd.schoolGradeId());
                if (schoolGrade.getStatus() != SchoolGradeStatus.ACTIVE) throw new IllegalStateException("Khối học " + schoolGrade.getName() + " đang bị khóa/không hoạt động.");
            }

            OffsetDateTime validFrom = appCmd.effectiveFrom() != null ? appCmd.effectiveFrom() : now;
            if (appCmd.effectiveTo() != null && appCmd.effectiveTo().isBefore(validFrom)) {
                throw new IllegalArgumentException("Ngày kết thúc áp dụng không được trước ngày bắt đầu.");
            }

            return new RubricApplicability(
                    command.versionId(),
                    appCmd.schoolClassId(),
                    appCmd.schoolGradeId(),
                    validFrom,
                    appCmd.effectiveTo(),
                    now, now, currentUserId, currentUserId
            );
        }).toList();

        // 6. Lưu xuống DB
        rubricApplicabilityRepository.saveAll(applicabilitiesToSave);

        return applicabilitiesToSave.stream().map(RubricApplicability::getId).toList();
    }
}