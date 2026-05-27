package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LanguageCodeTests {
    
    @Test
    void should_accept_all_uppercase_characters() {
        var code = new LanguageCode("JPN");
        assertThat(code.value()).isEqualTo("JPN");
    }

    @Test
    void should_reject_a_lower_character() {
        assertThrows(IllegalArgumentException.class, () -> new LanguageCode("eNG")) ;

    }
}
