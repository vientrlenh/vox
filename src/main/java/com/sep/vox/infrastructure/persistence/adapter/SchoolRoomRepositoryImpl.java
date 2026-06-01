package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.schoolroom.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.infrastructure.persistence.entity.SchoolRoomJpaEntity;
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
    public List<SchoolRoom> findAll() {
        return springDataSchoolRoomRepository.findAll()
                .stream()
                .map(SchoolRoomMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<SchoolRoom> findAll(int page, int size) {
        // 1. Code bây giờ cực kỳ "tự nhiên", không có một dấu ép kiểu nào!
        Pageable springPageable = PageRequest.of(page, size);

        // 2. Truyền thẳng vào hàm findAll
        Page<SchoolRoomJpaEntity> entityPage = springDataSchoolRoomRepository.findAll(springPageable);

        // 3. Map ruột content
        List<SchoolRoom> domainContent = entityPage.getContent().stream()
                .map(SchoolRoomMapper::toDomain)
                .toList();

        // 4. Bọc lại vào PageResult
        return new PageResult<>(
                domainContent,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }
}