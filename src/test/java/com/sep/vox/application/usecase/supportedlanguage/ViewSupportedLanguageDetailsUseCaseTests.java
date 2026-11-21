package com.sep.vox.application.usecase.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSupportedLanguageDetailsQuery;
import com.sep.vox.application.port.input.usecase.supportedlanguage.ViewSupportedLanguageDetailsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

class ViewSupportedLanguageDetailsUseCaseTests {

    private SupportedLanguageRepository supportedLanguageRepository;
    private UserContextPort userContextPort;
    private ViewSupportedLanguageDetailsUseCase useCase;

    @BeforeEach
    void setUp() {
        supportedLanguageRepository = mock(SupportedLanguageRepository.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewSupportedLanguageDetailsUseCase(supportedLanguageRepository, userContextPort);
    }

    @Test
    void details_should_return_language_when_found() {
        UUID id = UUID.randomUUID();
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.of(language(id, true)));

        SupportedLanguageDto result = useCase.execute(new ViewSupportedLanguageDetailsQuery(id));

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.code()).isEqualTo("EN");
    }

    @Test
    void details_should_throw_when_language_not_found() {
        UUID id = UUID.randomUUID();
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException exception = Assertions.assertThrows(
            NotFoundException.class,
            () -> useCase.execute(new ViewSupportedLanguageDetailsQuery(id))
        );

        assertThat(exception).hasMessage("Không tìm thấy ngôn ngữ");
    }

    @Test
    void details_should_throw_when_active_only_and_language_is_inactive() {
        UUID id = UUID.randomUUID();
        when(supportedLanguageRepository.findById(id)).thenReturn(Optional.of(language(id, false)));

        NotFoundException exception = org.junit.jupiter.api.Assertions.assertThrows(
            NotFoundException.class,
            () -> useCase.execute(new ViewSupportedLanguageDetailsQuery(id))
        );

        assertThat(exception).hasMessage("Không tìm thấy ngôn ngữ");
    }

    private static SupportedLanguage language(UUID id, boolean active) {
        return SupportedLanguage.builder()
            .id(id)
            .code(new LanguageCode("EN"))
            .name("English")
            .description(null)
            .active(active)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .createdBy(UUID.randomUUID().toString())
            .updatedBy(UUID.randomUUID().toString())
            .build();
    }
}
