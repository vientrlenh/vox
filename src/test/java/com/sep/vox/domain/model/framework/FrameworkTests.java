package com.sep.vox.domain.model.framework;

import static org.assertj.core.api.Assertions.assertThat;

import com.sep.vox.domain.valueobject.FrameworkCode;
import org.junit.jupiter.api.Test;

class FrameworkTests {

    @Test
    void registeredSetsEveryArgumentAndStartsInactive() {
        FrameworkCode code = new FrameworkCode("CEFR");

        Framework framework = Framework.registered(code, "CEFR framework", "Common European Framework");

        assertThat(framework.getId()).isNull();
        assertThat(framework.getCode()).isEqualTo(code);
        assertThat(framework.getName()).isEqualTo("CEFR framework");
        assertThat(framework.getDescription()).isEqualTo("Common European Framework");
        assertThat(framework.isActive()).isFalse();
    }
}
