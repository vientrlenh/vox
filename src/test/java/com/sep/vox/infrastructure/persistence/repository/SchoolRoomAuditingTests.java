package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.infrastructure.persistence.adapter.SchoolRoomRepositoryImpl;
import com.sep.vox.infrastructure.security.CustomUserDetails;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

@RepositoryTest
@Import(SchoolRoomRepositoryImpl.class)
class SchoolRoomAuditingTests {

    private static final String SYSTEM_ACTOR = "System";
    private static final int UUID_VERSION_7 = 7;

    @Autowired
    private SchoolRoomRepository schoolRoomRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenNobodyIsLoggedIn_thenNewRowHasVersionSevenIdEqualTimestampsAndSystemActor() {
        SchoolRoom stored = reload(createRoom("AUDIT_SYSTEM"));

        assertThat(stored.getId().version()).isEqualTo(UUID_VERSION_7);
        assertThat(stored.getCreatedAt()).isNotNull().isEqualTo(stored.getUpdatedAt());
        assertThat(stored.getCreatedBy()).isEqualTo(SYSTEM_ACTOR);
        assertThat(stored.getUpdatedBy()).isEqualTo(SYSTEM_ACTOR);
    }

    @Test
    void whenAUserIsLoggedIn_thenBothActorsAreTheUserIdText() {
        UUID userId = UUID.randomUUID();
        logIn(userId);

        SchoolRoom stored = reload(createRoom("AUDIT_USER"));

        assertThat(stored.getCreatedBy()).isEqualTo(userId.toString());
        assertThat(stored.getUpdatedBy()).isEqualTo(userId.toString());
        assertThat(stored.createdByUserId()).contains(userId);
    }

    @Test
    void whenUpdated_thenCreationValuesStayAndUpdateValuesMove() {
        UUID creatorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        logIn(creatorId);
        SchoolRoom created = reload(createRoom("AUDIT_UPDATE"));

        logIn(editorId);
        created.setName("Renamed");
        schoolRoomRepository.save(created);
        SchoolRoom updated = reload(created);

        assertThat(updated.getName()).isEqualTo("Renamed");
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getCreatedBy()).isEqualTo(creatorId.toString());
        assertThat(updated.getUpdatedAt()).isAfter(created.getUpdatedAt());
        assertThat(updated.getUpdatedBy()).isEqualTo(editorId.toString());
    }

    private SchoolRoom createRoom(String code) {
        return schoolRoomRepository.save(SchoolRoom.builder()
            .schoolId(UUID.randomUUID())
            .code(code)
            .name("Room " + code)
            .active(true)
            .build());
    }

    private SchoolRoom reload(SchoolRoom room) {
        entityManager.flush();
        entityManager.clear();
        return schoolRoomRepository.findById(room.getId()).orElseThrow();
    }

    private static void logIn(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.builder().id(userId).build();
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
