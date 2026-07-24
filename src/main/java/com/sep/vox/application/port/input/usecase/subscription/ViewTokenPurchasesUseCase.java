package com.sep.vox.application.port.input.usecase.subscription;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewTokenPurchasesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.TokenPurchaseQueryRepository;
import com.sep.vox.domain.dto.TokenPurchaseDto;

@Service
public class ViewTokenPurchasesUseCase implements IUseCase<ViewTokenPurchasesQuery, List<TokenPurchaseDto>> {

    private final TokenPurchaseQueryRepository tokenPurchaseQueryRepository;
    private final UserContextPort userContextPort;

    public ViewTokenPurchasesUseCase(TokenPurchaseQueryRepository tokenPurchaseQueryRepository, UserContextPort userContextPort) {
        this.tokenPurchaseQueryRepository = tokenPurchaseQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenPurchaseDto> execute(ViewTokenPurchasesQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        return tokenPurchaseQueryRepository.findAllByActiveSchoolSubscription(input.schoolId());
    }
}
