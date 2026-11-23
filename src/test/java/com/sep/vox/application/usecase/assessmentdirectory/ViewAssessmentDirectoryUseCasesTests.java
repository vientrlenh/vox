package com.sep.vox.application.usecase.assessmentdirectory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.assessmentdirectory.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService.AssessmentDirectoryScope;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryClassesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryGradesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryProctorsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentdirectory.ViewAssessmentDirectoryStudentsUseCase;
import com.sep.vox.application.query.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.application.query.repository.AssessmentDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.SchoolClassRepository;

/**
 * Phạm vi danh bạ kỳ thi theo `schoolWide`.
 *
 * <p>Điểm phải giữ: nhánh CLASS_TEST không bao giờ được rơi về truy vấn toàn trường —
 * đó chính là chỗ nới quyền nếu ai đó sửa nhầm.
 */
class ViewAssessmentDirectoryUseCasesTests {

    private AssessmentDirectoryAccessService accessService;
    private AssessmentDirectoryQueryRepository queryRepository;
    private SchoolClassRepository schoolClassRepository;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final ViewAssessmentDirectoryQuery query = new ViewAssessmentDirectoryQuery(assessmentId, "  an   nguyen ", 2, 20, List.of());

    @BeforeEach
    void setUp() {
        accessService = mock(AssessmentDirectoryAccessService.class);
        queryRepository = mock(AssessmentDirectoryQueryRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
    }

    // ---------- lớp ----------

    @Test
    void should_list_all_school_classes_when_scope_is_school_wide() {
        givenScope(true);
        ViewAssessmentDirectoryClassesUseCase useCase = new ViewAssessmentDirectoryClassesUseCase(accessService, schoolClassRepository);
        when(schoolClassRepository.findBySchoolId(
                any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(schoolClass()), 2, 20, 1, 1));

        PageResult<SchoolClassDto> result = useCase.execute(query);

        assertThat(result.content()).hasSize(1);
        verify(schoolClassRepository)
            .findBySchoolId(schoolId, "an nguyen", SchoolClassStatus.ACTIVE, null, null, 2, 20);
        verify(schoolClassRepository, never())
            .findByUserId(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void should_list_only_caller_classes_when_scope_is_class_test() {
        givenScope(false);
        ViewAssessmentDirectoryClassesUseCase useCase = new ViewAssessmentDirectoryClassesUseCase(accessService, schoolClassRepository);
        when(schoolClassRepository.findByUserId(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));

        useCase.execute(query);

        verify(schoolClassRepository)
            .findByUserId(schoolId, callerId, "an nguyen", SchoolClassStatus.ACTIVE, 2, 20);
        verify(schoolClassRepository, never())
            .findBySchoolId(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    // ---------- niên khóa ----------

    @Test
    void should_list_grades_when_scope_is_school_wide() {
        givenScope(true);
        ViewAssessmentDirectoryGradesUseCase useCase = new ViewAssessmentDirectoryGradesUseCase(accessService, queryRepository);
        when(queryRepository.findGradesBySchoolId(any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));

        useCase.execute(query);

        verify(queryRepository).findGradesBySchoolId(schoolId, "an nguyen", 2, 20);
    }

    @Test
    void should_reject_grades_for_class_test_chair() {
        givenScope(false);
        ViewAssessmentDirectoryGradesUseCase useCase = new ViewAssessmentDirectoryGradesUseCase(accessService, queryRepository);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> useCase.execute(query));

        assertThat(exception.getMessage())
            .isEqualTo("Bài kiểm tra trên lớp không hỗ trợ nhập thí sinh theo niên khóa");
        verify(queryRepository, never()).findGradesBySchoolId(any(), any(), anyInt(), anyInt());
    }

    // ---------- học sinh ----------

    @Test
    void should_list_all_school_students_when_scope_is_school_wide() {
        givenScope(true);
        ViewAssessmentDirectoryStudentsUseCase useCase = new ViewAssessmentDirectoryStudentsUseCase(accessService, queryRepository);
        when(queryRepository.findUsersBySchoolId(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(user()), 2, 20, 1, 1));

        PageResult<AssessmentDirectoryUserInfo> result = useCase.execute(query);

        assertThat(result.content()).hasSize(1);
        verify(queryRepository)
            .findUsersBySchoolId(schoolId, SchoolRoleCodes.STUDENT, "an nguyen", List.of(), 2, 20);
        verify(queryRepository, never()).findUsersByClassIds(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void should_list_only_students_of_caller_classes_when_scope_is_class_test() {
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = givenScope(false);
        UUID classId = UUID.randomUUID();
        when(accessService.callerClassIds(scope)).thenReturn(List.of(classId));
        ViewAssessmentDirectoryStudentsUseCase useCase = new ViewAssessmentDirectoryStudentsUseCase(accessService, queryRepository);
        when(queryRepository.findUsersByClassIds(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));

        useCase.execute(query);

        verify(queryRepository)
            .findUsersByClassIds(List.of(classId), SchoolRoleCodes.STUDENT, "an nguyen", List.of(), 2, 20);
        verify(queryRepository, never()).findUsersBySchoolId(any(), any(), any(), any(), anyInt(), anyInt());
    }

    // ---------- giám thị ----------

    @Test
    void should_always_list_school_teachers_for_proctors() {
        // No branching by kind: a proctor is always a school-wide teacher.
        givenScope(false);
        ViewAssessmentDirectoryProctorsUseCase useCase = new ViewAssessmentDirectoryProctorsUseCase(accessService, queryRepository);
        when(queryRepository.findUsersBySchoolId(any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));

        useCase.execute(query);

        verify(queryRepository)
            .findUsersBySchoolId(schoolId, SchoolRoleCodes.TEACHER, "an nguyen", List.of(), 2, 20);
    }

    private AssessmentDirectoryScope givenScope(boolean schoolWide) {
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = new AssessmentDirectoryScope(callerId, schoolId, schoolWide);
        when(accessService.resolveByAssessmentId(eq(assessmentId))).thenReturn(scope);
        return scope;
    }

    private SchoolClass schoolClass() {
        SchoolClass schoolClass = SchoolClass.create(
            schoolId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-01",
            "English 01",
            null);
        schoolClass.setId(UUID.randomUUID());
        return schoolClass;
    }

    private AssessmentDirectoryUserInfo user() {
        return new AssessmentDirectoryUserInfo(UUID.randomUUID(), "An Nguyen", "an@example.com", "ACTIVE");
    }
}
