package com.sep.vox.interfaces.graphql.mapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

public final class UpdateFrameworkVersionCommandMapper {

    private UpdateFrameworkVersionCommandMapper() {
    }

    public static UpdateFrameworkVersionCommand fromInput(UUID frameworkId, UUID versionId, UpdateFrameworkVersionInput input) {
        if (input.criteria() != null && input.criteria().size() > 100)
            throw new IllegalArgumentException("Không được có quá 100 tiêu chí");
        if (input.resultBands() != null && input.resultBands().size() > 50)
            throw new IllegalArgumentException("Không được có quá 50 dải kết quả");

        List<UpdateFrameworkVersionCommand.CriterionInput> criteria = input.criteria() == null ? null
            : input.criteria().stream().map(UpdateFrameworkVersionCommandMapper::toCriterionInput).toList();

        List<UpdateFrameworkVersionCommand.ResultBandInput> resultBands = input.resultBands() == null ? null
            : input.resultBands().stream().map(UpdateFrameworkVersionCommandMapper::toResultBandInput).toList();

        var effectiveFrom = parseDateTime(input.effectiveFrom());
        var effectiveTo = parseDateTime(input.effectiveTo());
        if (effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom))
            throw new IllegalArgumentException("Ngày kết thúc hiệu lực phải sau ngày bắt đầu hiệu lực");

        return new UpdateFrameworkVersionCommand(
            frameworkId,
            versionId,
            input.code(),
            input.name(),
            input.description(),
            effectiveFrom,
            effectiveTo,
            criteria,
            resultBands
        );
    }

    private static UpdateFrameworkVersionCommand.CriterionInput toCriterionInput(UpdateFrameworkVersionInput.CriterionInput c) {
        List<UpdateFrameworkVersionCommand.CriterionBandInput> bands = c.bands() == null ? null
            : c.bands().stream().map(UpdateFrameworkVersionCommandMapper::toCriterionBandInput).toList();
        return new UpdateFrameworkVersionCommand.CriterionInput(
            c.code(),
            c.name(),
            c.description(),
            bands
        );
    }

    private static UpdateFrameworkVersionCommand.CriterionBandInput toCriterionBandInput(UpdateFrameworkVersionInput.CriterionBandInput b) {
        return new UpdateFrameworkVersionCommand.CriterionBandInput(
            b.resultBandCode(),
            b.descriptor(),
            toSignals(b.positiveSignals()),
            toSignals(b.negativeSignals())
        );
    }

    private static FrameworkCriterionSignals toSignals(List<UpdateFrameworkVersionInput.SignalInput> raw) {
        if (raw == null) return new FrameworkCriterionSignals(List.of());
        return new FrameworkCriterionSignals(raw.stream()
            .map(s -> new FrameworkCriterionSignal(
                s.code(),
                s.description(),
                parseImportance(s.importance()),
                s.evidenceHint()))
            .toList());
    }

    private static UpdateFrameworkVersionCommand.ResultBandInput toResultBandInput(UpdateFrameworkVersionInput.ResultBandInput r) {
        if (r.scoreMin() != null && r.scoreMax() != null && r.scoreMin() > r.scoreMax())
            throw new IllegalArgumentException("Điểm tối thiểu không được lớn hơn điểm tối đa của kết quả: " + r.code());
        return new UpdateFrameworkVersionCommand.ResultBandInput(
            r.code(),
            r.label(),
            r.description(),
            parseBigDecimal(r.scoreMin()),
            parseBigDecimal(r.scoreMax()),
            r.order()
        );
    }

    private static FrameworkCriterionSignalImportance parseImportance(String value) {
        try {
            return FrameworkCriterionSignalImportance.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Giá trị độ quan trọng không hợp lệ: " + value
                + ". Các giá trị hợp lệ: HIGH, MEDIUM, LOW");
        }
    }

    private static OffsetDateTime parseDateTime(String value) {
        return value == null ? null : OffsetDateTime.parse(value);
    }

    private static BigDecimal parseBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
