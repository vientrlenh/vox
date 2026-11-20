package com.sep.vox.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Binds enum values of query and path parameters in any letter case, matching what
 * {@code spring.jackson.mapper.accept-case-insensitive-enums} does for JSON bodies.
 */
@Configuration
public class CaseInsensitiveEnumWebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new CaseInsensitiveStringToEnumConverterFactory());
    }

    private static final class CaseInsensitiveStringToEnumConverterFactory
            implements ConverterFactory<String, Enum<?>> {

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
            return source -> (T) convert(source.trim(), targetType);
        }

        private static Enum<?> convert(String source, Class<? extends Enum<?>> enumType) {
            if (source.isEmpty()) {
                return null;
            }
            for (Enum<?> constant : enumType.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(source)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Unknown enum value");
        }
    }
}
