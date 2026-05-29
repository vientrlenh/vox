package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.languagelevel.LanguageLevel;
import com.sep.vox.domain.repository.LanguageLevelRepository;
import com.sep.vox.infrastructure.persistence.mapper.LanguageLevelMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataLanguageLevelRepository;

@Repository
public class LanguageLevelRepositoryImpl implements LanguageLevelRepository {

    private final SpringDataLanguageLevelRepository springDataLanguageLevelRepository;

    public LanguageLevelRepositoryImpl(SpringDataLanguageLevelRepository springDataLanguageLevelRepository) {
        this.springDataLanguageLevelRepository = springDataLanguageLevelRepository;
    }

    @Override
    public Optional<LanguageLevel> findById(UUID id) {
        return springDataLanguageLevelRepository.findById(id)
            .map(LanguageLevelMapper::toDomain);
    }

    @Override
    public LanguageLevel save(LanguageLevel languageLevel) {
        var entity = LanguageLevelMapper.toJpa(languageLevel);
        var saved = springDataLanguageLevelRepository.save(entity);
        return LanguageLevelMapper.toDomain(saved);
    }
    
}
