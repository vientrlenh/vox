package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.assessment.CanViewAssessmentBlueprintDataUseCase;
import com.sep.vox.application.port.input.usecase.assessment.EstimateAssessmentTokenQuotaUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentStatusCountsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewMyAssessmentRoleUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpaper.ViewAssessmentPaperDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.dto.AssessmentPaperItemDto;
import com.sep.vox.domain.dto.QuestionSelectionSpecDto;
import com.sep.vox.interfaces.graphql.dataloader.AssessmentGraphQLDataLoaderConfig;

import graphql.schema.DataFetchingEnvironment;

/**
 * The paper-variant edit page has to tell a FIXED slot (locked) from a SELECTION slot (the question is chosen freely). Previously
 * AssessmentPaperItem did not expose slotType so the frontend could only rely on blueprintSlotId and hid the assign-question button
 * for both kinds.
 */
class AssessmentPaperItemSlotFieldTests {

    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final UUID SECTION_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();

    private GraphQLAssessmentController controller;

    @BeforeEach
    void setUp() {
        controller = new GraphQLAssessmentController(
            mock(ViewAssessmentsUseCase.class),
            mock(ViewAssessmentDetailsUseCase.class),
            mock(ViewAssessmentPaperDetailsUseCase.class),
            mock(ViewAssessmentStatusCountsUseCase.class),
            mock(ViewMyAssessmentRoleUseCase.class),
            mock(CanViewAssessmentBlueprintDataUseCase.class),
            mock(EstimateAssessmentTokenQuotaUseCase.class),
            mock(UserContextPort.class)
        );
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_expose_the_slot_type_of_a_blueprint_backed_item() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, AssessmentBlueprintSlotDto>getDataLoader(AssessmentGraphQLDataLoaderConfig.ASSESSMENT_BLUEPRINT_SLOT_BY_ASSESSMENT_BLUEPRINT_SLOT_ID)).thenReturn(loader);
        when(loader.load(SLOT_ID)).thenReturn(CompletableFuture.completedFuture(slot(AssessmentBlueprintSlotType.SELECTION, null)));

        CompletableFuture<AssessmentBlueprintSlotType> result = controller.assessmentPaperItemSlotType(item(SLOT_ID), env);

        assertThat(result.join()).isEqualTo(AssessmentBlueprintSlotType.SELECTION);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_expose_the_selection_criteria_of_a_selection_slot() {
        QuestionSelectionSpecDto spec = new QuestionSelectionSpecDto("READ_ALOUD", null, null, null, UUID.randomUUID());
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, AssessmentBlueprintSlotDto>getDataLoader(AssessmentGraphQLDataLoaderConfig.ASSESSMENT_BLUEPRINT_SLOT_BY_ASSESSMENT_BLUEPRINT_SLOT_ID)).thenReturn(loader);
        when(loader.load(SLOT_ID)).thenReturn(CompletableFuture.completedFuture(slot(AssessmentBlueprintSlotType.SELECTION, spec)));

        CompletableFuture<QuestionSelectionSpecDto> result = controller.assessmentPaperItemSelectionSpec(item(SLOT_ID), env);

        assertThat(result.join()).isSameAs(spec);
    }

    /**
     * A hand-written question is not generated from a blueprint -- it must not cost an extra slot
     * query.
     */
    @Test
    void should_return_null_slot_type_for_an_item_without_a_blueprint_slot() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

        CompletableFuture<AssessmentBlueprintSlotType> result = controller.assessmentPaperItemSlotType(item(null), env);

        assertThat(result.join()).isNull();
        verify(env, never()).getDataLoader(any());
    }

    /**
     * A slot that has been deleted: the mapped loader returns null, and the page must still allow
     * assigning a question (fail-open).
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void should_return_null_slot_type_when_the_slot_row_is_missing() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        DataLoader loader = mock(DataLoader.class);
        when(env.<UUID, AssessmentBlueprintSlotDto>getDataLoader(AssessmentGraphQLDataLoaderConfig.ASSESSMENT_BLUEPRINT_SLOT_BY_ASSESSMENT_BLUEPRINT_SLOT_ID)).thenReturn(loader);
        when(loader.load(SLOT_ID)).thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<AssessmentBlueprintSlotType> result = controller.assessmentPaperItemSlotType(item(SLOT_ID), env);

        assertThat(result.join()).isNull();
    }

    private AssessmentPaperItemDto item(UUID blueprintSlotId) {
        return new AssessmentPaperItemDto(ITEM_ID, blueprintSlotId, SECTION_ID, PAPER_ID, null, 1, BigDecimal.ONE);
    }

    private AssessmentBlueprintSlotDto slot(AssessmentBlueprintSlotType slotType, QuestionSelectionSpecDto spec) {
        return new AssessmentBlueprintSlotDto(
            SLOT_ID, SECTION_ID, UUID.randomUUID(), 1, BigDecimal.ONE,
            null, null, slotType, null, spec, null, null, null, null);
    }
}
