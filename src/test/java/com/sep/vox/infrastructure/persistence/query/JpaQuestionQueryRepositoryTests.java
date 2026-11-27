package com.sep.vox.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.query.dto.QuestionStatusCountInfo;
import com.sep.vox.application.query.repository.QuestionQueryRepository;
import com.sep.vox.support.IntegrationTest;

/**
 * The JPQL of {@code JpaQuestionQueryRepository} is built with
 * {@code em.createQuery(String)} at runtime, NOT {@code @Query} -- so Hibernate does not
 * check it at startup and a syntax error would only show up when someone opens the statistics screen.
 *
 * <p>This class exists to pull that moment forward to build time. It deliberately builds no data: it only
 * needs the query to run to cover the actual risk (syntax, entity names, field names, parameter
 * types). The part "counts exactly who may see what" belongs to the use case tests.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
@Transactional
class JpaQuestionQueryRepositoryTests {

    @Autowired
    private QuestionQueryRepository questionQueryRepository;

    @Test
    void count_accessible_by_status_jpql_should_be_executable() {
        List<QuestionStatusCountInfo> result = questionQueryRepository.countAccessibleByStatus(
            UUID.randomUUID(),
            UUID.randomUUID(),
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            "ALL",
            null
        );

        assertThat(result).isNotNull();
    }

    /** Every combination of parameters must run, including the rarely used scope branches. */
    @Test
    void count_accessible_by_status_should_accept_every_scope_and_filter() {
        for (String scope : new String[] { null, "ALL", "MINE", "COLLABORATING", "REVIEWING" }) {
            List<QuestionStatusCountInfo> result = questionQueryRepository.countAccessibleByStatus(
                UUID.randomUUID(),
                UUID.randomUUID(),
                true,
                false,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "%toán%",
                "READ_ALOUD",
                "SCHOOL_SHARED",
                scope,
                "%abc%"
            );

            assertThat(result).as("scope=%s", scope).isNotNull();
        }
    }
}
