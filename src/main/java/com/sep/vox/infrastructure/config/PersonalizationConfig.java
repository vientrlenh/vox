package com.sep.vox.infrastructure.config;

import com.sep.vox.infrastructure.properties.InterestQuizProperties;
import com.sep.vox.infrastructure.properties.PracticeGenerationProperties;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Cấu hình cá nhân hoá. Mọi con số về SỐ LƯỢNG CÂU khai ở application.yaml dưới
 * {@code app.personalization.*}, không rải hằng số trong mã -- xem hai lớp properties bên dưới.
 *
 * <p>Đăng ký tường minh dù {@code VoxApplication} đã có {@code @ConfigurationPropertiesScan}:
 * ba lớp properties có sẵn của repo (S3, outbox, SePay) đều làm vậy. Dựa vào mỗi scan thì ngày
 * ai đó gỡ annotation kia đi, Spring báo "not registered as a bean" lúc KHỞI ĐỘNG -- mà
 * compileJava không bắt được loại lỗi đó.
 */
@Configuration
@EnableConfigurationProperties({
    PracticeGenerationProperties.class,
    InterestQuizProperties.class
})
public class PersonalizationConfig {
}
