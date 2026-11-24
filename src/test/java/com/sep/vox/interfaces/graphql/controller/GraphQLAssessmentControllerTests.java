package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.usecase.assessment.ViewAssessmentsUseCase;

@ExtendWith(MockitoExtension.class)
class GraphQLAssessmentControllerTests {

    @Mock
    private ViewAssessmentsUseCase viewAssessmentsUseCase;

    @InjectMocks
    private GraphQLAssessmentController controller;

    @Test
    void assessments_should_reject_page_zero_without_calling_the_use_case() {
        assertThatThrownBy(() -> controller.assessments(null, null, null, null, 0, 20))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(viewAssessmentsUseCase);
    }

    @Test
    void class_tests_should_reject_page_zero_without_calling_the_use_case() {
        assertThatThrownBy(() -> controller.classTests(null, null, null, 0, 20))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(viewAssessmentsUseCase);
    }
}
