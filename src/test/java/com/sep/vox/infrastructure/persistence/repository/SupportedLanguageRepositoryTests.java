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

    private static SupportedLanguage newSupportedLanguage(String code, String name) {
        var now = OffsetDateTime.now();
        return new SupportedLanguage(
            new LanguageCode(code),
            name,
            name + " language",
            true,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
