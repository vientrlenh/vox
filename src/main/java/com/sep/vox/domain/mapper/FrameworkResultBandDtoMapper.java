package com.sep.vox.domain.mapper;

import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.model.framework.FrameworkResultBand;

public final class FrameworkResultBandDtoMapper {

    public static FrameworkResultBandDto toDto(FrameworkResultBand band) {
        return new FrameworkResultBandDto(
            band.getId(),
            band.getFrameworkVersionId(),
            band.getCode(),
            band.getLabel(),
            band.getDescription(),
            band.getOrder()
        );
    }
}
