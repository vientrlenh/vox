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
 * Câu JPQL của {@code JpaQuestionQueryRepository} được dựng bằng
 * {@code em.createQuery(String)} lúc chạy, KHÔNG phải {@code @Query} — nên Hibernate không
 * kiểm nó lúc khởi động và một lỗi cú pháp sẽ chỉ lộ ra khi có người mở màn hình thống kê.
 *
 * <p>Lớp này tồn tại để kéo thời điểm đó về lúc build. Nó cố tình không dựng dữ liệu: chỉ
 * cần query chạy được là đã phủ đúng phần rủi ro (cú pháp, tên entity, tên trường, kiểu
 * tham số). Phần "đếm đúng ai được xem gì" thuộc về test của use case.
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
