package com.sep.vox.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.service.exam.GradingScopeKind;

/**
 * Mặc định của màn chấm bài khi client không nói rõ loại bài.
 *
 * <p>Bảng điều phối của nhà trường là chỗ duy nhất còn gọi mà không truyền loại bài, và
 * để trống ở đó chính là lỗi trộn kỳ thi tập trung với bài kiểm tra trên lớp.
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

    /** Rơi về mặc định lúc này là im lặng đưa nhầm dữ liệu của loại bài kia. */
    @Test
    void should_reject_a_kind_that_does_not_exist() {
        assertThatThrownBy(() -> GradingScopeKind.orCentralized("MOCK_TEST"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
