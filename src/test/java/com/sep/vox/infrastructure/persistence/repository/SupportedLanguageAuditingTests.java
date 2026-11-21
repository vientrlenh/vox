package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;
import com.sep.vox.infrastructure.persistence.adapter.SupportedLanguageRepositoryImpl;
import com.sep.vox.infrastructure.security.CustomUserDetails;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

@RepositoryTest
@Import(SupportedLanguageRepositoryImpl.class)
class SupportedLanguageAuditingTests {

    private static final String SYSTEM_ACTOR = "System";
    private static final int UUID_VERSION_7 = 7;

    @Autowired
    private SupportedLanguageRepository supportedLanguageRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenNobodyIsLoggedIn_thenNewRowHasVersionSevenIdEqualTimestampsAndSystemActor() {
        SupportedLanguage saved = createLanguage("AU");

        SupportedLanguage stored = reload(saved);
        assertThat(stored.getId().version()).isEqualTo(UUID_VERSION_7);
        assertThat(stored.getCreatedAt()).isNotNull().isEqualTo(stored.getUpdatedAt());
        assertThat(stored.getCreatedBy()).isEqualTo(SYSTEM_ACTOR);
        assertThat(stored.getUpdatedBy()).isEqualTo(SYSTEM_ACTOR);
    }

    @Test
    void whenAUserIsLoggedIn_thenBothActorsAreTheUserIdText() {
        UUID userId = UUID.randomUUID();
        logIn(userId);

        SupportedLanguage stored = reload(createLanguage("AV"));

        assertThat(stored.getCreatedBy()).isEqualTo(userId.toString());
        assertThat(stored.getUpdatedBy()).isEqualTo(userId.toString());
        assertThat(stored.createdByUserId()).contains(userId);
    }

    @Test
    void whenUpdated_thenCreationValuesStayAndUpdateValuesMove() {
        UUID creatorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        logIn(creatorId);
        SupportedLanguage created = reload(createLanguage("AW"));

        logIn(editorId);
        created.setName("Renamed");
        supportedLanguageRepository.save(created);
        SupportedLanguage updated = reload(created);

        assertThat(updated.getName()).isEqualTo("Renamed");
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getCreatedBy()).isEqualTo(creatorId.toString());
        assertThat(updated.getUpdatedAt()).isAfter(created.getUpdatedAt());
        assertThat(updated.getUpdatedBy()).isEqualTo(editorId.toString());
    }

    @Test
    void whenSaveReturns_thenReportsWhetherTheReturnedObjectAlreadyShowsTheNewUpdatedAt() {
        SupportedLanguage created = reload(createLanguage("AX"));
        Instant before = created.getUpdatedAt();
        created.setName("Renamed");

        SupportedLanguage returned = supportedLanguageRepository.save(created);

        System.out.println("FLUSH-TIMING: save() returned updatedAt moved = " + returned.getUpdatedAt().isAfter(before));
        entityManager.flush();
        assertThat(reload(created).getUpdatedAt()).isAfter(before);
    }

    private SupportedLanguage createLanguage(String code) {
        return supportedLanguageRepository.save(SupportedLanguage.builder()
            .code(new LanguageCode(code))
            .name("Language " + code)
            .active(true)
            .build());
    }

    private SupportedLanguage reload(SupportedLanguage language) {
        entityManager.flush();
        entityManager.clear();
        return supportedLanguageRepository.findById(language.getId()).orElseThrow();
    }

    private static void logIn(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.builder().id(userId).build();
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
