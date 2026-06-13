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

import com.sep.vox.config.TestContainerConfig;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;
import com.sep.vox.infrastructure.persistence.adapter.SupportedLanguageRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    SupportedLanguageRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SupportedLanguageRepositoryTests {

    @Autowired
    private SupportedLanguageRepository supportedLanguageRepository;

    @Test
    void whenSave_thenReturnsPersistedSupportedLanguage() {
        var language = newSupportedLanguage("EN", "English");

        var saved = supportedLanguageRepository.save(language);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCode().value()).isEqualTo("EN");
        assertThat(saved.getName()).isEqualTo("English");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void whenFindById_thenReturnsSupportedLanguage() {
        var saved = supportedLanguageRepository.save(newSupportedLanguage("FR", "French"));

        var found = supportedLanguageRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCode().value()).isEqualTo("FR");
    }

    @Test
    void whenFindByCode_thenReturnsSupportedLanguage() {
        supportedLanguageRepository.save(newSupportedLanguage("JP", "Japanese"));

        var found = supportedLanguageRepository.findByCode("JP");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Japanese");
    }

    @Test
    void whenFindByCodeIn_thenReturnsMatchingSupportedLanguages() {
        supportedLanguageRepository.save(newSupportedLanguage("KO", "Korean"));
        supportedLanguageRepository.save(newSupportedLanguage("IT", "Italian"));

        var found = supportedLanguageRepository.findByCodeIn(Set.of("KO", "IT", "MISSING"));

        assertThat(found)
            .hasSize(2)
            .extracting(language -> language.getCode().value())
            .containsExactlyInAnyOrder("KO", "IT");
    }

    @Test
    void whenCount_thenReturnsNumberOfSupportedLanguages() {
        var before = supportedLanguageRepository.count();
        supportedLanguageRepository.save(newSupportedLanguage("DE", "German"));

        assertThat(supportedLanguageRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void whenFindAllWithSearch_thenMatchesCodeOrNameCaseInsensitive() {
        supportedLanguageRepository.save(newSupportedLanguage("PT", "Portuguese"));
        supportedLanguageRepository.save(newSupportedLanguage("NL", "Dutch"));

        var result = supportedLanguageRepository.findAll("port", null, new PageRequest(1, 20));

        assertThat(result.content())
            .extracting(language -> language.getCode().value())
            .contains("PT")
            .doesNotContain("NL");
    }

    @Test
    void whenFindAllWithActiveFilter_thenReturnsMatchingLanguages() {
        supportedLanguageRepository.save(newSupportedLanguage("TR", "Turkish", true));
        supportedLanguageRepository.save(newSupportedLanguage("PL", "Polish", false));

        var active = supportedLanguageRepository.findAll(null, true, new PageRequest(1, 20));
        var inactive = supportedLanguageRepository.findAll(null, false, new PageRequest(1, 20));

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
        supportedLanguageRepository.save(newSupportedLanguage("SV", "Swedish", true, OffsetDateTime.parse("2026-06-01T10:00:00Z")));
        supportedLanguageRepository.save(newSupportedLanguage("DA", "Danish", true, OffsetDateTime.parse("2026-06-02T10:00:00Z")));

        var result = supportedLanguageRepository.findAll(null, null, new PageRequest(1, 1));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getCode().value()).isEqualTo("DA");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalElements()).isGreaterThanOrEqualTo(2);
        assertThat(result.totalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void whenUpdateMutableFields_thenOnlyProvidedFieldsAreUpdated() {
        var saved = supportedLanguageRepository.save(newSupportedLanguage("RU", "Russian", true));
        var updatedAt = OffsetDateTime.parse("2026-06-10T10:00:00Z");
        var updatedBy = UUID.randomUUID();

        var updatedRows = supportedLanguageRepository.updateMutableFields(
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

        var updated = supportedLanguageRepository.findById(saved.getId()).orElseThrow();
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
        var saved = supportedLanguageRepository.save(newSupportedLanguage("VI", "Vietnamese", true));

        var updatedRows = supportedLanguageRepository.updateMutableFields(
            saved.getId(),
            null,
            false,
            null,
            false,
            null,
            true,
            null,
            false,
            OffsetDateTime.now(),
            UUID.randomUUID()
        );

        var updated = supportedLanguageRepository.findById(saved.getId()).orElseThrow();
        assertThat(updatedRows).isEqualTo(1);
        assertThat(updated.getDescription()).isNull();
    }

    @Test
    void whenUpdateMutableFieldsWithMissingId_thenReturnsZero() {
        var updatedRows = supportedLanguageRepository.updateMutableFields(
            UUID.randomUUID(),
            "ID",
            true,
            "Indonesian",
            true,
            "Indonesian language",
            true,
            true,
            true,
            OffsetDateTime.now(),
            UUID.randomUUID()
        );

        assertThat(updatedRows).isZero();
    }

    private static SupportedLanguage newSupportedLanguage(String code, String name) {
        return newSupportedLanguage(code, name, true);
    }

    private static SupportedLanguage newSupportedLanguage(String code, String name, boolean active) {
        return newSupportedLanguage(code, name, active, OffsetDateTime.now());
    }

    private static SupportedLanguage newSupportedLanguage(String code, String name, boolean active, OffsetDateTime now) {
        return new SupportedLanguage(
            new LanguageCode(code),
            name,
            name + " language",
            active,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
