package com.sep.vox.infrastructure.persistence.adapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRefreshTokenRepository;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository springDataRefreshTokenRepository;

    public RefreshTokenRepositoryImpl(SpringDataRefreshTokenRepository springDataRefreshTokenRepository) {
        this.springDataRefreshTokenRepository = springDataRefreshTokenRepository;
    }

    @Override
    public List<RefreshToken> findBySessionId(UUID sessionId) {
        return springDataRefreshTokenRepository.findBySessionId(sessionId)
            .stream()
            .map(RefreshTokenMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<RefreshToken> findById(UUID id) {
        return springDataRefreshTokenRepository.findById(id)
            .map(RefreshTokenMapper::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        var entity = RefreshTokenMapper.toJpa(token);
        var saved = springDataRefreshTokenRepository.save(entity);
        return RefreshTokenMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return springDataRefreshTokenRepository.findByTokenHash(tokenHash)
            .map(RefreshTokenMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String token) {
        return springDataRefreshTokenRepository.findByTokenHashForUpdate(token)
            .map(RefreshTokenMapper::toDomain);
    }

    @Override
    public int markUsedAndReplacedBy(UUID oldTokenId, UUID newTokenId, OffsetDateTime now) {
        return springDataRefreshTokenRepository.markUsedAndReplacedBy(oldTokenId, newTokenId, now);
    }
    
}
