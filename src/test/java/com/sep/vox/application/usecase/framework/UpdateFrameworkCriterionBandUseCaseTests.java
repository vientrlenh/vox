package com.sep.vox.application.usecase.framework;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sep.vox.application.port.input.command.framework.UpdateFrameworkCriterionBandCommand;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkCriterionBandUseCase;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

@ExtendWith(MockitoExtension.class)
class UpdateFrameworkCriterionBandUseCaseTests {

    private final UUID frameworkId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID criterionId = UUID.randomUUID();
    private final UUID bandId = UUID.randomUUID();
    private final FrameworkCriterionSignals noSignals = new FrameworkCriterionSignals(List.of());
    private final FrameworkCriterionSignals storedSignals = new FrameworkCriterionSignals(List.of(
        new FrameworkCriterionSignal("S1", "Stored signal", FrameworkCriterionSignalImportance.HIGH, null)));

    @Mock
    private FrameworkVersionRepository frameworkVersionRepository;

    @Mock
    private FrameworkCriterionRepository frameworkCriterionRepository;

    @Mock
    private FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    @InjectMocks
    private UpdateFrameworkCriterionBandUseCase useCase;

    private FrameworkCriterionBand storedBand() {
        FrameworkVersion draftVersion = FrameworkVersion.builder()
            .id(versionId).frameworkId(frameworkId).status(FrameworkVersionStatus.DRAFT).build();
        FrameworkCriterion criterion = FrameworkCriterion.builder()
            .id(criterionId).frameworkVersionId(versionId).build();
        FrameworkCriterionBand band = FrameworkCriterionBand.builder()
            .id(bandId).frameworkCriterionId(criterionId).descriptor("Stored descriptor")
            .positiveSignals(storedSignals).build();
        when(frameworkVersionRepository.findById(versionId)).thenReturn(Optional.of(draftVersion));
        when(frameworkCriterionRepository.findById(criterionId)).thenReturn(Optional.of(criterion));
        when(frameworkCriterionBandRepository.findById(bandId)).thenReturn(Optional.of(band));
        return band;
    }

    private UpdateFrameworkCriterionBandCommand command(String descriptor, boolean descriptorProvided) {
        return new UpdateFrameworkCriterionBandCommand(
            frameworkId, versionId, criterionId, bandId, descriptor, descriptorProvided, noSignals, noSignals);
    }

    @Test
    void execute_keepsTheDescriptorWhenItIsNotProvided() {
        FrameworkCriterionBand band = storedBand();

        useCase.execute(command(null, false));

        assertThat(band.getDescriptor()).isEqualTo("Stored descriptor");
    }

    @Test
    void execute_clearsTheDescriptorWhenNullIsProvided() {
        FrameworkCriterionBand band = storedBand();

        useCase.execute(command(null, true));

        assertThat(band.getDescriptor()).isNull();
    }

    @Test
    void execute_keepsTheDescriptorWhenAnEmptyTextIsProvided() {
        FrameworkCriterionBand band = storedBand();

        useCase.execute(command("", true));

        assertThat(band.getDescriptor()).isEqualTo("Stored descriptor");
    }

    @Test
    void execute_storesTheNormalizedDescriptorWhenATextIsProvided() {
        FrameworkCriterionBand band = storedBand();

        useCase.execute(command("  New   text ", true));

        assertThat(band.getDescriptor()).isEqualTo("New text");
    }

    @Test
    void execute_keepsTheSignalsWhenTheListsAreEmpty() {
        FrameworkCriterionBand band = storedBand();

        useCase.execute(command(null, false));

        assertThat(band.getPositiveSignals()).isEqualTo(storedSignals);
    }
}
