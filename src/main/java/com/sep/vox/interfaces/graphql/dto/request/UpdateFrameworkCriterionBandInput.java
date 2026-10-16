package com.sep.vox.interfaces.graphql.dto.request;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkCriterionBandCommand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public record UpdateFrameworkCriterionBandInput(
    String descriptor,
    List<SignalInput> positiveSignals,
    List<SignalInput> negativeSignals
) {

    public static UpdateFrameworkCriterionBandCommand toCommand(UUID frameworkId, UUID versionId, UUID criterionId, UUID bandId, UpdateFrameworkCriterionBandInput input) {
        return new UpdateFrameworkCriterionBandCommand(
                frameworkId, versionId, criterionId, bandId,
                input.descriptor(),
                toSignals(input.positiveSignals()),
                toSignals(input.negativeSignals()));
    }

    private static FrameworkCriterionSignals toSignals(List<SignalInput> raw) {
        if (raw == null) return new FrameworkCriterionSignals(List.of());
        return new FrameworkCriterionSignals(raw.stream()
                .map(s -> new FrameworkCriterionSignal(
                        s.code(),
                        s.description(),
                        FrameworkCriterionSignalImportance.valueOf(s.importance()),
                        s.evidenceHint()))
                .toList());
    }
}
