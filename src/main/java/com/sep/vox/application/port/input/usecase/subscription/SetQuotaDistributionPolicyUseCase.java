package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.subscription.SetQuotaDistributionPolicyCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;

/**
 * Sets the school's quota distribution ceiling for ONE quota type.
 *
 * <p>Lowering the ceiling below the amount ALREADY distributed is valid and deliberately not rejected: what was
 * distributed is past, and forbidding a lower ceiling would leave the administrator stuck, unable to tighten the
 * policy. The UI shows the excess so they know whom to take back from, and every FURTHER distribution is blocked
 * until the total falls under the ceiling.
 */
@Service
@RequiredArgsConstructor
public class SetQuotaDistributionPolicyUseCase implements IUseCase<SetQuotaDistributionPolicyCommand, BigDecimal> {

    private final SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public BigDecimal execute(SetQuotaDistributionPolicyCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Access denied");
        }

        BigDecimal ratio = input.distributableRatio();
        // The DB has a CHECK keeping this range, but it is checked here so the error comes out as a readable message
        // instead of a DataIntegrityViolationException.
        if (ratio == null || ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("The distribution ceiling must be between 0 and 100 percent");
        }

        return schoolQuotaPolicyRepository
            .upsertRatio(input.schoolId(), input.quotaType(), ratio)
            .getDistributableRatio();
    }
}
