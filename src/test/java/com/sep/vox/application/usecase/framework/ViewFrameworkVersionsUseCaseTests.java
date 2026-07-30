package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;

public class ViewFrameworkVersionsUseCaseTests {

    private FrameworkRepository frameworkRepository;
    private FrameworkVersionRepository frameworkVersionRepository;
    private ViewFrameworkVersionsUseCase useCase;
    private Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        frameworkRepository = mock(FrameworkRepository.class);
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        useCase = new ViewFrameworkVersionsUseCase(frameworkRepository, frameworkVersionRepository);
    }

    @Test
    void should_return_paginated_versions_when_framework_exists() {
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkVersionsQuery(frameworkId, null, 1, 20);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "CEFR", "Description",
            true, now, now, null, null
        );

        var version1 = new FrameworkVersion();
        version1.setId(UUID.randomUUID());
        version1.setFrameworkId(frameworkId);
        version1.setVersion(1);
        version1.setStatus(FrameworkVersionStatus.DRAFT);

        var version2 = new FrameworkVersion();
        version2.setId(UUID.randomUUID());
        version2.setFrameworkId(frameworkId);
        version2.setVersion(2);
        version2.setStatus(FrameworkVersionStatus.PUBLISHED);

        var pageResult = new PageResult<>(
            List.of(version1, version2),
            1,
            20,
            2,
            1
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkId(frameworkId, 1, 20))
            .thenReturn(pageResult);

        var result = useCase.execute(query);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(1);
        verify(frameworkRepository).findById(frameworkId);
    }

    @Test
    void should_throw_not_found_when_framework_missing() {
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkVersionsQuery(frameworkId, null, 1, 20);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(query));
    }

    @Test
    void should_return_empty_page_when_no_versions() {
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkVersionsQuery(frameworkId, null, 1, 20);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "CEFR", "Description",
            true, now, now, null, null
        );

        var emptyPage = new PageResult<FrameworkVersion>(List.of(), 1, 20, 0, 0);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkId(frameworkId, 1, 20))
            .thenReturn(emptyPage);

        var result = useCase.execute(query);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void should_handle_pagination_correctly() {
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkVersionsQuery(frameworkId, null, 2, 10);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "CEFR", "Description",
            true, now, now, null, null
        );

        var pageResult = new PageResult<FrameworkVersion>(List.of(), 2, 10, 25, 3);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkId(frameworkId, 2, 10))
            .thenReturn(pageResult);

        var result = useCase.execute(query);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void should_filter_by_status_when_status_provided() {
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkVersionsQuery(frameworkId, "published", 1, 20);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "CEFR", "Description",
            true, now, now, null, null
        );

        var version = new FrameworkVersion();
        version.setId(UUID.randomUUID());
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.PUBLISHED);

        var pageResult = new PageResult<>(List.of(version), 1, 20, 1, 1);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED, 1, 20))
            .thenReturn(pageResult);

        var result = useCase.execute(query);

        assertThat(result.content()).hasSize(1);
        verify(frameworkVersionRepository).findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED, 1, 20);
    }

    @Test
    void should_throw_when_status_invalid() {
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkVersionsQuery(frameworkId, "NOT_A_STATUS", 1, 20);

        var framework = new Framework(
            frameworkId, new FrameworkCode("CEFR"), "CEFR", "Description",
            true, now, now, null, null
        );
        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(query));
    }
}
