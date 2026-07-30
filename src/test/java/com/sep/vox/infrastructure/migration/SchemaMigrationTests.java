package com.sep.vox.infrastructure.migration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Đối chiếu schema do Flyway dựng với toàn bộ entity mapping.
 *
 * <p>Ba property dưới đây được khai tường minh thay vì dựa vào file cấu hình, vì thứ tự ưu tiên
 * giữa {@code application.yaml} và {@code application-test.yaml} đang không rõ ràng:
 * {@code application-test.yaml} khai {@code create-drop} và {@code H2Dialect}, nhưng thực tế chạy
 * lại là {@code validate} với Postgres dialect (do {@code spring.jpa.properties.*} ở file chính
 * được merge lên trên giá trị suy ra từ {@code database-platform}). Nếu test này chạy với
 * {@code create-drop} thì nó vẫn xanh nhưng mất hoàn toàn tác dụng.
 */
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@ActiveProfiles("test")
@Testcontainers
class SchemaMigrationTests {

    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
    }

    /**
     * Flyway chạy V1..Vn, rồi Hibernate {@code validate} so từng bảng và từng cột với entity. Lệch
     * bất cứ đâu thì context không load được nên test đỏ — assertion chính là việc context khởi
     * động thành công, vì thế body rỗng là đủ.
     */
    @Test
    void flyway_schema_should_match_entity_mappings() {
    }
}
