package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.infrastructure.persistence.mapper.DeviceSessionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataDeviceSessionRepository;

@Repository
public class DeviceSessionRepositoryImpl implements DeviceSessionRepository {

    private final SpringDataDeviceSessionRepository springDataDeviceSessionRepository;

    public DeviceSessionRepositoryImpl(SpringDataDeviceSessionRepository springDataDeviceSessionRepository) {
        this.springDataDeviceSessionRepository = springDataDeviceSessionRepository;
    }

    @Override
    public DeviceSession save(DeviceSession session) {
        var entity = DeviceSessionMapper.toJpa(session);
        var saved = springDataDeviceSessionRepository.save(entity);
        return DeviceSessionMapper.toDomain(saved);
    }

    @Override
    public Optional<DeviceSession> findById(UUID id) {
        return springDataDeviceSessionRepository.findById(id)
            .map(DeviceSessionMapper::toDomain);
    }

    @Override
    public List<DeviceSession> findByUserId(UUID userId) {
        return springDataDeviceSessionRepository.findByUserId(userId)
            .stream()
            .map(DeviceSessionMapper::toDomain)
            .toList();
    }

   
    
}
