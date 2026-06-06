package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolUserMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolUserRepository;

@Repository
public class SchoolUserRepositoryImpl implements SchoolUserRepository {

    private final SpringDataSchoolUserRepository springDataSchoolUserRepository;

    public SchoolUserRepositoryImpl(SpringDataSchoolUserRepository springDataSchoolUserRepository) {
        this.springDataSchoolUserRepository = springDataSchoolUserRepository;
    }

    @Override
    public Optional<SchoolUser> findByUserId(UUID userId) {
        return springDataSchoolUserRepository.findByUserId(userId)
            .map(SchoolUserMapper::toDomain);
    }

    @Override
    public SchoolUser save(SchoolUser schoolUser) {
        var entity = SchoolUserMapper.toJpa(schoolUser);
        var saved = springDataSchoolUserRepository.save(entity);
        return SchoolUserMapper.toDomain(saved);
    }
}
