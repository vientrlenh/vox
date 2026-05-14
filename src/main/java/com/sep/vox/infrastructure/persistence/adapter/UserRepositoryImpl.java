package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.infrastructure.persistence.mapper.UserMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataUserRepository;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    public UserRepositoryImpl(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username)
            .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id)
            .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return springDataUserRepository.findByPhone(phone)
            .map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email)
            .map(UserMapper::toDomain);
    }

    @Override
    public User save(User user) {
        var entity = UserMapper.toJpa(user);
        var saved = springDataUserRepository.save(entity);
        return UserMapper.toDomain(saved);
    }
    
}
