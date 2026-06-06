package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.infrastructure.persistence.entity.SchoolRoomJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolRoomMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolRoomRepository;

@Repository
public class SchoolRoomRepositoryImpl implements SchoolRoomRepository {

    private final SpringDataSchoolRoomRepository springDataSchoolRoomRepository;

    public SchoolRoomRepositoryImpl(SpringDataSchoolRoomRepository springDataSchoolRoomRepository) {
        this.springDataSchoolRoomRepository = springDataSchoolRoomRepository;
    }

    @Override
    public Optional<SchoolRoom> findById(UUID id) {
        return springDataSchoolRoomRepository.findById(id)
                .map(SchoolRoomMapper::toDomain);
    }

    @Override
    public SchoolRoom save(SchoolRoom room) {
        var entity = SchoolRoomMapper.toJpa(room);
        var saved = springDataSchoolRoomRepository.save(entity);
        return SchoolRoomMapper.toDomain(saved);
    }

    @Override
    public boolean existsBySchoolIdAndCode(UUID schoolId, String code) {
        return springDataSchoolRoomRepository.existsBySchoolIdAndCode(schoolId, code);
    }

    @Override
    public PageResult<SchoolRoom> findBySchoolId(UUID schoolId, int page, int size) {
        Pageable springPageable = PageRequest.of(page, size);

        // Gọi hàm của Spring Data
        Page<SchoolRoomJpaEntity> entityPage = springDataSchoolRoomRepository.findBySchoolId(schoolId, springPageable);

        // Map sang Domain Model
        List<SchoolRoom> domainContent = entityPage.getContent().stream()
                .map(SchoolRoomMapper::toDomain)
                .toList();

        return new PageResult<>(
                domainContent,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public Optional<SchoolRoom> findByIdForUpdate(UUID id) {
        return springDataSchoolRoomRepository.findByIdForUpdate(id)
                .map(SchoolRoomMapper::toDomain);
    }
}