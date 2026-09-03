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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.application.port.input.usecase.auth.LogoutUseCase;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.config.ContainerTestConfig;
import com.sep.vox.domain.model.devicesession.DeviceSession;
import com.sep.vox.domain.model.devicesession.SessionPlatform;
import com.sep.vox.domain.model.notification.NotificationDevice;
import com.sep.vox.domain.model.notification.NotificationDevicePlatform;
import com.sep.vox.domain.model.refreshtoken.RefreshToken;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.NotificationDeviceRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;

/**
 * Đăng xuất phải gỡ luôn thiết bị nhận thông báo của máy vừa đăng xuất.
 *
 * <p>Vì sao cần transaction thật: bug gốc nằm ĐÚNG ở ranh giới giao dịch, không ở logic nào cả.
 * {@code NotificationDeviceRevokeListener} chạy ở pha AFTER_COMMIT, và lúc đó
 * {@code EntityManagerHolder} của giao dịch vừa commit vẫn còn gắn vào thread -- nên một
 * {@code @Transactional} propagation mặc định (REQUIRED) sẽ THAM GIA vào giao dịch đã chết thay vì
 * mở giao dịch mới, và câu DELETE bị Hibernate từ chối với
 * {@code No active transaction for update or delete query}.
 *
 * <p>Hỏng này IM LẶNG hoàn toàn: {@code TransactionSynchronizationUtils} bắt Throwable và chỉ log
 * ERROR, nên /logout vẫn trả 200 và phiên vẫn bị thu hồi đúng. Thứ duy nhất mất đi là việc gỡ
 * thiết bị -- tức là trên máy phòng lab, thông báo điểm của học sinh vừa rời ghế vẫn hiện lên cho
 * học sinh ngồi xuống sau. Không có test nào ở tầng dưới thấy được điều đó:
 * {@code LogoutUseCaseTests} và {@code DeviceSessionProviderTests} đều dựng đối tượng bằng
 * {@code new} với mock, nên không có proxy, không có giao dịch, và listener không bao giờ chạy.
 *
 * <p>Lớp này CỐ Ý không {@code @Transactional}: một test bọc trong giao dịch rollback thì không bao
 * giờ commit, mà không commit thì pha AFTER_COMMIT không xảy ra -- test sẽ xanh mà chẳng kiểm được
 * gì.
 *
 * <p>Mỗi test dùng {@code userId} và {@code installationId} sinh mới, nên các dòng còn sót lại
 * không ảnh hưởng lẫn nhau; schema vốn cũng bị dựng lại theo vòng đời context.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestGrpcTransport
// application-test.yaml ghim pool ở 2, mà một lần đăng xuất giữ tới BA kết nối cùng lúc: giao dịch
// ngoài của LogoutUseCase, giao dịch REQUIRES_NEW của revoke (giao dịch ngoài bị treo nhưng vẫn giữ
// kết nối), rồi giao dịch REQUIRES_NEW của listener -- kết nối của revoke chưa được trả lại lúc
// callback AFTER_COMMIT chạy, vì Spring chỉ trả ở cleanupAfterCompletion. Với pool 2 thì listener
// chờ hết 30 giây rồi chết vì "Connection is not available", chứ không phải vì lỗi giao dịch.
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=5")
class LogoutNotificationDeviceCleanUpTests extends ContainerTestConfig {

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
     * Rỗng = người gọi không mang access token còn hạn. Đó là trạng thái của một phiên bị bỏ quên
     * và là đường đi mà {@link LogoutUseCase} chỉ còn cookie làm bằng chứng -- cũng là đường ngắn
     * nhất tới listener đang được kiểm ở đây.
     */
    @MockitoBean
    private UserContextPort userContextPort;

    private UUID userId;
    private UUID sessionId;
    /** Sinh mới mỗi test: {@code refresh_tokens.token_hash} có unique index (idx_refresh_token_hash). */
    private String rawRefreshToken;

    @BeforeEach
    void setUp() {
        when(userContextPort.findCurrentAuthenticatedUserId()).thenReturn(Optional.empty());

        userId = UUID.randomUUID();
        rawRefreshToken = "raw-refresh-token-" + UUID.randomUUID();
        var now = Instant.now();

        sessionId = transactionTemplate.execute(status -> deviceSessionRepository.save(new DeviceSession(
            userId, DEVICE_ID, "Lab PC", SessionPlatform.WEB,
            "203.0.113.10", "JUnit User Agent", null)).getId());

        transactionTemplate.executeWithoutResult(status -> refreshTokenRepository.save(
            RefreshToken.createFresh(sessionId, sessionTokenManagerPort.hash(rawRefreshToken), now)));

        registerNotificationDevice(DEVICE_ID);
    }

    @Test
    void should_remove_the_notification_device_of_the_logged_out_device() {
        logoutUseCase.execute(new LogoutCommand(rawRefreshToken, DEVICE_ID));

        // Phiên bị thu hồi là điều kiện để listener chạy -- kiểm luôn để một ngày nào đó test này
        // hỏng vì logout không còn thu hồi được gì thì không bị đọc nhầm thành "đã gỡ thiết bị".
        assertThat(deviceSessionRepository.findById(sessionId).orElseThrow().isRevoked()).isTrue();
        assertThat(notificationDeviceRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void should_leave_notification_devices_of_other_devices_alone() {
        registerNotificationDevice(OTHER_DEVICE_ID);

        logoutUseCase.execute(new LogoutCommand(rawRefreshToken, DEVICE_ID));

        assertThat(notificationDeviceRepository.findByUserId(userId))
            .extracting(NotificationDevice::getDeviceId)
            .containsExactly(OTHER_DEVICE_ID);
    }

    private void registerNotificationDevice(String deviceId) {
        var now = Instant.now();
        // installationId là duy nhất trên toàn bảng (ON CONFLICT của registerDevice dựa vào đó),
        // nên phải sinh mới cho từng dòng chứ không dùng hằng số.
        var installationId = UUID.randomUUID().toString();
        transactionTemplate.executeWithoutResult(status -> notificationDeviceRepository.save(
            NotificationDevice.create(
                userId, deviceId, NotificationDevicePlatform.WEB, installationId, now)));
    }
}
