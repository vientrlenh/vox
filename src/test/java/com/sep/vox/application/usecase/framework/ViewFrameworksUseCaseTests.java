package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewFrameworksQuery;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworksUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class ViewFrameworksUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private ViewFrameworksUseCase useCase;

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        useCase = new ViewFrameworksUseCase(frameworkRepository);
    }

    @Test
    void should_return_paginated_frameworks() {
        ViewFrameworksQuery query = new ViewFrameworksQuery(1, 10, null, null);

        Framework fw1 = Framework.builder()
            .id(UUID.randomUUID())
            .code(new FrameworkCode("CEFR"))
            .name("CEFR Framework")
            .description("Description 1")
            .active(true)
            .build();
        Framework fw2 = Framework.builder()
            .id(UUID.randomUUID())
            .code(new FrameworkCode("TOEIC"))
            .name("TOEIC Framework")
            .description("Description 2")
            .active(true)
            .build();

        PageResult<Framework> pageResult = new PageResult<>(
            List.of(fw1, fw2),
            1,
            10,
            2,
            1
        );

        when(frameworkRepository.findAll(1, 10, null, null))
            .thenReturn(pageResult);

        PageResult<FrameworkDto> result = useCase.execute(query);

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(2);
        verify(frameworkRepository).findAll(1, 10, null, null);
    }

    @Test
    void should_return_empty_when_no_frameworks() {
        ViewFrameworksQuery query = new ViewFrameworksQuery(1, 10, null, null);
        PageResult<Framework> emptyPage = new PageResult<Framework>(List.of(), 1, 10, 0, 0);

        when(frameworkRepository.findAll(1, 10, null, null))
            .thenReturn(emptyPage);

        PageResult<FrameworkDto> result = useCase.execute(query);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void should_convert_page_to_zero_based_index() {
        ViewFrameworksQuery query = new ViewFrameworksQuery(2, 10, null, null);

        Framework fw1 = Framework.builder()
            .id(UUID.randomUUID())
            .code(new FrameworkCode("CEFR"))
            .name("CEFR")
            .description("Description")
            .active(true)
            .build();

        PageResult<Framework> pageResult = new PageResult<>(
            List.of(fw1),
            2,
            10,
            1,
            1
        );

        when(frameworkRepository.findAll(2, 10, null, null))
            .thenReturn(pageResult);

        useCase.execute(query);

        verify(frameworkRepository).findAll(2, 10, null, null);
    }
}
