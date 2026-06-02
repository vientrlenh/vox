package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.schoolroom.SchoolRoom;
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
}