package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewFrameworksQuery;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworksUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class ViewFrameworksUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private ViewFrameworksUseCase useCase;
    private OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        useCase = new ViewFrameworksUseCase(frameworkRepository);
    }

    @Test
    void should_return_paginated_frameworks() {
        var query = new ViewFrameworksQuery(1, 10);

        var fw1 = new Framework(
            UUID.randomUUID(), new FrameworkCode("CEFR"), "CEFR Framework", "Description 1",
            true, now, now, null, null
        );
        var fw2 = new Framework(
            UUID.randomUUID(), new FrameworkCode("TOEIC"), "TOEIC Framework", "Description 2",
            true, now, now, null, null
        );

        var pageResult = new PageResult<>(
            List.of(fw1, fw2),
            1,
            10,
            2,
            1
        );

        when(frameworkRepository.findAll(1, 10))
            .thenReturn(pageResult);

        var result = useCase.execute(query);

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(2);
        verify(frameworkRepository).findAll(1, 10);
    }

    @Test
    void should_return_empty_when_no_frameworks() {
        var query = new ViewFrameworksQuery(1, 10);
        var emptyPage = new PageResult<Framework>(List.of(), 1, 10, 0, 0);

        when(frameworkRepository.findAll(1, 10))
            .thenReturn(emptyPage);

        var result = useCase.execute(query);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void should_convert_page_to_zero_based_index() {
        var query = new ViewFrameworksQuery(2, 10);

        var fw1 = new Framework(
            UUID.randomUUID(), new FrameworkCode("CEFR"), "CEFR", "Description",
            true, now, now, null, null
        );

        var pageResult = new PageResult<>(
            List.of(fw1),
            2,
            10,
            1,
            1
        );

        when(frameworkRepository.findAll(2, 10))
            .thenReturn(pageResult);

        useCase.execute(query);

        verify(frameworkRepository).findAll(2, 10);
    }
}
