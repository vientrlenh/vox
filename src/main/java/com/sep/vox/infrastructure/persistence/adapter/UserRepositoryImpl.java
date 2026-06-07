package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
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


    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }


    @Override
    public int changeUserPassword(String email, String passwordHash) {
        return springDataUserRepository.changeUserPassword(email, passwordHash);
    }


    @Override
    public boolean existsByEmailAndStatus(String email, UserStatus status) {
        return springDataUserRepository.existsByEmailAndStatus(email, status.name());
    }


    @Override
    public Optional<User> findByEmailAndStatus(String email, UserStatus status) {
        return springDataUserRepository.findByEmailAndStatus(email, status.name())
            .map(UserMapper::toDomain);
    }


    @Override
    public Optional<User> findByIdAndStatus(UUID id, UserStatus status) {
        return springDataUserRepository.findByIdAndStatus(id, status.name())
            .map(UserMapper::toDomain);
    }


    @Override
    public boolean existsByIdAndStatus(UUID id, UserStatus status) {
        return springDataUserRepository.existsByIdAndStatus(id, status.name());
    }


    @Override
    public boolean existsByPhone(String phone) {
        return springDataUserRepository.existsByPhone(phone);
    }
    
}
