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
import com.sep.vox.domain.model.languagelevel.LanguageLevel;
import com.sep.vox.domain.repository.LanguageLevelRepository;
import com.sep.vox.domain.valueobject.LanguageRank;
import com.sep.vox.domain.valueobject.LevelCode;
import com.sep.vox.infrastructure.persistence.adapter.LanguageLevelRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    TestContainerConfig.class,
    LanguageLevelRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LanguageLevelRepositoryTests {

    @Autowired
    private LanguageLevelRepository languageLevelRepository;

    @Test
    void whenSave_thenReturnsPersistedLanguageLevel() {
        var languageLevel = newLanguageLevel("A1", "Beginner", 1);

        var saved = languageLevelRepository.save(languageLevel);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSchoolId()).isEqualTo(languageLevel.getSchoolId());
        assertThat(saved.getLanguageId()).isEqualTo(languageLevel.getLanguageId());
        assertThat(saved.getCode().value()).isEqualTo("A1");
        assertThat(saved.getName()).isEqualTo("Beginner");
        assertThat(saved.getRank().value()).isEqualTo(1);
    }

    @Test
    void whenFindById_thenReturnsLanguageLevel() {
        var saved = languageLevelRepository.save(newLanguageLevel("B1", "Intermediate", 3));

        var found = languageLevelRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCode().value()).isEqualTo("B1");
        assertThat(found.get().getName()).isEqualTo("Intermediate");
    }

    private static LanguageLevel newLanguageLevel(String code, String name, int rank) {
        var now = OffsetDateTime.now();
        return new LanguageLevel(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new LevelCode(code),
            name,
            new LanguageRank(rank),
            true,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
