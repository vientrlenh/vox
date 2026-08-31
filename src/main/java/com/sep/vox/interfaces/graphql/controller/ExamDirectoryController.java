package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewExamDirectoryQuery;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryClassesUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryGradesUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryProctorsUseCase;
import com.sep.vox.application.port.input.usecase.examdirectory.ViewExamDirectoryStudentsUseCase;
import com.sep.vox.application.query.dto.ExamDirectoryGradeInfo;
import com.sep.vox.application.query.dto.ExamDirectoryUserInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.interfaces.shared.PageArguments;

/**
 * Bề mặt GraphQL "danh bạ kỳ thi" — chỉ đọc.
 *
 * <p>Tách khỏi {@link SchoolController} (toàn quyền SCHOOL_ADMIN) để không phải nới
 * quyền trên các type dùng chung. {@code @PreAuthorize} ở đây chỉ là chốt thô cho
 * đúng hai vai trò có thể tổ chức kỳ thi; luật thật (school admin cùng trường HOẶC
 * chủ tịch kỳ thi, và phạm vi theo {@code ExamKind}) nằm ở
 * {@code ExamDirectoryAccessService} — vì "chủ tịch" là một dòng trong
 * {@code exam_members}, không phải một vai trò toàn cục.
 */
@Controller
public class ExamDirectoryController {

    private final ViewExamDirectoryClassesUseCase viewExamDirectoryClassesUseCase;
    private final ViewExamDirectoryGradesUseCase viewExamDirectoryGradesUseCase;
    private final ViewExamDirectoryStudentsUseCase viewExamDirectoryStudentsUseCase;
    private final ViewExamDirectoryProctorsUseCase viewExamDirectoryProctorsUseCase;

    public ExamDirectoryController(
            ViewExamDirectoryClassesUseCase viewExamDirectoryClassesUseCase,
            ViewExamDirectoryGradesUseCase viewExamDirectoryGradesUseCase,
            ViewExamDirectoryStudentsUseCase viewExamDirectoryStudentsUseCase,
            ViewExamDirectoryProctorsUseCase viewExamDirectoryProctorsUseCase) {
        this.viewExamDirectoryClassesUseCase = viewExamDirectoryClassesUseCase;
        this.viewExamDirectoryGradesUseCase = viewExamDirectoryGradesUseCase;
        this.viewExamDirectoryStudentsUseCase = viewExamDirectoryStudentsUseCase;
        this.viewExamDirectoryProctorsUseCase = viewExamDirectoryProctorsUseCase;
    }

    @QueryMapping(name = "examDirectoryClasses")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<SchoolClassDto> examDirectoryClasses(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewExamDirectoryClassesUseCase.execute(query(examId, search, page, size));
    }

    @QueryMapping(name = "examDirectoryGrades")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<ExamDirectoryGradeInfo> examDirectoryGrades(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewExamDirectoryGradesUseCase.execute(query(examId, search, page, size));
    }

    @QueryMapping(name = "examDirectoryStudents")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<ExamDirectoryUserInfo> examDirectoryStudents(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "excludeUserIds") List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return viewExamDirectoryStudentsUseCase.execute(query(examId, search, page, size, excludeUserIds));
    }

    @QueryMapping(name = "examDirectoryProctors")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER')")
    public PageResult<ExamDirectoryUserInfo> examDirectoryProctors(
            @Argument(name = "examId") UUID examId,
            @Argument(name = "search") String search,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "excludeUserIds") List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return viewExamDirectoryProctorsUseCase.execute(query(examId, search, page, size, excludeUserIds));
    }

    private static ViewExamDirectoryQuery query(UUID examId, String search, Integer page, Integer size) {
        return query(examId, search, page, size, List.of());
    }

    private static ViewExamDirectoryQuery query(UUID examId, String search, Integer page, Integer size,
            List<UUID> excludeUserIds) {
        PageArguments.validate(page, size);
        return new ViewExamDirectoryQuery(examId, search, page, size, excludeUserIds);
    }
}
