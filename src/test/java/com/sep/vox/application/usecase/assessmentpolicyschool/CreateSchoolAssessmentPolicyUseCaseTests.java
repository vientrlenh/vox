package com.sep.vox.application.usecase.assessmentpolicyschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.service.GradeLevelBandScopeGuardService;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CreateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.GradeLevelBandScopeRepository;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * 1 Rubric Version được phép gắn với nhiều Assessment Policy, kể cả khi 2 Policy nằm ở 2 lớp
 * (schoolClassId) khác nhau trong cùng trường.
 * Ranh giới sau V44: nhiều Assessment Policy ở các PHẠM VI khác nhau được dùng chung một Rubric
 * Version, nhưng mỗi phạm vi vẫn chỉ được đúng một chính sách còn hiệu lực.
 */
class CreateSchoolAssessmentPolicyUseCaseTests {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_ID = UUID.randomUUID();
    private static final UUID FRAMEWORK_VERSION_ID = UUID.randomUUID();
    private static final UUID RUBRIC_ID = UUID.randomUUID();
    private static final UUID RUBRIC_VERSION_ID = UUID.randomUUID();
    private static final UUID TARGET_BAND_ID = UUID.randomUUID();
    private static final UUID LANGUAGE_ID = UUID.randomUUID();

    private AssessmentPolicyRepository assessmentPolicyRepository;
    private SchoolClassRepository schoolClassRepository;
    // Trạng thái phiên bản Rubric là thứ các test dưới đây đổi qua lại, nên mock phải là field.
    private RubricVersion rubricVersion;
    private CreateSchoolAssessmentPolicyUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentPolicyRepository = mock(AssessmentPolicyRepository.class);
        var frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        var frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        var rubricVersionRepository = mock(RubricVersionRepository.class);
        var rubricRepository = mock(RubricRepository.class);
        var languageRepository = mock(SupportedLanguageRepository.class);
        var gradeLevelRepository = mock(GradeLevelRepository.class);
        var schoolGradeRepository = mock(SchoolGradeRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
        var schoolUserRepository = mock(SchoolUserRepository.class);
        var userRepository = mock(UserRepository.class);
        var userContextPort = mock(UserContextPort.class);

        // Guard thật, repository giả: chưa stub dòng trần nào nên
        // findByGradeLevelIdAndFrameworkVersionId trả Optional.empty() -> guard không chặn gì.
        // Đúng cái các test dưới đây cần, vì chúng kiểm ranh giới phạm vi chứ không phải trần
        // bậc. Trần bậc có bộ test riêng ở GradeLevelBandScopeGuardServiceTests.
        var gradeLevelBandScopeGuardService = new GradeLevelBandScopeGuardService(
                mock(GradeLevelBandScopeRepository.class), frameworkResultBandRepository,
                gradeLevelRepository, schoolGradeRepository, schoolClassRepository);

        useCase = new CreateSchoolAssessmentPolicyUseCase(
                assessmentPolicyRepository, frameworkVersionRepository, frameworkResultBandRepository,
                rubricVersionRepository, rubricRepository, languageRepository, gradeLevelRepository,
                schoolGradeRepository, schoolClassRepository, schoolUserRepository, userRepository, userContextPort,
                gradeLevelBandScopeGuardService);

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(ADMIN_ID);
        User currentUser = mock(User.class);
        when(currentUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(currentUser));

        SchoolUser schoolUser = mock(SchoolUser.class);
        when(schoolUser.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolUserRepository.findByUserId(ADMIN_ID)).thenReturn(Optional.of(schoolUser));

        FrameworkVersion frameworkVersion = mock(FrameworkVersion.class);
        when(frameworkVersion.getStatus()).thenReturn(FrameworkVersionStatus.PUBLISHED);
        when(frameworkVersion.getFrameworkId()).thenReturn(FRAMEWORK_ID);
        when(frameworkVersionRepository.findById(FRAMEWORK_VERSION_ID)).thenReturn(Optional.of(frameworkVersion));

        when(languageRepository.existsById(any())).thenReturn(true);

        FrameworkResultBand targetBand = mock(FrameworkResultBand.class);
        when(targetBand.getFrameworkVersionId()).thenReturn(FRAMEWORK_VERSION_ID);
        when(frameworkResultBandRepository.findById(TARGET_BAND_ID)).thenReturn(Optional.of(targetBand));

        rubricVersion = mock(RubricVersion.class);
        when(rubricVersion.getStatus()).thenReturn(RubricStatus.DRAFT);
        when(rubricVersion.getRubricId()).thenReturn(RUBRIC_ID);
        when(rubricVersionRepository.findById(RUBRIC_VERSION_ID)).thenReturn(Optional.of(rubricVersion));

        Rubric rubric = mock(Rubric.class);
        when(rubric.getOwnerType()).thenReturn(RubricOwnerType.SCHOOL);
        when(rubric.getSchoolId()).thenReturn(SCHOOL_ID);
        when(rubric.getFrameworkId()).thenReturn(FRAMEWORK_ID);
        when(rubricRepository.findById(RUBRIC_ID)).thenReturn(Optional.of(rubric));

        when(assessmentPolicyRepository.existsActiveForScopeAnyRubricVersion(any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(assessmentPolicyRepository.findMaxVersionForScope(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(assessmentPolicyRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateAssessmentPolicyCommand command(UUID schoolClassId) {
        return new CreateAssessmentPolicyCommand(
                SCHOOL_ID, FRAMEWORK_VERSION_ID, RUBRIC_VERSION_ID, LANGUAGE_ID,
                null, null, schoolClassId, TARGET_BAND_ID, null, null,
                Instant.now(), null);
    }

    private void stubSchoolClass(UUID classId) {
        SchoolClass schoolClass = mock(SchoolClass.class);
        when(schoolClass.getSchoolId()).thenReturn(SCHOOL_ID);
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
    }

    @Test
    void allows_whenSameBatchReusesRubricVersionAcrossDifferentClasses() {
        // V44: lớp chuyên và lớp thường cần chính sách riêng theo phạm vi Lớp nhưng chấm bằng cùng
        // một bộ tiêu chí. Trước V44 mỗi chính sách buộc phải có phiên bản rubric riêng, nên trường
        // phải nhân bản y hệt bộ tiêu chí cho từng lớp.
        UUID classId1 = UUID.randomUUID();
        UUID classId2 = UUID.randomUUID();
        stubSchoolClass(classId1);
        stubSchoolClass(classId2);

        List<CreateAssessmentPolicyCommand> commands = List.of(
                command(classId1),
                command(classId2) // lớp khác -> scope khác, cùng rubricVersionId
        );

        assertThat(useCase.execute(commands)).hasSize(2);
    }

    @Test
    void stillRejects_whenTwoPoliciesInBatchShareTheSameScope() {
        // Ràng buộc còn hiệu lực sau V44: mỗi phạm vi chỉ một chính sách. Lúc chấm bài chỉ MỘT
        // chính sách được chọn cho một phạm vi, nên bản thứ hai sẽ là dữ liệu chết.
        UUID classId = UUID.randomUUID();
        stubSchoolClass(classId);

        assertThatThrownBy(() -> useCase.execute(List.of(command(classId), command(classId))))
                .isInstanceOf(DuplicatedException.class)
                .hasMessageContaining("trùng phạm vi áp dụng");
    }

    @Test
    void allowsAttachingToAnAlreadyPublishedRubricVersion() {
        // Nới 2026-08-23. Luật cũ chặn PUBLISHED, tức là trường chỉ có đúng một cửa sổ -- trước lúc
        // ban hành phiên bản -- để khai hết mọi lớp sẽ dùng bộ tiêu chí này. Mà ban hành lại là điều
        // kiện để dùng được cho kỳ thi, nên thêm một lớp sau đó buộc phải sao lại cả Rubric.
        when(rubricVersion.getStatus()).thenReturn(RubricStatus.PUBLISHED);
        UUID classId = UUID.randomUUID();
        stubSchoolClass(classId);

        assertThat(useCase.execute(List.of(command(classId)))).hasSize(1);
    }

    @Test
    void rejects_whenRubricVersionIsArchived() {
        // Chiều ngược lại của cùng một luật: phiên bản đã thu hồi thì không được gắn thêm chính sách,
        // nếu không là chấm học sinh bằng thang trường đã bỏ. Luật cũ dùng `== PUBLISHED` nên nhánh
        // ARCHIVED lọt qua -- chặn nhầm bản đang dùng, cho qua bản đã bỏ.
        when(rubricVersion.getStatus()).thenReturn(RubricStatus.ARCHIVED);
        UUID classId = UUID.randomUUID();
        stubSchoolClass(classId);

        assertThatThrownBy(() -> useCase.execute(List.of(command(classId))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ARCHIVED");
    }
}
