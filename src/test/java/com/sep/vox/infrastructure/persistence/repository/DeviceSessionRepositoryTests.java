package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.infrastructure.persistence.adapter.DeviceSessionRepositoryImpl;

@DataJpaTest
@ActiveProfiles("test")
@Import({
    DeviceSessionRepositoryImpl.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeviceSessionRepositoryTests extends ContainerTestConfig {

    @Autowired
    private DeviceSessionRepository deviceSessionRepository;

    @Test
    void whenSave_thenReturnsPersistedDeviceSession() {
        var userId = UUID.randomUUID();

        var saved = deviceSessionRepository.save(newDeviceSession(userId, "device-1", "Chrome on Windows"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getDeviceId()).isEqualTo("device-1");
        assertThat(saved.getDeviceName()).isEqualTo("Chrome on Windows");
        assertThat(saved.getPlatform()).isEqualTo(SessionPlatform.WEB);
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    void whenFindById_thenReturnsDeviceSession() {
        var saved = deviceSessionRepository.save(newDeviceSession(UUID.randomUUID(), "device-2", "Safari on iPhone"));

        var found = deviceSessionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getDeviceId()).isEqualTo("device-2");
    }

    @Test
    void whenFindByUserId_thenReturnsOnlySessionsForThatUser() {
        var userId = UUID.randomUUID();
        deviceSessionRepository.save(newDeviceSession(userId, "device-3", "Chrome on Windows"));
        deviceSessionRepository.save(newDeviceSession(userId, "device-4", "Android App"));
        deviceSessionRepository.save(newDeviceSession(UUID.randomUUID(), "device-5", "Desktop App"));

        var found = deviceSessionRepository.findByUserId(userId);

        assertThat(found).hasSize(2);
        assertThat(found)
            .extracting(DeviceSession::getDeviceId)
            .containsExactlyInAnyOrder("device-3", "device-4");
    }

    private static DeviceSession newDeviceSession(UUID userId, String deviceId, String deviceName) {
        return new DeviceSession(
            userId,
            deviceId,
            deviceName,
            SessionPlatform.WEB,
            "203.0.113.10",
            "JUnit User Agent",
            null
        );
    }
}
