package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.session.Session;
import com.sep.vox.domain.repository.SessionRepository;
import com.sep.vox.infrastructure.persistence.mapper.SessionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSessionRepository;

@Repository
public class SessionRepositoryImpl implements SessionRepository {

    private final SpringDataSessionRepository springDataSessionRepository;

    public SessionRepositoryImpl(SpringDataSessionRepository springDataSessionRepository) {
        this.springDataSessionRepository = springDataSessionRepository;
    }

    @Override
    public Session save(Session session) {
        var entity = SessionMapper.toJpa(session);
        var saved = springDataSessionRepository.save(entity);
        return SessionMapper.toDomain(saved);
    }

    @Override
    public Optional<Session> findByUserIdAndRefreshTokenHash(UUID userId, String refreshTokenHash) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findByUserIdAndRefreshTokenHash'");
    }
    
}
