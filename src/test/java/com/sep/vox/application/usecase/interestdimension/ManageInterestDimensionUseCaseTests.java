package com.sep.vox.application.usecase.interestdimension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.command.interestdimension.UpsertInterestDimensionCommand;
import com.sep.vox.application.port.input.service.InterestQuizBankTopUpService;
import com.sep.vox.application.port.input.usecase.interestdimension.ManageInterestDimensionUseCase;
import com.sep.vox.domain.model.personalization.InterestDimension;
import com.sep.vox.domain.repository.InterestDimensionRepository;

@ExtendWith(MockitoExtension.class)
class ManageInterestDimensionUseCaseTests {

    private static final String CODE = "MUSIC";
    private static final String STORED_DESCRIPTION = "Stored description";

    @Mock
    private InterestDimensionRepository repository;

    @Mock
    private InterestQuizBankTopUpService quizBankTopUpService;

    @InjectMocks
    private ManageInterestDimensionUseCase useCase;

    private InterestDimension storedDimension() {
        return InterestDimension.builder().code(CODE).label("Music").description(STORED_DESCRIPTION)
            .active(true).quizEligible(true).displayOrder(1).build();
    }

    private InterestDimension updateAndCaptureSaved(UpsertInterestDimensionCommand command) {
        when(repository.findByCode(CODE)).thenReturn(Optional.of(storedDimension()));
        when(repository.save(any(InterestDimension.class))).thenAnswer(invocation -> invocation.getArgument(0));
        useCase.update(command);
        ArgumentCaptor<InterestDimension> saved = ArgumentCaptor.forClass(InterestDimension.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void update_keeps_the_description_when_it_is_not_provided() {
        InterestDimension saved = updateAndCaptureSaved(
            new UpsertInterestDimensionCommand(CODE, "New label", null, false, null, null, null));

        assertThat(saved.getDescription()).isEqualTo(STORED_DESCRIPTION);
        assertThat(saved.getLabel()).isEqualTo("New label");
    }

    @Test
    void update_clears_the_description_when_it_is_provided_as_null() {
        InterestDimension saved = updateAndCaptureSaved(
            new UpsertInterestDimensionCommand(CODE, null, null, true, null, null, null));

        assertThat(saved.getDescription()).isNull();
    }

    @Test
    void update_replaces_the_description_when_a_value_is_provided() {
        InterestDimension saved = updateAndCaptureSaved(
            new UpsertInterestDimensionCommand(CODE, null, "  Fresh  ", true, null, null, null));

        assertThat(saved.getDescription()).isEqualTo("Fresh");
    }

    @Test
    void create_ignores_the_flag_and_stores_no_description_for_a_null_one() {
        when(repository.findByCode(CODE)).thenReturn(Optional.empty());
        when(repository.save(any(InterestDimension.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.create(new UpsertInterestDimensionCommand(CODE, "Music", null, true, null, null, null));

        ArgumentCaptor<InterestDimension> saved = ArgumentCaptor.forClass(InterestDimension.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getDescription()).isNull();
    }
}
