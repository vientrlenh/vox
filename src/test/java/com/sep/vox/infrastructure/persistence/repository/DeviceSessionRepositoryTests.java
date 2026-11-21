package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.DeviceSessionRepositoryImpl;
import com.sep.vox.support.RepositoryTest;

@RepositoryTest
@Import({
    DeviceSessionRepositoryImpl.class
})
class DeviceSessionRepositoryTests {

    @Autowired
    private DeviceSessionRepository deviceSessionRepository;

    @Test
    void whenSave_thenReturnsPersistedDeviceSession() {
        UUID userId = UUID.randomUUID();

        DeviceSession saved = deviceSessionRepository.save(newDeviceSession(userId, "device-1", "Chrome on Windows"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getDeviceId()).isEqualTo("device-1");
        assertThat(saved.getDeviceName()).isEqualTo("Chrome on Windows");
        assertThat(saved.getPlatform()).isEqualTo(SessionPlatform.WEB);
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    void whenFindById_thenReturnsDeviceSession() {
        DeviceSession saved = deviceSessionRepository.save(newDeviceSession(UUID.randomUUID(), "device-2", "Safari on iPhone"));

        Optional<DeviceSession> found = deviceSessionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getDeviceId()).isEqualTo("device-2");
    }

    @Test
    void whenFindByUserId_thenReturnsOnlySessionsForThatUser() {
        UUID userId = UUID.randomUUID();
        deviceSessionRepository.save(newDeviceSession(userId, "device-3", "Chrome on Windows"));
        deviceSessionRepository.save(newDeviceSession(userId, "device-4", "Android App"));
        deviceSessionRepository.save(newDeviceSession(UUID.randomUUID(), "device-5", "Desktop App"));

        List<DeviceSession> found = deviceSessionRepository.findByUserId(userId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(session -> session.getDeviceId())
            .containsExactlyInAnyOrder("device-3", "device-4");
    }

    private static DeviceSession newDeviceSession(UUID userId, String deviceId, String deviceName) {
        return DeviceSession.builder()
            .userId(userId)
            .deviceId(deviceId)
            .deviceName(deviceName)
            .platform(SessionPlatform.WEB)
            .ipAddress("203.0.113.10")
            .userAgent("JUnit User Agent")
            .build();
    }
}
