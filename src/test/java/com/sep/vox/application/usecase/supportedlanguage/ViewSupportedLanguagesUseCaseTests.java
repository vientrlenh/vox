package com.sep.vox.application.usecase.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.ViewSupportedLanguagesQuery;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguagesUseCase;
import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class ViewSupportedLanguagesUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private ViewSupportedLanguagesUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        useCase = new ViewSupportedLanguagesUseCase(supportedLanguageRepository);
    }

    @Test
    void view_should_return_languages_with_normalized_search_and_filter() {
        var language = language("EN", "English", true);
        var page = new PageResult<>(List.of(language), 1, 20, 1, 1);
        when(supportedLanguageRepository.findAll("English Language", true, new PageRequest(1, 20))).thenReturn(page);

        var result = useCase.execute(new ViewSupportedLanguagesQuery(1, 20, "  English   Language  ", true));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).code()).isEqualTo("EN");
        assertThat(result.totalElements()).isEqualTo(1);
        verify(supportedLanguageRepository).findAll("English Language", true, new PageRequest(1, 20));
    }

    @Test
    void view_should_throw_when_page_or_size_invalid() {
        var pageException = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> useCase.execute(new ViewSupportedLanguagesQuery(0, 20, null, null))
        );
        var sizeException = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> useCase.execute(new ViewSupportedLanguagesQuery(1, 0, null, null))
        );

        assertThat(pageException).hasMessage("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
        assertThat(sizeException).hasMessage("Số trang hoặc kích cỡ trang yêu cầu không hợp lệ");
    }

    private static SupportedLanguage language(String code, String name, boolean active) {
        var now = OffsetDateTime.now();
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
