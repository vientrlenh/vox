package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRefreshTokenRepository;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository springDataRefreshTokenRepository;

    public RefreshTokenRepositoryImpl(SpringDataRefreshTokenRepository springDataRefreshTokenRepository) {
        this.springDataRefreshTokenRepository = springDataRefreshTokenRepository;
    }

    @Override
    public List<RefreshToken> findByDeviceSessionId(UUID deviceSessionId) {
        return springDataRefreshTokenRepository.findByDeviceSessionId(deviceSessionId)
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
        RefreshTokenJpaEntity entity = RefreshTokenMapper.toJpa(token);
        RefreshTokenJpaEntity saved = springDataRefreshTokenRepository.save(entity);
        return RefreshTokenMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return springDataRefreshTokenRepository.findByTokenHashForUpdate(tokenHash)
            .map(RefreshTokenMapper::toDomain);
    }

    @Override
    public int markUsedAndReplacedBy(UUID oldTokenId, UUID newTokenId, Instant now) {
        return springDataRefreshTokenRepository.markUsedAndReplacedBy(oldTokenId, newTokenId, now);
    }

    @Override
    public boolean existsByTokenHash(String tokenHash) {
        return springDataRefreshTokenRepository.existsByTokenHash(tokenHash);
    }
    
}
