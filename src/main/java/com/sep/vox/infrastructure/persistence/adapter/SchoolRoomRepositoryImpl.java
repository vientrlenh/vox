package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import org.springframework.data.domain.PageRequest;
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
    public PageResult<SchoolRoom> findAllBySchoolId(UUID schoolId, int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);

        var page = springDataSchoolRoomRepository.findBySchoolId(schoolId, pageable);

        List<SchoolRoom> domainContent = page.getContent().stream()
                .map(SchoolRoomMapper::toDomain)
                .toList();

        return new PageResult<>(
                domainContent,
                pageNumber,
                size,
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    public boolean existsBySchoolIdAndIsActive(UUID schoolId, boolean isActive) {
        return springDataSchoolRoomRepository.existsBySchoolIdAndIsActive(schoolId, isActive);
    }

    @Override
    public int updateSchoolRoomAtomic(UUID id, String name, String description, OffsetDateTime now, UUID updatedBy) {
        return springDataSchoolRoomRepository.updateSchoolRoomAtomic(id, name, description, now, updatedBy);
    }
}