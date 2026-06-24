package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionDetailsQuery;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionDetailsUseCase;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

public class ViewFrameworkVersionDetailsUseCaseTests {

    private FrameworkVersionRepository frameworkVersionRepository;
    private ViewFrameworkVersionDetailsUseCase useCase;
    private OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        frameworkVersionRepository = mock(FrameworkVersionRepository.class);
        useCase = new ViewFrameworkVersionDetailsUseCase(frameworkVersionRepository);
    }

    @Test
    void should_return_version_details() {
        var versionId = UUID.randomUUID();
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkVersionDetailsQuery(versionId);

        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setCode("V1_0");
        version.setName("Version 1.0");
        version.setVersion(1);
        version.setStatus(FrameworkVersionStatus.PUBLISHED);
        version.setEffectiveFrom(now);
        version.setEffectiveTo(now.plusDays(365));

        when(frameworkVersionRepository.findById(versionId)).thenReturn(Optional.of(version));

        var result = useCase.execute(query);

        assertThat(result.id()).isEqualTo(versionId);
        assertThat(result.code()).isEqualTo("V1_0");
        assertThat(result.status()).isEqualTo(FrameworkVersionStatus.PUBLISHED.toString());
        assertThat(result.criteria()).isEmpty();
        assertThat(result.resultBands()).isEmpty();
        verify(frameworkVersionRepository).findById(versionId);
    }

    @Test
    void should_throw_not_found_when_version_missing() {
        var versionId = UUID.randomUUID();
        var query = new ViewFrameworkVersionDetailsQuery(versionId);

        when(frameworkVersionRepository.findById(versionId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(query));
    }

    @Test
    void should_return_shallow_dto_with_empty_nested_lists() {
        var versionId = UUID.randomUUID();
        var frameworkId = UUID.randomUUID();
        var query = new ViewFrameworkVersionDetailsQuery(versionId);

        var version = new FrameworkVersion();
        version.setId(versionId);
        version.setFrameworkId(frameworkId);
        version.setCode("V1_0");
        version.setName("Empty Version");
        version.setStatus(FrameworkVersionStatus.DRAFT);

        when(frameworkVersionRepository.findById(versionId)).thenReturn(Optional.of(version));

        var result = useCase.execute(query);

        assertThat(result.id()).isEqualTo(versionId);
        assertThat(result.criteria()).isEqualTo(List.of());
        assertThat(result.resultBands()).isEqualTo(List.of());
    }
}
