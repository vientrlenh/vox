package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkDetailsQuery;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkDetailsUseCase;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class ViewFrameworkDetailsUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private ViewFrameworkDetailsUseCase useCase;
    private OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        useCase = new ViewFrameworkDetailsUseCase(frameworkRepository);
    }

    @Test
    void should_return_framework_details() {
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkDetailsQuery(frameworkId);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "CEFR Framework", "European Framework",
            true, UUID.randomUUID(), now, now, null, null
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));

        var result = useCase.execute(query);

        assertThat(result.id()).isEqualTo(frameworkId);
        assertThat(result.name()).isEqualTo("CEFR Framework");
        assertThat(result.isActive()).isTrue();
        verify(frameworkRepository).findById(frameworkId);
    }

    @Test
    void should_throw_not_found_when_framework_missing() {
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkDetailsQuery(frameworkId);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(query));
    }

    @Test
    void should_return_dto_with_all_fields() {
        var frameworkId = UUID.randomUUID();
        var currentVersionId = UUID.randomUUID();
        var query = new ViewFrameworkDetailsQuery(frameworkId);

        var framework = new Framework(
            frameworkId, new FrameworkCode("TOEIC"), "TOEIC Framework", "TOEIC Test",
            false, currentVersionId, now, now, null, null
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));

        var result = useCase.execute(query);

        assertThat(result.code()).isEqualTo("TOEIC");
        assertThat(result.description()).isEqualTo("TOEIC Test");
        assertThat(result.isActive()).isFalse();
        assertThat(result.currentVersionId()).isEqualTo(currentVersionId);
    }
}
