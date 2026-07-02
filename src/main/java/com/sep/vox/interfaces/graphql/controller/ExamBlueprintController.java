package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewExamBlueprintDetailsQuery;
import com.sep.vox.application.port.input.query.ViewExamBlueprintsQuery;
import com.sep.vox.application.port.input.usecase.examblueprint.ViewExamBlueprintDetailsUseCase;
import com.sep.vox.application.port.input.usecase.examblueprint.ViewExamBlueprintsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.dto.ExamBlueprintSectionDto;
import com.sep.vox.domain.dto.ExamBlueprintSlotDto;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.ExamBlueprintSectionDtoMapper;
import com.sep.vox.domain.mapper.ExamBlueprintSlotDtoMapper;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;
import com.sep.vox.domain.repository.ExamBlueprintSectionRepository;
import com.sep.vox.domain.repository.ExamBlueprintSlotRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Controller("graphqlExamBlueprintController")
public class ExamBlueprintController {

    private final ViewExamBlueprintsUseCase viewExamBlueprintsUseCase;
    private final ViewExamBlueprintDetailsUseCase viewExamBlueprintDetailsUseCase;
    private final ExamBlueprintVersionRepository examBlueprintVersionRepository;
    private final ExamBlueprintSectionRepository examBlueprintSectionRepository;
    private final ExamBlueprintSlotRepository examBlueprintSlotRepository;
    private final QuestionRepository questionRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ExamBlueprintController(
            ViewExamBlueprintsUseCase viewExamBlueprintsUseCase,
            ViewExamBlueprintDetailsUseCase viewExamBlueprintDetailsUseCase,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamBlueprintSectionRepository examBlueprintSectionRepository,
            ExamBlueprintSlotRepository examBlueprintSlotRepository,
            QuestionRepository questionRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.viewExamBlueprintsUseCase = viewExamBlueprintsUseCase;
        this.viewExamBlueprintDetailsUseCase = viewExamBlueprintDetailsUseCase;
        this.examBlueprintVersionRepository = examBlueprintVersionRepository;
        this.examBlueprintSectionRepository = examBlueprintSectionRepository;
        this.examBlueprintSlotRepository = examBlueprintSlotRepository;
        this.questionRepository = questionRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @QueryMapping(name = "examBlueprints")
    public PageResult<ExamBlueprintDto> examBlueprints(
            @Argument UUID schoolId,
            @Argument Boolean isActive,
            @Argument UUID languageId,
            @Argument String examKind,
            @Argument String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        return viewExamBlueprintsUseCase.execute(
            new ViewExamBlueprintsQuery(schoolId, isActive, languageId, examKind, keyword, page, size)
        );
    }

    @QueryMapping(name = "examBlueprint")
    public ExamBlueprintDto examBlueprint(@Argument(name = "id") UUID id) {
        return viewExamBlueprintDetailsUseCase.execute(new ViewExamBlueprintDetailsQuery(id));
    }

    @SchemaMapping(typeName = "ExamBlueprint", field = "versions")
    public List<ExamBlueprintVersionDto> versions(ExamBlueprintDto source, @Argument ExamBlueprintVersionStatus status) {
        if (status == null) {
            return ExamBlueprintVersionDtoMapper.toDtoList(examBlueprintVersionRepository.findByBlueprintId(source.id()));
        }
        return ExamBlueprintVersionDtoMapper.toDtoList(
            examBlueprintVersionRepository.findByBlueprintIdAndStatus(source.id(), status)
        );
    }

    @SchemaMapping(typeName = "ExamBlueprintVersion", field = "sections")
    public List<ExamBlueprintSectionDto> sections(ExamBlueprintVersionDto source) {
        return ExamBlueprintSectionDtoMapper.toDtoList(examBlueprintSectionRepository.findByBlueprintVersionId(source.id()));
    }

    @SchemaMapping(typeName = "ExamBlueprintSection", field = "slots")
    public List<ExamBlueprintSlotDto> slots(ExamBlueprintSectionDto source) {
        return ExamBlueprintSlotDtoMapper.toDtoList(examBlueprintSlotRepository.findBySectionId(source.id()));
    }

    @SchemaMapping(typeName = "ExamBlueprintSlot", field = "fixedQuestion")
    public QuestionDto fixedQuestion(ExamBlueprintSlotDto source) {
        if (source.fixedQuestionId() == null) {
            return null;
        }
        return resolveQuestion(source.fixedQuestionId());
    }

    private QuestionDto resolveQuestion(UUID questionId) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var systemAdmin = userContextPort.isSystemAdmin();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return questionRepository.findAccessibleById(questionId, currentUserId, currentSchoolId, systemAdmin, schoolAdmin)
            .map(QuestionDtoMapper::toQuestionDto)
            .orElse(null);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}
