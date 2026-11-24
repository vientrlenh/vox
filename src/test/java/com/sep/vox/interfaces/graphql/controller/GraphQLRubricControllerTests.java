package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricsUseCase;
import com.sep.vox.interfaces.shared.PageArguments;

@ExtendWith(MockitoExtension.class)
class GraphQLRubricControllerTests {

    @Mock
    private ViewSystemRubricsUseCase viewSystemRubricsUseCase;

    @InjectMocks
    private GraphQLRubricController controller;

    @Test
    void view_system_rubrics_should_reject_a_size_above_the_cap_without_calling_the_use_case() {
        assertThatThrownBy(() -> controller.viewSystemRubrics(1, PageArguments.MAX_PAGE_SIZE + 1))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(viewSystemRubricsUseCase);
    }
}
