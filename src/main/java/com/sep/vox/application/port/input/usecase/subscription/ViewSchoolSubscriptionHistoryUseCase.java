package com.sep.vox.application.port.input.usecase.subscription;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionHistoryQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
public class ViewSchoolSubscriptionHistoryUseCase implements IUseCase<ViewSchoolSubscriptionHistoryQuery, List<SchoolSubscriptionDto>> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolSubscriptionHistoryUseCase(SchoolSubscriptionRepository schoolSubscriptionRepository, UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public List<SchoolSubscriptionDto> execute(ViewSchoolSubscriptionHistoryQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return schoolSubscriptionRepository.findBySchoolId(input.schoolId()).stream()
            .sorted(Comparator.comparing((SchoolSubscription subscription) -> subscription.getCreatedAt()).reversed())
            .map(SchoolSubscriptionDto::toDto)
            .toList();
    }
}
