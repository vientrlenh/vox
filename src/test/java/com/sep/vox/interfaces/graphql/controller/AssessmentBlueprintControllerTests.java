package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.usecase.assessmentblueprint.ViewAssessmentBlueprintsUseCase;

@ExtendWith(MockitoExtension.class)
class AssessmentBlueprintControllerTests {

    @Mock
    private ViewAssessmentBlueprintsUseCase viewAssessmentBlueprintsUseCase;

    @InjectMocks
    private AssessmentBlueprintController controller;

    @Test
    void assessment_blueprints_should_reject_page_zero_without_calling_the_use_case() {
        assertThatThrownBy(() -> controller.assessmentBlueprints(null, null, null, null, null, 0, 20))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(viewAssessmentBlueprintsUseCase);
    }
}
