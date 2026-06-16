package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolUserRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    SchoolUserRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SchoolUserRepositoryTests extends ContainerTestConfig {

    @Autowired
    private SchoolUserRepository schoolUserRepository;

    @Test
    void whenFindByUserIdIn_thenReturnsMatchingSchoolUsers() {
        var schoolId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var anotherUserId = UUID.randomUUID();
        schoolUserRepository.save(new SchoolUser(schoolId, userId, OffsetDateTime.now(), null));
        schoolUserRepository.save(new SchoolUser(schoolId, anotherUserId, OffsetDateTime.now(), null));
        schoolUserRepository.save(new SchoolUser(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now(), null));

        var found = schoolUserRepository.findByUserIdIn(Set.of(userId, anotherUserId, UUID.randomUUID()));

        assertThat(found)
            .hasSize(2)
            .extracting(SchoolUser::getUserId)
            .containsExactlyInAnyOrder(userId, anotherUserId);
    }
}
