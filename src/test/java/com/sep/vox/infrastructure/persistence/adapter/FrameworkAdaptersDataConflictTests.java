package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sep.vox.application.exception.DataConflictException;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.valueobject.FrameworkCode;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkCriterionBandJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkCriterionJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkResultBandJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.framework.FrameworkVersionJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkCriterionBandRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkCriterionRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkResultBandRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataFrameworkVersionRepository;

@ExtendWith(MockitoExtension.class)
class FrameworkAdaptersDataConflictTests {

    private static final DataIntegrityViolationException VIOLATION = new DataIntegrityViolationException("unique violation");

    @Mock
    private SpringDataFrameworkRepository springDataFrameworkRepository;

    @Mock
    private SpringDataFrameworkVersionRepository springDataFrameworkVersionRepository;

    @Mock
    private SpringDataFrameworkCriterionRepository springDataFrameworkCriterionRepository;

    @Mock
    private SpringDataFrameworkCriterionBandRepository springDataFrameworkCriterionBandRepository;

    @Mock
    private SpringDataFrameworkResultBandRepository springDataFrameworkResultBandRepository;

    @Test
    void frameworkSave_throwsDataConflictWhenTheDatabaseRejectsTheRow() {
        when(springDataFrameworkRepository.save(org.mockito.ArgumentMatchers.any(FrameworkJpaEntity.class))).thenThrow(VIOLATION);
        FrameworkRepositoryImpl adapter = new FrameworkRepositoryImpl(springDataFrameworkRepository);
        Framework framework = Framework.builder().code(new FrameworkCode("CEFR")).name("CEFR").description("d").build();

        assertThatThrownBy(() -> adapter.save(framework))
            .isInstanceOf(DataConflictException.class)
            .hasCause(VIOLATION);
    }

    @Test
    void frameworkVersionSave_throwsDataConflictWhenTheDatabaseRejectsTheRow() {
        when(springDataFrameworkVersionRepository.save(org.mockito.ArgumentMatchers.any(FrameworkVersionJpaEntity.class)))
            .thenThrow(VIOLATION);
        FrameworkVersionRepositoryImpl adapter = new FrameworkVersionRepositoryImpl(springDataFrameworkVersionRepository);
        FrameworkVersion version = FrameworkVersion.builder().code("V1").status(FrameworkVersionStatus.DRAFT).build();

        assertThatThrownBy(() -> adapter.save(version))
            .isInstanceOf(DataConflictException.class)
            .hasCause(VIOLATION);
    }

    @Test
    void frameworkCriterionSaveAndSaveAll_throwDataConflictWhenTheDatabaseRejectsTheRows() {
        when(springDataFrameworkCriterionRepository.save(org.mockito.ArgumentMatchers.any(FrameworkCriterionJpaEntity.class)))
            .thenThrow(VIOLATION);
        when(springDataFrameworkCriterionRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<FrameworkCriterionJpaEntity>>any()))
            .thenThrow(VIOLATION);
        FrameworkCriterionRepositoryImpl adapter = new FrameworkCriterionRepositoryImpl(springDataFrameworkCriterionRepository);
        FrameworkCriterion criterion = FrameworkCriterion.builder().code("FLUENCY").build();

        assertThatThrownBy(() -> adapter.save(criterion)).isInstanceOf(DataConflictException.class);
        assertThatThrownBy(() -> adapter.saveAll(List.of(criterion))).isInstanceOf(DataConflictException.class);
    }

    @Test
    void frameworkCriterionBandSaveAll_throwsDataConflictWhenTheDatabaseRejectsTheRows() {
        when(springDataFrameworkCriterionBandRepository.saveAll(
            org.mockito.ArgumentMatchers.<Iterable<FrameworkCriterionBandJpaEntity>>any())).thenThrow(VIOLATION);
        FrameworkCriterionBandRepositoryImpl adapter =
            new FrameworkCriterionBandRepositoryImpl(springDataFrameworkCriterionBandRepository);
        FrameworkCriterionBand band = FrameworkCriterionBand.builder().descriptor("d").build();

        assertThatThrownBy(() -> adapter.saveAll(List.of(band))).isInstanceOf(DataConflictException.class);
    }

    @Test
    void frameworkResultBandSaveAndSaveAll_throwDataConflictWhenTheDatabaseRejectsTheRows() {
        when(springDataFrameworkResultBandRepository.save(org.mockito.ArgumentMatchers.any(FrameworkResultBandJpaEntity.class)))
            .thenThrow(VIOLATION);
        when(springDataFrameworkResultBandRepository.saveAll(
            org.mockito.ArgumentMatchers.<Iterable<FrameworkResultBandJpaEntity>>any())).thenThrow(VIOLATION);
        FrameworkResultBandRepositoryImpl adapter = new FrameworkResultBandRepositoryImpl(springDataFrameworkResultBandRepository);
        FrameworkResultBand band = FrameworkResultBand.builder().code("B1").label("Basic").build();

        assertThatThrownBy(() -> adapter.save(band)).isInstanceOf(DataConflictException.class);
        assertThatThrownBy(() -> adapter.saveAll(List.of(band))).isInstanceOf(DataConflictException.class);
    }
}
