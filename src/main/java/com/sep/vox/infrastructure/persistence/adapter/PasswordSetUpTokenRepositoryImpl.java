package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.infrastructure.persistence.mapper.PasswordSetUpTokenMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPasswordSetUpTokenRepository;

@Repository
public class PasswordSetUpTokenRepositoryImpl implements PasswordSetUpTokenRepository {

    private final SpringDataPasswordSetUpTokenRepository springDataPasswordSetUpTokenRepository;

    public PasswordSetUpTokenRepositoryImpl(SpringDataPasswordSetUpTokenRepository springDataPasswordSetUpTokenRepository) {
        this.springDataPasswordSetUpTokenRepository = springDataPasswordSetUpTokenRepository;
    }

    @Override
    public Optional<PasswordSetUpToken> findById(UUID id) {
        return springDataPasswordSetUpTokenRepository.findById(id)
            .map(PasswordSetUpTokenMapper::toDomain);
    }

    @Override
    public Optional<PasswordSetUpToken> findByUserIdAndTokenHash(UUID userId, String tokenHash) {
        return springDataPasswordSetUpTokenRepository.findByUserIdAndTokenHash(userId, tokenHash)
            .map(PasswordSetUpTokenMapper::toDomain);
    }

    @Override
    public PasswordSetUpToken save(PasswordSetUpToken passwordSetUpToken) {
        var entity = PasswordSetUpTokenMapper.toJpa(passwordSetUpToken);
        var saved = springDataPasswordSetUpTokenRepository.save(entity);
        return PasswordSetUpTokenMapper.toDomain(saved);
    }
    
}
