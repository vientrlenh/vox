package com.sep.vox.domain.dto;

import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveInteger;

public record FrameworkResultBandDto(
    UUID id,
    UUID frameworkVersionId,
    String code,
    String label,
    String description,
    Integer order
) {

    public static FrameworkResultBandDto toDto(FrameworkResultBand frameworkResultBand) {
        return new FrameworkResultBandDto(
            frameworkResultBand.getId(),
            frameworkResultBand.getFrameworkVersionId(),
            Code.valueOf(frameworkResultBand.getCode()),
            frameworkResultBand.getLabel(),
            frameworkResultBand.getDescription(),
            PositiveInteger.valueOf(frameworkResultBand.getOrder())
        );
    }
}
