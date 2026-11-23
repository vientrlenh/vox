package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBanksUseCase;

@ExtendWith(MockitoExtension.class)
class GraphQLQuestionBankControllerTests {

    @Mock
    private ViewQuestionBanksUseCase viewQuestionBanksUseCase;

    @InjectMocks
    private GraphQLQuestionBankController controller;

    @Test
    void question_banks_should_reject_page_zero_without_calling_the_use_case() {
        assertThatThrownBy(() -> controller.questionBanks(null, null, null, null, null, null, 0, 20))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(viewQuestionBanksUseCase);
    }
}
