package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.subscription.ViewSchoolSubscriptionQuotaRecordsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolSubscriptionQuotaRecordsUseCase implements IUseCase<ViewSchoolSubscriptionQuotaRecordsQuery, List<SchoolSubscriptionQuotaRecordDto>> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository schoolSubscriptionQuotaRecordRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<SchoolSubscriptionQuotaRecordDto> execute(ViewSchoolSubscriptionQuotaRecordsQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        SchoolSubscription activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(input.schoolId())
            .orElse(null);
        if (activeSubscription == null) {
            return List.of();
        }

        List<SchoolSubscriptionQuotaRecord> quotas = schoolSubscriptionQuotaRecordRepository.findBySchoolSubscriptionId(activeSubscription.getId());

        return SchoolSubscriptionQuotaRecordDto.toDtoList(quotas);
    }
}
