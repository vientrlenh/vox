package com.sep.vox.infrastructure.initializer;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.valueobject.LanguageCode;

@Component
@Order(3)
public class SupportedLanguageInitializer implements ApplicationRunner {

    private final SupportedLanguageRepository supportedLanguageRepository;

    public SupportedLanguageInitializer(SupportedLanguageRepository supportedLanguageRepository) {
        this.supportedLanguageRepository = supportedLanguageRepository;
    }

    private static final String ENGLISH_CODE = "ENG";

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportedLanguageInitializer.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (supportedLanguageRepository.count() > 0) {
            LOGGER.info("Supported language already exist in database. Skip initialize");
            return;
        }
        var now = Instant.now();
        var english = new SupportedLanguage(
            new LanguageCode(ENGLISH_CODE), 
            "Tiếng Anh", 
            "Tiếng Anh hỗ trợ cho việc luyện thi", 
            true, 
            now, 
            now, 
            null, 
            null
        );
        supportedLanguageRepository.save(english);
        LOGGER.info("Supported language initialized successfully");
    }
    
}
