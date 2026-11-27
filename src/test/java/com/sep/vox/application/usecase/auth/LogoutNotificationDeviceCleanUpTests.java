package com.sep.vox.application.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.port.input.command.auth.LogoutCommand;
import com.sep.vox.application.port.input.usecase.auth.LogoutUseCase;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.NotificationDeviceRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;
import com.sep.vox.support.IntegrationTest;

/**
 * Logout must also remove the notification device of the machine that just logged out.
 *
 * <p>Why a real transaction is needed: the original bug sits EXACTLY on the transaction boundary, not in any
 * logic. {@code NotificationDeviceRevokeListener} runs in the AFTER_COMMIT phase, and at that moment the
 * {@code EntityManagerHolder} of the transaction that just committed is still bound to the thread -- so a
 * default-propagation (REQUIRED) {@code @Transactional} JOINS the dead transaction instead of opening a new
 * one, and Hibernate rejects the DELETE with {@code No active transaction for update or delete query}.
 *
 * <p>This failure is completely SILENT: {@code TransactionSynchronizationUtils} catches the Throwable and only
 * logs an ERROR, so /logout still returns 200 and the session is still revoked correctly. The only thing
 * lost is removing the device -- meaning that on a lab computer, the grade notifications of the student who
 * just left the seat still pop up for the student who sits down next. No test at a lower level can see that:
 * {@code LogoutUseCaseTests} and {@code DeviceSessionProviderTests} both build the objects with {@code new}
 * and mocks, so there is no proxy, no transaction, and the listener never runs.
 *
 * <p>This class is DELIBERATELY not {@code @Transactional}: a test wrapped in a rollback transaction never
 * commits, and without a commit the AFTER_COMMIT phase never happens -- the test would be green and check
 * nothing.
 *
 * <p>Each test uses a freshly generated {@code userId} and {@code installationId}, so leftover rows do not
 * affect each other; the schema is rebuilt with the context lifecycle anyway.
 */
@IntegrationTest
@AutoConfigureTestGrpcTransport
// application-test.yaml pins the pool at 2, but one logout holds up to THREE connections at once: the outer
// transaction of LogoutUseCase, the REQUIRES_NEW transaction of revoke (the outer transaction is suspended
// but keeps its connection), then the REQUIRES_NEW transaction of the listener -- the revoke connection has
// not been returned when the AFTER_COMMIT callback runs, because Spring only returns it in
// cleanupAfterCompletion. With a pool of 2 the listener waits 30 seconds and dies with "Connection is not
// available", not with a transaction error.
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=5")
class LogoutNotificationDeviceCleanUpTests {

    private static final String DEVICE_ID = "lab-pc-01";
    private static final String OTHER_DEVICE_ID = "home-laptop";

    @Autowired
    private LogoutUseCase logoutUseCase;

    @Autowired
    private DeviceSessionRepository deviceSessionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private NotificationDeviceRepository notificationDeviceRepository;

    @Autowired
    private SessionTokenManagerPort sessionTokenManagerPort;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Empty = the caller carries no valid access token. That is the state of an abandoned session and the path
     * where {@link LogoutUseCase} has only the cookie as evidence -- also the shortest path to the listener
     * being checked here.
     */
    @MockitoBean
    private UserContextPort userContextPort;

    private UUID userId;
    private UUID sessionId;
    /** Fresh for every test: {@code refresh_tokens.token_hash} has a unique index (idx_refresh_token_hash). */
    private String rawRefreshToken;

    @BeforeEach
    void setUp() {
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());

        userId = UUID.randomUUID();
        rawRefreshToken = "raw-refresh-token-" + UUID.randomUUID();
        Instant now = Instant.now();

        sessionId = transactionTemplate.execute(status -> deviceSessionRepository.save(DeviceSession.builder()
            .userId(userId)
            .deviceId(DEVICE_ID)
            .deviceName("Lab PC")
            .platform(SessionPlatform.WEB)
            .ipAddress("203.0.113.10")
            .userAgent("JUnit User Agent")
            .build()).getId());

        transactionTemplate.executeWithoutResult(status -> refreshTokenRepository.save(
            RefreshToken.createFresh(sessionId, sessionTokenManagerPort.hash(rawRefreshToken), now)));

        registerNotificationDevice(DEVICE_ID);
    }

    @Test
    void should_remove_the_notification_device_of_the_logged_out_device() {
        logoutUseCase.execute(new LogoutCommand(rawRefreshToken, DEVICE_ID));

        // The session being revoked is the condition for the listener to run -- also checked here so that if this
        // test ever breaks because logout no longer revokes anything, it is not misread as "device removed".
        assertThat(deviceSessionRepository.findById(sessionId).orElseThrow().isRevoked()).isTrue();
        assertThat(notificationDeviceRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void should_leave_notification_devices_of_other_devices_alone() {
        registerNotificationDevice(OTHER_DEVICE_ID);

        logoutUseCase.execute(new LogoutCommand(rawRefreshToken, DEVICE_ID));

        assertThat(notificationDeviceRepository.findByUserId(userId))
            .extracting(device -> device.getDeviceId())
            .containsExactly(OTHER_DEVICE_ID);
    }

    private void registerNotificationDevice(String deviceId) {
        Instant now = Instant.now();
        // installationId is unique across the whole table (the ON CONFLICT of registerDevice relies
        // on it), so it must be generated fresh for each row rather than using a constant.
        String installationId = UUID.randomUUID().toString();
        transactionTemplate.executeWithoutResult(status -> notificationDeviceRepository.save(
            NotificationDevice.create(
                userId, deviceId, NotificationDevicePlatform.WEB, installationId, now)));
    }
}
