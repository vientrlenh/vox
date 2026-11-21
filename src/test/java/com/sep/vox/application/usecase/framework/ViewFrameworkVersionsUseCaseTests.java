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
import com.sep.vox.domain.dto.FrameworkVersionDto;
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
        UUID frameworkId = UUID.randomUUID();
        ViewFrameworkVersionsQuery query = new ViewFrameworkVersionsQuery(frameworkId, null, 1, 20);

        Framework framework = Framework.builder()
            .id(frameworkId)
            .code(new FrameworkCode("CEFR"))
            .name("CEFR")
            .description("Description")
            .active(true)
            .build();

        FrameworkVersion version1 = new FrameworkVersion();
        version1.setId(UUID.randomUUID());
        version1.setFrameworkId(frameworkId);
        version1.setVersion(1);
        version1.setStatus(FrameworkVersionStatus.DRAFT);

        FrameworkVersion version2 = new FrameworkVersion();
        version2.setId(UUID.randomUUID());
        version2.setFrameworkId(frameworkId);
        version2.setVersion(2);
        version2.setStatus(FrameworkVersionStatus.PUBLISHED);

        PageResult<FrameworkVersion> pageResult = new PageResult<>(
            List.of(version1, version2),
            1,
            20,
            2,
            1
        );

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkId(frameworkId, 1, 20))
            .thenReturn(pageResult);

        PageResult<FrameworkVersionDto> result = useCase.execute(query);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(1);
        verify(frameworkRepository).findById(frameworkId);
    }

    @Test
    void should_throw_not_found_when_framework_missing() {
        UUID frameworkId = UUID.randomUUID();
        ViewFrameworkVersionsQuery query = new ViewFrameworkVersionsQuery(frameworkId, null, 1, 20);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(query));
    }

    @Test
    void should_return_empty_page_when_no_versions() {
        UUID frameworkId = UUID.randomUUID();
        ViewFrameworkVersionsQuery query = new ViewFrameworkVersionsQuery(frameworkId, null, 1, 20);

        Framework framework = Framework.builder()
            .id(frameworkId)
            .code(new FrameworkCode("CEFR"))
            .name("CEFR")
            .description("Description")
            .active(true)
            .build();

        PageResult<FrameworkVersion> emptyPage = new PageResult<FrameworkVersion>(List.of(), 1, 20, 0, 0);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkId(frameworkId, 1, 20))
            .thenReturn(emptyPage);

        PageResult<FrameworkVersionDto> result = useCase.execute(query);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void should_handle_pagination_correctly() {
        UUID frameworkId = UUID.randomUUID();
        ViewFrameworkVersionsQuery query = new ViewFrameworkVersionsQuery(frameworkId, null, 2, 10);

        Framework framework = Framework.builder()
            .id(frameworkId)
            .code(new FrameworkCode("CEFR"))
            .name("CEFR")
            .description("Description")
            .active(true)
            .build();

        PageResult<FrameworkVersion> pageResult = new PageResult<FrameworkVersion>(List.of(), 2, 10, 25, 3);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkId(frameworkId, 2, 10))
            .thenReturn(pageResult);

        PageResult<FrameworkVersionDto> result = useCase.execute(query);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void should_filter_by_status_when_status_provided() {
        UUID frameworkId = UUID.randomUUID();
        ViewFrameworkVersionsQuery query = new ViewFrameworkVersionsQuery(frameworkId, FrameworkVersionStatus.PUBLISHED, 1, 20);

        Framework framework = Framework.builder()
            .id(frameworkId)
            .code(new FrameworkCode("CEFR"))
            .name("CEFR")
            .description("Description")
            .active(true)
            .build();

        FrameworkVersion version = new FrameworkVersion();
        version.setId(UUID.randomUUID());
        version.setFrameworkId(frameworkId);
        version.setStatus(FrameworkVersionStatus.PUBLISHED);

        PageResult<FrameworkVersion> pageResult = new PageResult<>(List.of(version), 1, 20, 1, 1);

        when(frameworkRepository.findById(frameworkId)).thenReturn(Optional.of(framework));
        when(frameworkVersionRepository.findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED, 1, 20))
            .thenReturn(pageResult);

        PageResult<FrameworkVersionDto> result = useCase.execute(query);

        assertThat(result.content()).hasSize(1);
        verify(frameworkVersionRepository).findByFrameworkIdAndStatus(frameworkId, FrameworkVersionStatus.PUBLISHED, 1, 20);
    }
}
