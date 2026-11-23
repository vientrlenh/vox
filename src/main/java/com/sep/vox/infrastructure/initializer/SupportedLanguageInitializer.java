package com.sep.vox.infrastructure.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class SupportedLanguageInitializer implements ApplicationRunner {

    private final SupportedLanguageRepository supportedLanguageRepository;

    private static final String ENGLISH_CODE = "ENG";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (supportedLanguageRepository.count() > 0) {
            log.info("Supported language already exist in database. Skip initialize");
            return;
        }
        SupportedLanguage english = SupportedLanguage.builder()
            .code(new LanguageCode(ENGLISH_CODE))
            .name("Tiếng Anh")
            .description("Tiếng Anh hỗ trợ cho việc luyện thi")
            .active(true)
            .build();
        supportedLanguageRepository.save(english);
        log.info("Supported language initialized successfully");
    }
    
}
