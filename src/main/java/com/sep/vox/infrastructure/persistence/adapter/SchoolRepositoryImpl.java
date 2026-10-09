package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.shared.PageResult;
import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.SchoolMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor 
public class SchoolRepositoryImpl implements SchoolRepository {

    private final SpringDataSchoolRepository springDataSchoolRepository;
    @Override
    public Optional<School> findById(UUID id) {
        return springDataSchoolRepository.findById(id)
            .map(SchoolMapper::toDomain);
    }

    @Override
    public Optional<School> findByCode(String code) {
        return springDataSchoolRepository.findByCode(code)
            .map(SchoolMapper::toDomain);
    }

    @Override
    public List<School> findByDomain(String domain) {
        return springDataSchoolRepository.findByDomain(domain)
            .stream()
            .map(SchoolMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<School> findAll(int page, int size, String search, Boolean isActive) {
        Pageable pageRequest = PageRequest.of(page, size);
        String pattern = StringNormalization.toLikePattern(search);
        Page<SchoolJpaEntity> pageable = isActive != null
            ? springDataSchoolRepository.findAllBySearchAndIsActive(pattern, isActive, pageRequest)
            : springDataSchoolRepository.findAllBySearch(pattern, pageRequest);
        List<School> schools = pageable.getContent()
                                        .stream()
                                        .map(SchoolMapper::toDomain)
                                        .toList();
        return PageResult.fromRepository(
            schools, 
            page, 
            size, 
            pageable.getTotalElements(), 
            pageable.getTotalPages()
        );
    }

    @Override
    public School save(School school) {
        SchoolJpaEntity entity = SchoolMapper.toJpa(school);
        SchoolJpaEntity saved = springDataSchoolRepository.save(entity);
        return SchoolMapper.toDomain(saved);
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataSchoolRepository.existsById(id);
    }


    @Override
    public boolean existsByContactEmailAndIdNot(String email, UUID id) {
        return springDataSchoolRepository.existsByContactEmailAndIdNot(email, id);
    }

    @Override
    public boolean existsByContactPhoneAndIdNot(String phone, UUID id) {
        return springDataSchoolRepository.existsByContactPhoneAndIdNot(phone, id);
    }

    @Override
    public void deleteById(UUID id) {
        springDataSchoolRepository.deleteById(id);
    }



    @Override
    public boolean existsByIdAndIsActiveTrue(UUID schoolId) {
       return springDataSchoolRepository.existsByIdAndIsActiveTrue(schoolId);
    }

    @Override
    public int updateSchoolAtomic(UUID id, String name, String description, String phone,
                                  String email, String domain, String address, Integer studentCount,
                                  Instant now, UUID updatedBy) {
        return springDataSchoolRepository.updateSchoolAtomic(
                id, name, description, phone, email, domain, address, studentCount, now, updatedBy
        );
    }

    @Override
    public List<School> findByIdIn(Collection<UUID> ids) {
        return springDataSchoolRepository.findByIdIn(ids)
            .stream()
            .map(SchoolMapper::toDomain)
            .toList();

    }


    @Override
    public boolean existsByCode(String code) {
        return springDataSchoolRepository.existsByCode(code);
    }


    @Override
    public long countAll() {
        return springDataSchoolRepository.count();
    }

    @Override
    public long countByIsActiveTrue() {
        return springDataSchoolRepository.countByIsActiveTrue();
    }

    @Override
    public List<UUID> findIdsWithOngoingExam(Instant now) {
        return springDataSchoolRepository.findIdsWithOngoingExam(now);
    }

}
