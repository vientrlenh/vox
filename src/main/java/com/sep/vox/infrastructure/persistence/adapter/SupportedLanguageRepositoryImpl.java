package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.infrastructure.persistence.mapper.SupportedLanguageMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSupportedLanguageRepository;

@Repository
public class SupportedLanguageRepositoryImpl implements SupportedLanguageRepository {

    private final SpringDataSupportedLanguageRepository springDataSupportedLanguageRepository;

    public SupportedLanguageRepositoryImpl(SpringDataSupportedLanguageRepository springDataSupportedLanguageRepository) {
        this.springDataSupportedLanguageRepository = springDataSupportedLanguageRepository;
    }

    @Override
    public Optional<SupportedLanguage> findById(UUID id) {
        return springDataSupportedLanguageRepository.findById(id)
            .map(SupportedLanguageMapper::toDomain);
    }

    @Override
    public Optional<SupportedLanguage> findByCode(String code) {
        return springDataSupportedLanguageRepository.findByCode(code)
            .map(SupportedLanguageMapper::toDomain);
    }

    @Override
    public SupportedLanguage save(SupportedLanguage supportedLanguage) {
        var entity = SupportedLanguageMapper.toJpa(supportedLanguage);
        var saved = springDataSupportedLanguageRepository.save(entity);
        return SupportedLanguageMapper.toDomain(saved);
    }

    @Override
    public long count() {
        return springDataSupportedLanguageRepository.count();
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataSupportedLanguageRepository.existsById(id);
    }

    @Override
    public boolean existsByIdAndIsActive(UUID id, boolean isActive) {
        return springDataSupportedLanguageRepository.existsByIdAndIsActive(id, isActive);
    }
    
}
