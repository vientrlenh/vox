package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.ArchiveSchoolRubricVersionCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArchiveSchoolRubricVersionUseCase implements IUseCase<ArchiveSchoolRubricVersionCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;

    @Override
    @Transactional
    public UUID execute(ArchiveSchoolRubricVersionCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa.");
        }

        RubricVersion version = loadVersionOwnedBySchool(command, currentUserId);
        requireActiveSchool(command.schoolId());
        requireArchivable(version);

        Instant now = Instant.now();
        version.setStatus(RubricStatus.ARCHIVED);
        // effectiveTo must not move before effectiveFrom when the version has not become effective yet.
        version.setEffectiveTo(now.isBefore(version.getEffectiveFrom()) ? version.getEffectiveFrom() : now);

        RubricVersion saved = rubricVersionRepository.save(version);
        return saved.getId();
    }

    private RubricVersion loadVersionOwnedBySchool(ArchiveSchoolRubricVersionCommand command, UUID currentUserId) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản Rubric này."));

        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ Rubric gốc."));

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("BẢO MẬT: Bạn không có quyền can thiệp vào phiên bản của trường khác.");
        }
        return version;
    }

    private void requireActiveSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trường học."));
        if (!school.isActive()) {
            throw new ForbiddenException("Hành động bị từ chối: Trường học này đang bị vô hiệu hóa trên hệ thống.");
        }
    }

    private void requireArchivable(RubricVersion version) {
        if (version.getStatus() != RubricStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ có thể lưu trữ (ARCHIVE) phiên bản đang ở trạng thái PUBLISHED.");
        }

        // Block archiving while an Assessment Policy in DRAFT/PUBLISHED still uses this version: archiving
        // unconditionally would leave an effective Policy pointing at an ARCHIVED version. Mirrors
        // existsOtherActiveByRubricVersionId on the policy-archive side (ArchiveSchoolAssessmentPolicyUseCase).
        if (assessmentPolicyRepository.existsActiveByRubricVersionId(version.getId())) {
            throw new IllegalArgumentException(
                    "Không thể lưu trữ phiên bản này vì còn Assessment Policy đang DRAFT/PUBLISHED sử dụng. "
                            + "Hãy lưu trữ các Assessment Policy đó trước.");
        }
    }
}