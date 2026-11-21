package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;
import com.sep.vox.infrastructure.persistence.adapter.SupportedLanguageRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

@RepositoryTest
@Import({
    SupportedLanguageRepositoryImpl.class
})
class SupportedLanguageRepositoryTests {

    @Autowired
    private SupportedLanguageRepository supportedLanguageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void whenSave_thenReturnsPersistedSupportedLanguage() {
        SupportedLanguage language = newSupportedLanguage("EN", "English");

        SupportedLanguage saved = supportedLanguageRepository.save(language);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode().value()).isEqualTo("EN");
        assertThat(saved.getName()).isEqualTo("English");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void whenFindById_thenReturnsSupportedLanguage() {
        SupportedLanguage saved = supportedLanguageRepository.save(newSupportedLanguage("FR", "French"));

        Optional<SupportedLanguage> found = supportedLanguageRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCode().value()).isEqualTo("FR");
    }

    @Test
    void whenFindByCode_thenReturnsSupportedLanguage() {
        supportedLanguageRepository.save(newSupportedLanguage("JP", "Japanese"));

        Optional<SupportedLanguage> found = supportedLanguageRepository.findByCode("JP");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Japanese");
    }

    @Test
    void whenFindByCodeIn_thenReturnsMatchingSupportedLanguages() {
        supportedLanguageRepository.save(newSupportedLanguage("KO", "Korean"));
        supportedLanguageRepository.save(newSupportedLanguage("IT", "Italian"));

        List<SupportedLanguage> found = supportedLanguageRepository.findByCodeIn(Set.of("KO", "IT", "MISSING"));

        assertThat(found)
            .hasSize(2)
            .extracting(language -> language.getCode().value())
            .containsExactlyInAnyOrder("KO", "IT");
    }

    @Test
    void whenCount_thenReturnsNumberOfSupportedLanguages() {
        long before = supportedLanguageRepository.count();
        supportedLanguageRepository.save(newSupportedLanguage("DE", "German"));

        assertThat(supportedLanguageRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void whenFindAllWithSearch_thenMatchesCodeOrNameCaseInsensitive() {
        supportedLanguageRepository.save(newSupportedLanguage("PT", "Portuguese"));
        supportedLanguageRepository.save(newSupportedLanguage("NL", "Dutch"));

        PageResult<SupportedLanguage> result = supportedLanguageRepository.findAll("port", null, 1, 20);

        assertThat(result.content())
            .extracting(language -> language.getCode().value())
            .contains("PT")
            .doesNotContain("NL");
    }

    @Test
    void whenFindAllWithActiveFilter_thenReturnsMatchingLanguages() {
        supportedLanguageRepository.save(newSupportedLanguage("TR", "Turkish", true));
        supportedLanguageRepository.save(newSupportedLanguage("PL", "Polish", false));

        PageResult<SupportedLanguage> active = supportedLanguageRepository.findAll(null, true, 1, 20);
        PageResult<SupportedLanguage> inactive = supportedLanguageRepository.findAll(null, false, 1, 20);

        assertThat(active.content())
            .extracting(language -> language.getCode().value())
            .contains("TR")
            .doesNotContain("PL");
        assertThat(inactive.content())
            .extracting(language -> language.getCode().value())
            .contains("PL")
            .doesNotContain("TR");
    }

    @Test
    void whenFindAll_thenReturnsRequestedPageSortedByCreatedAtDescending() {
        SupportedLanguage older = supportedLanguageRepository.save(newSupportedLanguage("SV", "Swedish", true));
        entityManager.flush();
        entityManager.createNativeQuery(
                "update supported_languages set created_at = created_at - interval '1 second' where id = :id")
            .setParameter("id", older.getId())
            .executeUpdate();
        supportedLanguageRepository.save(newSupportedLanguage("DA", "Danish", true));

        PageResult<SupportedLanguage> result = supportedLanguageRepository.findAll(null, null, 1, 1);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getCode().value()).isEqualTo("DA");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(2);
        assertThat(result.totalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void whenUpdateMutableFields_thenOnlyProvidedFieldsAreUpdated() {
        SupportedLanguage saved = supportedLanguageRepository.save(newSupportedLanguage("RU", "Russian", true));
        Instant updatedAt = Instant.parse("2026-06-10T10:00:00Z");
        String updatedBy = UUID.randomUUID().toString();

        int updatedRows = supportedLanguageRepository.updateMutableFields(
            saved.getId(),
            "ES",
            true,
            null,
            false,
            "Spanish language",
            true,
            false,
            true,
            updatedAt,
            updatedBy
        );

        SupportedLanguage updated = supportedLanguageRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedRows).isEqualTo(1);
        assertThat(updated.getCode().value()).isEqualTo("ES");
        assertThat(updated.getName()).isEqualTo("Russian");
        assertThat(updated.getDescription()).isEqualTo("Spanish language");
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(updated.getUpdatedBy()).isEqualTo(updatedBy);
    }

    @Test
    void whenUpdateMutableFieldsWithNullDescription_thenClearsDescription() {
        SupportedLanguage saved = supportedLanguageRepository.save(newSupportedLanguage("VI", "Vietnamese", true));

        int updatedRows = supportedLanguageRepository.updateMutableFields(
            saved.getId(),
            null,
            false,
            null,
            false,
            null,
            true,
            null,
            false,
            Instant.now(),
            UUID.randomUUID().toString()
        );

        SupportedLanguage updated = supportedLanguageRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedRows).isEqualTo(1);
        assertThat(updated.getDescription()).isNull();
    }

    @Test
    void whenUpdateMutableFieldsWithMissingId_thenReturnsZero() {
        int updatedRows = supportedLanguageRepository.updateMutableFields(
            UUID.randomUUID(),
            "ID",
            true,
            "Indonesian",
            true,
            "Indonesian language",
            true,
            true,
            true,
            Instant.now(),
            UUID.randomUUID().toString()
        );

        assertThat(updatedRows).isZero();
    }

    private static SupportedLanguage newSupportedLanguage(String code, String name) {
        return newSupportedLanguage(code, name, true);
    }

    private static SupportedLanguage newSupportedLanguage(String code, String name, boolean active) {
        return SupportedLanguage.builder()
            .code(new LanguageCode(code))
            .name(name)
            .description(name + " language")
            .active(active)
            .build();
    }
}
