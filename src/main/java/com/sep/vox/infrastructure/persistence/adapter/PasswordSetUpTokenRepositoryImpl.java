package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.passwordsetuptoken.PasswordSetUpTokenJpaEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.passwordsetuptoken.PasswordSetUpToken;
import com.sep.vox.domain.repository.PasswordSetUpTokenRepository;
import com.sep.vox.infrastructure.persistence.mapper.PasswordSetUpTokenMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPasswordSetUpTokenRepository;

@Repository
@RequiredArgsConstructor
public class PasswordSetUpTokenRepositoryImpl implements PasswordSetUpTokenRepository {

    private final SpringDataPasswordSetUpTokenRepository springDataPasswordSetUpTokenRepository;

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
        PasswordSetUpTokenJpaEntity entity = PasswordSetUpTokenMapper.toJpa(passwordSetUpToken);
        PasswordSetUpTokenJpaEntity saved = springDataPasswordSetUpTokenRepository.save(entity);
        return PasswordSetUpTokenMapper.toDomain(saved);
    }

    @Override
    public int updateUsedToken(UUID userId, String tokenHash, Instant now) {
        return springDataPasswordSetUpTokenRepository.updateUsedToken(userId, tokenHash, now);
    }
    
}
