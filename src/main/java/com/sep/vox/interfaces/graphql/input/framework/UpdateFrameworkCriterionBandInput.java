package com.sep.vox.interfaces.graphql.input.framework;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.ArgumentValue;
import com.sep.vox.application.port.input.command.framework.UpdateFrameworkCriterionBandCommand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public record UpdateFrameworkCriterionBandInput(
    ArgumentValue<String> descriptor,
    ArgumentValue<List<SignalInput>> positiveSignals,
    ArgumentValue<List<SignalInput>> negativeSignals
) {

    public static UpdateFrameworkCriterionBandCommand toCommand(
            UUID frameworkId, UUID versionId, UUID criterionId, UUID bandId,
            UpdateFrameworkCriterionBandInput input) {
        return new UpdateFrameworkCriterionBandCommand(
                frameworkId, versionId, criterionId, bandId,
                input.descriptor().value(), !input.descriptor().isOmitted(),
                toSignals(input.positiveSignals().value()),
                toSignals(input.negativeSignals().value()));
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
