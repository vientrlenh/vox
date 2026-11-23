package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkDetailsQuery;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkDetailsUseCase;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class ViewFrameworkDetailsUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private ViewFrameworkDetailsUseCase useCase;

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        useCase = new ViewFrameworkDetailsUseCase(frameworkRepository);
    }

    @Test
    void should_return_framework_details() {
        UUID frameworkId = UUID.randomUUID();
        ViewFrameworkDetailsQuery query = new ViewFrameworkDetailsQuery(frameworkId);

        Framework framework = Framework.builder()
            .id(frameworkId)
            .code(new FrameworkCode("CEFR"))
            .name("CEFR Framework")
            .description("European Framework")
            .active(true)
            .build();

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));

        FrameworkDto result = useCase.execute(query);

        assertThat(result.id()).isEqualTo(frameworkId);
        assertThat(result.name()).isEqualTo("CEFR Framework");
        assertThat(result.isActive()).isTrue();
        verify(frameworkRepository).findById(frameworkId);
    }

    @Test
    void should_throw_not_found_when_framework_missing() {
        UUID frameworkId = UUID.randomUUID();
        ViewFrameworkDetailsQuery query = new ViewFrameworkDetailsQuery(frameworkId);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(query));
    }

    @Test
    void should_return_dto_with_all_fields() {
        UUID frameworkId = UUID.randomUUID();
        ViewFrameworkDetailsQuery query = new ViewFrameworkDetailsQuery(frameworkId);

        Framework framework = Framework.builder()
            .id(frameworkId)
            .code(new FrameworkCode("TOEIC"))
            .name("TOEIC Framework")
            .description("TOEIC Test")
            .active(false)
            .build();

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));

        FrameworkDto result = useCase.execute(query);

        assertThat(result.code()).isEqualTo("TOEIC");
        assertThat(result.description()).isEqualTo("TOEIC Test");
        assertThat(result.isActive()).isFalse();
    }
}
