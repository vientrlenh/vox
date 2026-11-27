package com.sep.vox.application.port.input.usecase.dashboard;

import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;
import com.sep.vox.domain.common.PageResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.dashboard.ViewSchoolsAtRiskQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;
import com.sep.vox.application.query.repository.SchoolsAtRiskQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolsAtRiskResponse;
import com.sep.vox.domain.common.BusinessConstant;

/**
 * The "schools needing attention" page -- the landing place of the four rows on the card of the same name on the
 * system overview page.
 *
 * <p>ONE page with four filters, not four pages: the four groups only differ in the predicate over the same table.
 *
 * <p>The counts come from the SAME {@link PlatformBusinessHealthQueryRepository} that the overview page uses, and
 * are not counted again with a different query. This is the most fragile point of the screen: the card says 5, the
 * user clicks it, and a different filter would give 9 with nobody knowing which is right.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolsAtRiskUseCase implements IUseCase<ViewSchoolsAtRiskQuery, SchoolsAtRiskResponse> {

    private final PlatformBusinessHealthQueryRepository platformBusinessHealthQueryRepository;
    private final SchoolsAtRiskQueryRepository schoolsAtRiskQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolsAtRiskResponse execute(ViewSchoolsAtRiskQuery input) {
        // ONE "now" for both the count and the list. Calling Instant.now() twice would let a period that expires
        // exactly between the two calls make the card and the table right below it differ by one school, and
        // there would be no way to reproduce it.
        Instant now = Instant.now();
        Instant expiringThrough = now.plus(BusinessConstant.DASHBOARD_EXPIRING_SOON_DAYS, ChronoUnit.DAYS);

        SchoolSubscriptionHealthDto health = platformBusinessHealthQueryRepository.countSchoolSubscriptionHealth(now, expiringThrough);
        SchoolsAtRiskResponse.BucketCounts counts = new SchoolsAtRiskResponse.BucketCounts(
            health.expiringSoonSchools(),
            health.lapsedSchools(),
            health.suspendedSchools(),
            platformBusinessHealthQueryRepository.countSchoolsInDebt()
        );

        PageResult<SchoolAtRiskDto> schools = schoolsAtRiskQueryRepository.findByBucket(
            input.bucket(), now, expiringThrough, input.keyword(), input.page(), input.size());

        return new SchoolsAtRiskResponse(input.bucket(), counts, schools);
    }
}
