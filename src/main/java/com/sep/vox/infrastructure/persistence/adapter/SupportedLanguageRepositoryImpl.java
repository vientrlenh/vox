package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
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
    public List<SupportedLanguage> findByCodeIn(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return springDataSupportedLanguageRepository.findByCodeIn(codes)
            .stream()
            .map(SupportedLanguageMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<SupportedLanguage> findAll(String search, Boolean isActive, PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(
            pageRequest.page() - 1,
            pageRequest.size(),
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))
        );
        var page = springDataSupportedLanguageRepository.findAllWithSearchAndFilters(
            toSearchPattern(search),
            isActive,
            pageable
        );
        return new PageResult<>(
            page.getContent().stream()
                .map(SupportedLanguageMapper::toDomain)
                .toList(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
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

    private static String toSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.toLowerCase() + "%";
    }
    
}
