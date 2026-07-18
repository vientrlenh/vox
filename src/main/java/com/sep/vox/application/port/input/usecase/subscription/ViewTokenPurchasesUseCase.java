package com.sep.vox.application.port.input.usecase.subscription;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewTokenPurchasesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.TokenPurchaseDto;
import com.sep.vox.domain.mapper.TokenPurchaseDtoMapper;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

@Service
public class ViewTokenPurchasesUseCase implements IUseCase<ViewTokenPurchasesQuery, List<TokenPurchaseDto>> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final TokenPurchaseRepository tokenPurchaseRepository;
    private final TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private final UserContextPort userContextPort;

    public ViewTokenPurchasesUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            TokenPurchaseRepository tokenPurchaseRepository,
            TokenPurchaseItemRepository tokenPurchaseItemRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.tokenPurchaseRepository = tokenPurchaseRepository;
        this.tokenPurchaseItemRepository = tokenPurchaseItemRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenPurchaseDto> execute(ViewTokenPurchasesQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(input.schoolId());
        if (activeSubscription.isEmpty()) {
            return List.of();
        }

        return tokenPurchaseRepository.findAllBySubscriptionId(activeSubscription.get().getId()).stream()
            .map(purchase -> TokenPurchaseDtoMapper.toDto(purchase, tokenPurchaseItemRepository.findAllByPurchaseId(purchase.getId())))
            .toList();
    }
}
