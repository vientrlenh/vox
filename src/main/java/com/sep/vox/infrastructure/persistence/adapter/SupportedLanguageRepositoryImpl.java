package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.supportedlanguage.SupportedLanguageJpaEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.infrastructure.persistence.mapper.SupportedLanguageMapper;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSupportedLanguageRepository;
import com.sep.vox.infrastructure.persistence.DataConflictTranslation;

@Repository
@RequiredArgsConstructor
public class SupportedLanguageRepositoryImpl implements SupportedLanguageRepository {

    private final SpringDataSupportedLanguageRepository springDataSupportedLanguageRepository;

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
    public Optional<SupportedLanguage> findByName(String name) {
        return springDataSupportedLanguageRepository.findByName(name)
            .map(SupportedLanguageMapper::toDomain);
    }

    @Override
    public List<SupportedLanguage> findByCodeIn(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        Set<String> upperCodes = codes.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        return springDataSupportedLanguageRepository.findByCodeIn(upperCodes)
            .stream()
            .map(SupportedLanguageMapper::toDomain)
            .toList();
    }

    @Override
    public List<SupportedLanguage> findByNameIn(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return springDataSupportedLanguageRepository.findByNameIn(names)
            .stream()
            .map(SupportedLanguageMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<SupportedLanguage> findAll(String search, Boolean isActive, int pageNumber, int size) {
        PageRequest pageable = OneBasedPaging.toPageRequest(
            pageNumber,
            size,
            Sort.by(Sort.Direction.DESC, (SupportedLanguageJpaEntity entity) -> entity.getCreatedAt()).and(Sort.by(Sort.Direction.ASC, (SupportedLanguageJpaEntity entity) -> entity.getId()))
        );
        Page<SupportedLanguageJpaEntity> page = springDataSupportedLanguageRepository.findAllWithSearchAndFilters(
            toSearchPattern(search),
            isActive,
            pageable
        );
        return new PageResult<>(
            page.getContent().stream()
                .map(SupportedLanguageMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public SupportedLanguage save(SupportedLanguage supportedLanguage) {
        SupportedLanguageJpaEntity entity = SupportedLanguageMapper.toJpa(supportedLanguage);
        SupportedLanguageJpaEntity saved = DataConflictTranslation.translating(() -> springDataSupportedLanguageRepository.save(entity));
        return SupportedLanguageMapper.toDomain(saved);
    }

    @Override
    public int updateMutableFields(UUID id, String code, boolean codeProvided, String name, boolean nameProvided,
            String description, boolean descriptionProvided, Boolean isActive, boolean isActiveProvided,
            Instant updatedAt, String updatedBy) {
        return DataConflictTranslation.translating(() -> springDataSupportedLanguageRepository.updateMutableFields(
            id,
            code,
            codeProvided,
            name,
            nameProvided,
            description,
            descriptionProvided,
            isActive,
            isActiveProvided,
            updatedAt,
            updatedBy
        ));
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
    
    @Override
    public List<SupportedLanguage> findByIdIn(Collection<UUID> ids) {
        return springDataSupportedLanguageRepository.findByIdIn(ids)
            .stream()
            .map(SupportedLanguageMapper::toDomain)
            .toList();
    }
    
}
