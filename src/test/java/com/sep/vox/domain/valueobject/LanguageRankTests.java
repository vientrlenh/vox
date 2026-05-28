package com.sep.vox.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LanguageRankTests {

    @Test
    void should_accept_positive_language_rank() {
        var languageRank = new LanguageRank(1);

        assertThat(languageRank.value()).isEqualTo(1);
    }

    @Test
    void should_reject_zero_language_rank() {
        assertThrows(IllegalArgumentException.class, () -> new LanguageRank(0));
    }

    @Test
    void should_reject_negative_language_rank() {
        assertThrows(IllegalArgumentException.class, () -> new LanguageRank(-1));
    }
}
