package com.sep.vox.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * Validator chỉ chạy đúng một lần lúc khởi động, nên nếu logic của nó sai chiều (lọc nhầm
 * tập, so nhầm bảng) thì hậu quả là app vẫn lên bình thường và không kiểm gì cả -- im lặng
 * y hệt lúc chưa có validator. Test này gọi thẳng bean với ba bảng ánh xạ thật.
 *
 * <p>Không có test cho nhánh ném lỗi: cả ba bảng đều là {@code static final}, muốn làm chúng
 * lệch nhau phải dùng reflection, và cái bẫy thật sự -- ai đó thêm event mới mà quên một bảng
 * -- đã được {@code NotificationPushedEventConsumerTests} bắt bằng phép so trực tiếp hai tập
 * eventType.
 */
class NotificationCategoryMappingValidatorTests {

    @Test
    void should_pass_with_the_real_mapping_tables() {
        var checker = new NotificationCategoryMappingValidator().notificationCategoryMappingChecker();

        assertThatCode(checker::afterPropertiesSet).doesNotThrowAnyException();
    }
}
