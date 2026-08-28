package com.sep.vox.application.usecase.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguagesUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class ViewSupportedLanguagesUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private UserContextPort userContextPort;
    private ViewSupportedLanguagesUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSupportedLanguagesUseCase(supportedLanguageRepository, userContextPort);
    }

    @Test
    void view_should_return_languages_with_normalized_search_and_filter() {
        when(userContextPort.isSystemAdmin()).thenReturn(true);
        var language = language("EN", "English", true);
        var page = new PageResult<>(List.of(language), 1, 20, 1, 1);
        when(supportedLanguageRepository.findAll("English Language", true, 1, 20)).thenReturn(page);

        var result = useCase.execute(new ViewSupportedLanguagesQuery(1, 20, "  English   Language  ", true));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).code()).isEqualTo("EN");
        assertThat(result.totalElements()).isEqualTo(1);
        verify(supportedLanguageRepository).findAll("English Language", true, 1, 20);
    }

    private static SupportedLanguage language(String code, String name, boolean active) {
        var now = Instant.now();
        return new SupportedLanguage(
            UUID.randomUUID(),
            new LanguageCode(code),
            name,
            null,
            active,
            now,
            now,
            UUID.randomUUID(),
            UUID.randomUUID()
        );
    }
}
