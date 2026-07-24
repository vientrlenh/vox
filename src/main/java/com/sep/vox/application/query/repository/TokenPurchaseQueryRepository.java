package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.TokenPurchaseDto;

public interface TokenPurchaseQueryRepository {
    List<TokenPurchaseDto> findAllByActiveSchoolSubscription(UUID schoolId);
}
