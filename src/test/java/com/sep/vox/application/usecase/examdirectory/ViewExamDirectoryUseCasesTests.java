package com.sep.vox.application.usecase.examdirectory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewExamDirectoryQuery;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService.ExamDirectoryScope;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryClassesUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryGradesUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryProctorsUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryStudentsUseCase;
import com.sep.vox.application.query.dto.ExamDirectoryUserInfo;
import com.sep.vox.application.query.repository.ExamDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;
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
class ViewExamDirectoryUseCasesTests {

    private ExamDirectoryAccessService accessService;
    private ExamDirectoryQueryRepository queryRepository;
    private SchoolClassRepository schoolClassRepository;

    private final UUID examId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final ViewExamDirectoryQuery query = new ViewExamDirectoryQuery(examId, "  an   nguyen ", 2, 20);

    @BeforeEach
    void setUp() {
        accessService = mock(ExamDirectoryAccessService.class);
        queryRepository = mock(ExamDirectoryQueryRepository.class);
        schoolClassRepository = mock(SchoolClassRepository.class);
    }

    // ---------- lớp ----------

    @Test
    void should_list_all_school_classes_when_scope_is_school_wide() {
        givenScope(true);
        var useCase = new ViewExamDirectoryClassesUseCase(accessService, schoolClassRepository);
        when(schoolClassRepository.findBySchoolId(
                any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(schoolClass()), 2, 20, 1, 1));

        var result = useCase.execute(query);

        assertThat(result.content()).hasSize(1);
        verify(schoolClassRepository)
            .findBySchoolId(schoolId, "an nguyen", SchoolClassStatus.ACTIVE, null, null, 2, 20);
        verify(schoolClassRepository, never())
            .findByUserId(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void should_list_only_caller_classes_when_scope_is_class_test() {
        givenScope(false);
        var useCase = new ViewExamDirectoryClassesUseCase(accessService, schoolClassRepository);
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
        var useCase = new ViewExamDirectoryGradesUseCase(accessService, queryRepository);
        when(queryRepository.findGradesBySchoolId(any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));

        useCase.execute(query);

        verify(queryRepository).findGradesBySchoolId(schoolId, "an nguyen", 2, 20);
    }

    @Test
    void should_reject_grades_for_class_test_chair() {
        givenScope(false);
        var useCase = new ViewExamDirectoryGradesUseCase(accessService, queryRepository);

        var exception = assertThrows(ForbiddenException.class, () -> useCase.execute(query));

        assertThat(exception.getMessage())
            .isEqualTo("Bài kiểm tra trên lớp không hỗ trợ nhập thí sinh theo niên khóa");
        verify(queryRepository, never()).findGradesBySchoolId(any(), any(), anyInt(), anyInt());
    }

    // ---------- học sinh ----------

    @Test
    void should_list_all_school_students_when_scope_is_school_wide() {
        givenScope(true);
        var useCase = new ViewExamDirectoryStudentsUseCase(accessService, queryRepository);
        when(queryRepository.findUsersBySchoolId(any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(user()), 2, 20, 1, 1));

        var result = useCase.execute(query);

        assertThat(result.content()).hasSize(1);
        verify(queryRepository)
            .findUsersBySchoolId(schoolId, SchoolRoleCodes.STUDENT, "an nguyen", 2, 20);
        verify(queryRepository, never()).findUsersByClassIds(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void should_list_only_students_of_caller_classes_when_scope_is_class_test() {
        var scope = givenScope(false);
        var classId = UUID.randomUUID();
        when(accessService.callerClassIds(scope)).thenReturn(List.of(classId));
        var useCase = new ViewExamDirectoryStudentsUseCase(accessService, queryRepository);
        when(queryRepository.findUsersByClassIds(any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));

        useCase.execute(query);

        verify(queryRepository)
            .findUsersByClassIds(List.of(classId), SchoolRoleCodes.STUDENT, "an nguyen", 2, 20);
        verify(queryRepository, never()).findUsersBySchoolId(any(), any(), any(), anyInt(), anyInt());
    }

    // ---------- giám thị ----------

    @Test
    void should_always_list_school_teachers_for_proctors() {
        // Không phân nhánh theo kind: giám thị luôn là giáo viên toàn trường.
        givenScope(false);
        var useCase = new ViewExamDirectoryProctorsUseCase(accessService, queryRepository);
        when(queryRepository.findUsersBySchoolId(any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 2, 20, 0, 0));

        useCase.execute(query);

        verify(queryRepository)
            .findUsersBySchoolId(schoolId, SchoolRoleCodes.TEACHER, "an nguyen", 2, 20);
    }

    private ExamDirectoryScope givenScope(boolean schoolWide) {
        var scope = new ExamDirectoryScope(callerId, schoolId, schoolWide);
        when(accessService.resolveByExamId(eq(examId))).thenReturn(scope);
        return scope;
    }

    private SchoolClass schoolClass() {
        var schoolClass = SchoolClass.create(
            schoolId, UUID.randomUUID(), UUID.randomUUID(), "ENG-01", "English 01", null,
            UUID.randomUUID(), OffsetDateTime.now());
        schoolClass.setId(UUID.randomUUID());
        return schoolClass;
    }

    private ExamDirectoryUserInfo user() {
        return new ExamDirectoryUserInfo(UUID.randomUUID(), "An Nguyen", "an@example.com", "ACTIVE");
    }
}
