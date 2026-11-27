package com.sep.vox.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.service.assessment.GradingScopeKind;

/**
 * Default of the grading screen when the client does not state the assessment kind.
 *
 * <p>The school dispatch board is the only place that still calls without passing the kind, and
 * leaving it blank there is exactly the bug of mixing centralized assessments with class tests.
 */
class GradingScopeKindTests {

    @Test
    void should_default_to_centralized_when_no_kind_is_given() {
        assertThat(GradingScopeKind.orCentralized(null)).isEqualTo("CENTRALIZED");
        assertThat(GradingScopeKind.orCentralized("")).isEqualTo("CENTRALIZED");
        assertThat(GradingScopeKind.orCentralized("   ")).isEqualTo("CENTRALIZED");
    }

    @Test
    void should_keep_the_kind_the_caller_asked_for() {
        assertThat(GradingScopeKind.orCentralized("CLASS_TEST")).isEqualTo("CLASS_TEST");
        assertThat(GradingScopeKind.orCentralized(" CENTRALIZED ")).isEqualTo("CENTRALIZED");
    }

    /** Falling back to the default now would silently hand over the data of the other kind. */
    @Test
    void should_reject_a_kind_that_does_not_exist() {
        assertThatThrownBy(() -> GradingScopeKind.orCentralized("MOCK_TEST"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
