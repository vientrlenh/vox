package com.sep.vox.domain.model.supportedlanguage;

import static org.assertj.core.api.Assertions.assertThat;

import com.sep.vox.domain.valueobject.LanguageCode;
import org.junit.jupiter.api.Test;

class SupportedLanguageTests {

    @Test
    void definedSetsEveryArgumentAndStartsActive() {
        LanguageCode code = new LanguageCode("EN");

        SupportedLanguage language = SupportedLanguage.defined(code, "English", "English language");

        assertThat(language.getId()).isNull();
        assertThat(language.getCode()).isEqualTo(code);
        assertThat(language.getName()).isEqualTo("English");
        assertThat(language.getDescription()).isEqualTo("English language");
        assertThat(language.isActive()).isTrue();
    }
}
