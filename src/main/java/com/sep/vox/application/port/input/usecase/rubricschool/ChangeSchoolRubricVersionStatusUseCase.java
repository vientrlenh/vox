package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.ChangeSchoolRubricVersionStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.rubric.*;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.service.rubric.RubricScoringConsistencyValidator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangeSchoolRubricVersionStatusUseCase implements IUseCase<ChangeSchoolRubricVersionStatusCommand, UUID> {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final FrameworkRepository frameworkRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public UUID execute(ChangeSchoolRubricVersionStatusCommand command) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("Lỗi tài khoản."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("Tài khoản bị khóa.");

        RubricVersion version = loadVersionToChange(command);
        Rubric rubric = loadRubricOwnedBySchool(version, command, currentUserId);
        requireTransitionAllowed(command.status(), version, rubric);

        version.setStatus(command.status());

        rubricVersionRepository.save(version);
        return version.getId();
    }

    private RubricVersion loadVersionToChange(ChangeSchoolRubricVersionStatusCommand command) {
        RubricVersion version = rubricVersionRepository.findById(command.versionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản Rubric."));

        if (version.getStatus() == command.status()) {
            throw new IllegalArgumentException("Phiên bản đã đang ở trạng thái " + command.status() + ".");
        }
        return version;
    }

    private Rubric loadRubricOwnedBySchool(RubricVersion version, ChangeSchoolRubricVersionStatusCommand command, UUID currentUserId) {
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rubric gốc."));

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản của bạn không được liên kết với bất kỳ trường học nào."));

        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL ||
                !rubric.getSchoolId().equals(command.schoolId()) ||
                !schoolUser.getSchoolId().equals(rubric.getSchoolId())) {
            throw new ForbiddenException("Hành động bị từ chối: Không thể chuyển trạng thái Rubric của trường khác.");
        }
        return rubric;
    }

    private void requireDraftWithCriteria(RubricVersion version) {
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể ban hành (PUBLISH) phiên bản đang ở trạng thái Nháp (DRAFT).");
        }

        // A version without criteria or a scoring scale cannot grade anything: once published it would be
        // useless and could no longer be edited (only archived).
        if (rubricCriterionRepository.findByRubricVersionId(version.getId()).isEmpty()) {
            throw new IllegalArgumentException("Không thể ban hành phiên bản này vì chưa có tiêu chí (Criterion) nào.");
        }
    }

    private void requireActiveFramework(Rubric rubric) {
        Framework framework = frameworkRepository.findById(rubric.getFrameworkId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Khung tiêu chuẩn (Framework) liên kết."));
        if (!framework.isActive()) {
            throw new IllegalArgumentException("Không thể ban hành Rubric này vì Khung tiêu chuẩn (Framework) gốc đang bị vô hiệu hóa.");
        }
    }

    private void requireTransitionAllowed(RubricStatus targetStatus, RubricVersion version, Rubric rubric) {
        if (targetStatus == RubricStatus.PUBLISHED) {
            requirePublishable(version, rubric);
        } else if (targetStatus == RubricStatus.ARCHIVED) {
            throw new IllegalArgumentException("Hành động bị từ chối: Vui lòng dùng chức năng Lưu trữ (Archive) riêng để chuyển phiên bản sang ARCHIVED.");
        } else if (targetStatus == RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Hành động bị từ chối: Không thể chuyển một phiên bản đã Ban hành/Lưu trữ quay ngược lại trạng thái Nháp (DRAFT). Vui lòng tạo phiên bản mới.");
        }
    }

    private void requirePublishable(RubricVersion version, Rubric rubric) {
        requireDraftWithCriteria(version);
        requireActiveFramework(rubric);

        if (!assessmentPolicyRepository.existsPublishedByRubricVersionId(version.getId())) {
            throw new IllegalArgumentException("Không thể ban hành Rubric này vì chưa có Assessment Policy nào liên kết đang ở trạng thái PUBLISHED.");
        }

        RubricScoringConsistencyValidator.assertPublishable(
                version.getTotalScoreMethod(),
                version.getScoringScaleMin(),
                version.getScoringScaleMax(),
                rubricCriterionRepository.findByRubricVersionId(version.getId()));
    }
}
