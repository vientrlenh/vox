package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.infrastructure.persistence.entity.RubricApplicabilityJpaEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RubricApplicabilityRepositoryTests {

    @Autowired
    private SpringDataRubricApplicabilityRepository repository;

    @Test
    void whenExistsBySchoolClassId_thenReturnsWhetherApplicabilityExists() {
        var schoolClassId = UUID.randomUUID();
        var now = OffsetDateTime.now();
        repository.save(new RubricApplicabilityJpaEntity(
            UUID.randomUUID(),
            schoolClassId,
            null,
            now,
            null,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        ));

        assertThat(repository.existsBySchoolClassId(schoolClassId)).isTrue();
        assertThat(repository.existsBySchoolClassId(UUID.randomUUID())).isFalse();
    }
}
