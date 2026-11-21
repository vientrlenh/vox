package com.sep.vox.application.usecase.rubricsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.CreateSystemRubricResultBandsCommand;
import com.sep.vox.application.port.input.command.CreateSystemRubricResultBandsCommand.ResultBandItemCommand;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricResultBandsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.UserRepository;

public class CreateSystemRubricResultBandsUseCaseTests {

    private RubricResultBandRepository rubricResultBandRepository;
    private RubricVersionRepository rubricVersionRepository;
    private RubricRepository rubricRepository;
    private UserContextPort userContextPort;
    private UserRepository userRepository;
    private CreateSystemRubricResultBandsUseCase useCase;

    private final UUID rubricId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        rubricResultBandRepository = mock(RubricResultBandRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        rubricRepository = mock(RubricRepository.class);
        userContextPort = mock(UserContextPort.class);
        userRepository = mock(UserRepository.class);
        useCase = new CreateSystemRubricResultBandsUseCase(
                rubricResultBandRepository, rubricVersionRepository, rubricRepository,
                userContextPort, userRepository);

        RubricVersion version = RubricVersion.builder()
                .id(versionId)
                .rubricId(rubricId)
                .version(1)
                .code("V1")
                .name("Version 1")
                .description("Desc")
                .status(RubricStatus.DRAFT)
                .effectiveFrom(now)
                .effectiveTo(now.plus(30, ChronoUnit.DAYS))
                .scoringScaleMin(BigDecimal.valueOf(0))
                .scoringScaleMax(BigDecimal.valueOf(10))
                .totalScoreMethod(RubricTotalScoreMethod.WEIGHTED_AVERAGE)
                .build();
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(version));

        Rubric rubric = Rubric.builder()
                .id(rubricId)
                .languageId(UUID.randomUUID())
                .frameworkId(UUID.randomUUID())
                .code("R1")
                .name("Rubric 1")
                .description("Desc")
                .ownerType(RubricOwnerType.SYSTEM)
                .schoolId(null)
                .build();
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(rubric));

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        User currentUser = mock(User.class);
        when(currentUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        when(rubricResultBandRepository.findByRubricVersionId(versionId)).thenReturn(List.of());

        when(rubricResultBandRepository.saveAll(any())).thenAnswer(invocation -> {
            List<com.sep.vox.domain.model.rubric.RubricResultBand> bands = invocation.getArgument(0);
            bands.forEach(b -> b.setId(UUID.randomUUID()));
            return bands;
        });
    }

    private ResultBandItemCommand band(String code, String name, double min, double max, int order) {
        return new ResultBandItemCommand(code, name, null, BigDecimal.valueOf(min), BigDecimal.valueOf(max), order);
    }

    // 4 mức: Yếu / Trung bình / Khá / Giỏi, khớp thang điểm tổng 0-10 của version
    private List<ResultBandItemCommand> fourLevels() {
        return List.of(
                band("YEU", "Yếu", 0, 3.99, 1),
                band("TB", "Trung bình", 4, 5.99, 2),
                band("KHA", "Khá", 6, 7.99, 3),
                band("GIOI", "Giỏi", 8, 10, 4));
    }

    @Test
    void should_create_4_result_bands_successfully() {
        CreateSystemRubricResultBandsCommand command = new CreateSystemRubricResultBandsCommand(versionId, fourLevels());

        List<UUID> ids = useCase.execute(command);

        assertThat(ids).hasSize(4);
        assertThat(ids).allSatisfy(id -> assertThat(id).isNotNull());
    }

    @Test
    void should_throw_when_version_not_draft() {
        RubricVersion publishedVersion = RubricVersion.builder()
                .id(versionId)
                .rubricId(rubricId)
                .version(1)
                .code("V1")
                .name("Version 1")
                .description("Desc")
                .status(RubricStatus.PUBLISHED)
                .effectiveFrom(now)
                .effectiveTo(now.plus(30, ChronoUnit.DAYS))
                .scoringScaleMin(BigDecimal.valueOf(0))
                .scoringScaleMax(BigDecimal.valueOf(10))
                .totalScoreMethod(RubricTotalScoreMethod.WEIGHTED_AVERAGE)
                .build();
        when(rubricVersionRepository.findById(versionId)).thenReturn(Optional.of(publishedVersion));

        CreateSystemRubricResultBandsCommand command = new CreateSystemRubricResultBandsCommand(versionId, fourLevels());

        assertThrows(IllegalStateException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_forbidden_when_rubric_belongs_to_school() {
        Rubric schoolRubric = Rubric.builder()
                .id(rubricId)
                .languageId(UUID.randomUUID())
                .frameworkId(UUID.randomUUID())
                .code("R1")
                .name("Rubric 1")
                .description("Desc")
                .ownerType(RubricOwnerType.SCHOOL)
                .schoolId(UUID.randomUUID())
                .build();
        when(rubricRepository.findById(rubricId)).thenReturn(Optional.of(schoolRubric));

        CreateSystemRubricResultBandsCommand command = new CreateSystemRubricResultBandsCommand(versionId, fourLevels());

        assertThrows(ForbiddenException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_bands_overlap() {
        List<CreateSystemRubricResultBandsCommand.ResultBandItemCommand> overlapping = List.of(
                band("YEU", "Yếu", 0, 4, 1),
                band("TB", "Trung bình", 4, 6, 2));
        CreateSystemRubricResultBandsCommand command = new CreateSystemRubricResultBandsCommand(versionId, overlapping);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_score_out_of_scale() {
        List<CreateSystemRubricResultBandsCommand.ResultBandItemCommand> outOfScale = List.of(band("YEU", "Yếu", -1, 3, 1));
        CreateSystemRubricResultBandsCommand command = new CreateSystemRubricResultBandsCommand(versionId, outOfScale);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }

    @Test
    void should_throw_when_duplicate_code_in_batch() {
        List<CreateSystemRubricResultBandsCommand.ResultBandItemCommand> duplicated = List.of(
                band("YEU", "Yếu", 0, 3.99, 1),
                band("YEU", "Trung bình", 4, 5.99, 2));
        CreateSystemRubricResultBandsCommand command = new CreateSystemRubricResultBandsCommand(versionId, duplicated);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command));
    }
}