package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolQuotaPolicy;
import com.sep.vox.domain.repository.SchoolQuotaPolicyRepository;
import com.sep.vox.support.IntegrationTest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * The distribution ceiling decides how much a school may hand out, so two properties must hold: the default
 * does NOT change the old behaviour, and a ratio outside 0..1 cannot be written.
 *
 * <p>The multiplication that yields the "distributable share" is covered here too: it is the number the backend uses
 * to reject a request, and rounding the wrong way by a few millionths of a dong is enough to block a distribution
 * that follows the rules.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class SchoolQuotaPolicyTests {

    @Autowired
    private SchoolQuotaPolicyRepository schoolQuotaPolicyRepository;

    @PersistenceContext
    private EntityManager em;

    private final UUID schoolId = UUID.randomUUID();

    @Test
    void should_default_to_fully_distributable_when_the_school_never_set_anything() {
        // No row in the DB. A school that has never touched the configuration screen must see
        // behavior identical to before V5 -- the whole wallet can be distributed.
        SchoolQuotaPolicy policy = schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, QuotaType.ASSESSMENT);

        assertThat(policy.getDistributableRatio()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(policy.distributableAmountOf(new BigDecimal("10000")))
            .isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    void should_take_the_configured_share_of_the_pool() {
        SchoolQuotaPolicy policy = SchoolQuotaPolicy.builder()
            .schoolId(schoolId)
            .quotaType(QuotaType.ASSESSMENT)
            .distributableRatio(new BigDecimal("0.8000"))
            .createdAt(null)
            .updatedAt(null)
            .build();

        // A 10,000 VND wallet, 80% ceiling -> 8,000 VND can be distributed. Exactly the agreed
        // example.
        assertThat(policy.distributableAmountOf(new BigDecimal("10000")))
            .isEqualByComparingTo(new BigDecimal("8000"));
    }

    @Test
    void should_round_the_distributable_share_down_not_up() {
        // Rounding UP would allow distributing a few millionths of a dong more than the ceiling --
        // breaking the very constraint this policy was created to set.
        SchoolQuotaPolicy policy = SchoolQuotaPolicy.builder()
            .schoolId(schoolId)
            .quotaType(QuotaType.ASSESSMENT)
            .distributableRatio(new BigDecimal("0.3333"))
            .createdAt(null)
            .updatedAt(null)
            .build();

        BigDecimal distributable = policy.distributableAmountOf(new BigDecimal("10000.0000005"));

        assertThat(distributable).isLessThanOrEqualTo(new BigDecimal("10000.0000005").multiply(new BigDecimal("0.3333")));
        assertThat(distributable.scale()).isEqualTo(6);
    }

    @Test
    void should_keep_one_policy_per_school_and_quota_type() {
        schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.ASSESSMENT, new BigDecimal("0.8000"));
        schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.ASSESSMENT, new BigDecimal("0.5000"));
        em.flush();

        // The second set OVERWRITES instead of adding a row -- otherwise two rows with the same (school, type) would
        // give a different ceiling depending on which row is read first.
        assertThat(schoolQuotaPolicyRepository.findBySchoolId(schoolId)).hasSize(1);
        assertThat(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, QuotaType.ASSESSMENT)
            .getDistributableRatio()).isEqualByComparingTo(new BigDecimal("0.5000"));
    }

    @Test
    void should_hold_a_separate_policy_for_each_quota_type() {
        // This is the whole reason for splitting by type: keep 30% of the exam wallet while distributing all of the speaking-practice wallet.
        schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.ASSESSMENT, new BigDecimal("0.7000"));
        schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.PRACTICE, BigDecimal.ONE);
        em.flush();

        assertThat(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, QuotaType.ASSESSMENT)
            .getDistributableRatio()).isEqualByComparingTo(new BigDecimal("0.7000"));
        assertThat(schoolQuotaPolicyRepository.findBySchoolIdAndQuotaType(schoolId, QuotaType.PRACTICE)
            .getDistributableRatio()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void should_reject_a_ratio_above_one_at_the_database() {
        // A ratio > 1 would let the school distribute more than the wallet holds -- exactly what this constraint exists
        // to forbid. It is enforced at the database level so a new write path that forgets the check still cannot get through.
        //
        // The exception surfaces at flush(): the id is generated by Hibernate, so upsertRatio() only queues the INSERT.
        assertThatThrownBy(() -> {
            schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.ASSESSMENT, new BigDecimal("1.5000"));
            em.flush();
        })
            .hasMessageContaining("chk_school_quota_policies_ratio_in_range");
    }

    @Test
    void should_reject_a_negative_ratio_at_the_database() {
        assertThatThrownBy(() -> {
            schoolQuotaPolicyRepository.upsertRatio(schoolId, QuotaType.PRACTICE, new BigDecimal("-0.1000"));
            em.flush();
        })
            .hasMessageContaining("chk_school_quota_policies_ratio_in_range");
    }
}
