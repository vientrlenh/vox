package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.query.ViewExamDetailsQuery;
import com.sep.vox.application.port.input.query.ViewExamsQuery;
import com.sep.vox.application.port.input.usecase.exam.ViewExamDetailsUseCase;
import com.sep.vox.application.port.input.usecase.exam.ViewExamsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamMemberDto;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.domain.dto.ExamPaperSectionDto;
import com.sep.vox.domain.dto.ExamSecurePoolDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.ExamMemberDtoMapper;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.mapper.ExamPaperItemDtoMapper;
import com.sep.vox.domain.mapper.ExamPaperSectionDtoMapper;
import com.sep.vox.domain.mapper.ExamSecurePoolDtoMapper;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamPaperStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamSecurePoolRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

@Controller("graphqlExamController")
public class ExamController {

    private final ViewExamsUseCase viewExamsUseCase;
    private final ViewExamDetailsUseCase viewExamDetailsUseCase;
    private final ExamMemberRepository examMemberRepository;
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperSectionRepository examPaperSectionRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final ExamSecurePoolRepository examSecurePoolRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final QuestionRepository questionRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public ExamController(
            ViewExamsUseCase viewExamsUseCase,
            ViewExamDetailsUseCase viewExamDetailsUseCase,
            ExamMemberRepository examMemberRepository,
            ExamPaperRepository examPaperRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamSecurePoolRepository examSecurePoolRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            QuestionRepository questionRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.viewExamsUseCase = viewExamsUseCase;
        this.viewExamDetailsUseCase = viewExamDetailsUseCase;
        this.examMemberRepository = examMemberRepository;
        this.examPaperRepository = examPaperRepository;
        this.examPaperSectionRepository = examPaperSectionRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.examSecurePoolRepository = examSecurePoolRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.questionRepository = questionRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    @QueryMapping(name = "exams")
    public PageResult<ExamDto> exams(
            @Argument ExamKind kind,
            @Argument ExamStatus status,
            @Argument UUID schoolId,
            @Argument String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        return viewExamsUseCase.execute(new ViewExamsQuery(kind, status, schoolId, null, keyword, page, size));
    }

    @QueryMapping(name = "classTests")
    public PageResult<ExamDto> classTests(
            @Argument ExamStatus status,
            @Argument UUID schoolClassId,
            @Argument String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        return viewExamsUseCase.execute(
            new ViewExamsQuery(ExamKind.CLASS_TEST, status, null, schoolClassId, keyword, page, size)
        );
    }

    @QueryMapping(name = "exam")
    public ExamDto exam(@Argument(name = "id") UUID id) {
        return viewExamDetailsUseCase.execute(new ViewExamDetailsQuery(id));
    }

    @SchemaMapping(typeName = "Exam", field = "members")
    public List<ExamMemberDto> members(ExamDto source) {
        return ExamMemberDtoMapper.toDtoList(examMemberRepository.findByExamId(source.id()));
    }

    @SchemaMapping(typeName = "ExamMember", field = "user")
    public CompletableFuture<UserDto> examMemberUser(ExamMemberDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.userId());
    }

    @SchemaMapping(typeName = "Exam", field = "papers")
    public List<ExamPaperDto> papers(ExamDto source, @Argument ExamPaperStatus status) {
        if (status == null) {
            return ExamPaperDtoMapper.toDtoList(examPaperRepository.findByExamId(source.id()));
        }
        return ExamPaperDtoMapper.toDtoList(examPaperRepository.findByExamIdAndStatus(source.id(), status));
    }

    @SchemaMapping(typeName = "ExamPaper", field = "sections")
    public List<ExamPaperSectionDto> sections(ExamPaperDto source) {
        return ExamPaperSectionDtoMapper.toDtoList(examPaperSectionRepository.findByPaperId(source.id()));
    }

    @SchemaMapping(typeName = "ExamPaperSection", field = "items")
    public List<ExamPaperItemDto> items(ExamPaperSectionDto source) {
        return ExamPaperItemDtoMapper.toDtoList(examPaperItemRepository.findBySectionId(source.id()));
    }

    @SchemaMapping(typeName = "Exam", field = "securePool")
    public ExamSecurePoolDto securePool(ExamDto source) {
        return examSecurePoolRepository.findByExamId(source.id())
            .map(ExamSecurePoolDtoMapper::toDto)
            .orElse(null);
    }

    @SchemaMapping(typeName = "Exam", field = "schoolClassId")
    public UUID schoolClassId(ExamDto source) {
        return examCandidateRepository.findByExamId(source.id()).stream()
            .findFirst()
            .flatMap(candidate -> schoolClassUserRepository.findByUserId(candidate.getStudentId()).stream()
                .filter(SchoolClassUser::isActive)
                .findFirst())
            .map(SchoolClassUser::getSchoolClassId)
            .orElse(null);
    }

    @SchemaMapping(typeName = "ExamPaperItem", field = "question")
    public QuestionDto question(ExamPaperItemDto source) {
        if (source.questionId() == null) {
            return null;
        }
        return resolveQuestion(source.questionId());
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
