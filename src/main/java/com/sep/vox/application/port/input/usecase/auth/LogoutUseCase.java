package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.LogoutCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;

/**
 * Thu hồi phiên thiết bị khi người dùng đăng xuất.
 *
 * <p>Trước khi có use case này, {@link SessionManagerPort#revoke} chỉ được gọi từ
 * {@link RefreshUseCase} lúc phát hiện bất thường (refresh token bị dùng lại, deviceId lệch).
 * Nghĩa là đăng xuất BÌNH THƯỜNG không thu hồi gì cả: client chỉ xoá token trong máy, còn phiên
 * phía server sống trọn 72 giờ theo TTL của cookie refresh_token -- ai nhặt được cookie đó vẫn
 * đổi ra access token mới được. Trên máy phòng lab dùng chung thì đó là phiên của học sinh vừa
 * rời ghế.
 *
 * <p>KHÔNG ném lỗi khi không tìm thấy phiên nào để thu hồi. Đăng xuất là thao tác một chiều:
 * client xoá token trong máy bất kể server trả gì, nên một ngoại lệ ở đây chỉ tạo ra đúng tình
 * trạng tệ nhất -- client mất token trong khi server vẫn giữ phiên sống, và không còn ai gọi
 * lại được nữa. Gọi lần hai trên phiên đã thu hồi cũng vô hại: câu UPDATE lọc
 * {@code revokedAt IS NULL} nên trả 0 dòng và {@code DeviceSessionRevokedEvent} không phát lại.
 *
 * <p>Hai nguồn tìm phiên, cố ý GỘP chứ không phải chọn một:
 * <ul>
 *   <li>Cookie refresh_token -- chính xác nhất vì đó đúng là credential đang được trả lại, và là
 *       nguồn DUY NHẤT còn dùng được khi access token đã hết hạn (15 phút, JWT_EXPIRATION_MS).
 *       Phiên bị bỏ quên -- thứ cần thu hồi nhất -- gần như luôn ở trạng thái đó.</li>
 *   <li>(userId đang đăng nhập, deviceId) -- vớt những phiên mà cookie không còn trỏ tới.
 *       {@link LoginUseCase} tạo một DeviceSession MỚI ở MỖI lần đăng nhập, kể cả trên cùng một
 *       thiết bị, nên một máy có thể đang mang nhiều phiên sống cùng lúc. Đăng xuất trên máy đó
 *       phải dọn hết; bỏ sót cái nào thì nó tiếp tục sống mà người dùng không có đường nào thấy
 *       để tự tắt.</li>
 * </ul>
 *
 * <p>Access token ĐÃ phát thì không thu hồi được: {@code JwtAuthTokenProvider} không đặt claim
 * phiên nào vào token và {@code JwtAuthenticationFilter} chỉ kiểm chữ ký, nên nó vẫn dùng được
 * tối đa 15 phút sau khi đăng xuất. Đóng hẳn khoảng đó đòi hỏi thêm claim phiên vào token và tra
 * trạng thái thu hồi ở MỌI request -- một lượt đọc DB cho toàn bộ API, đắt hơn nhiều so với thứ
 * nó mua được. Bù lại phía client: cả web lẫn desktop đều xoá access token ngay khi đăng xuất.
 */
@Service
public class LogoutUseCase implements IUseCase<LogoutCommand, Void> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final SessionManagerPort sessionManagerPort;
    private final UserContextPort userContextPort;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository, DeviceSessionRepository deviceSessionRepository, SessionTokenManagerPort sessionTokenManagerPort, SessionManagerPort sessionManagerPort, UserContextPort userContextPort) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.sessionTokenManagerPort = sessionTokenManagerPort;
        this.sessionManagerPort = sessionManagerPort;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(LogoutCommand input) {
        var command = normalize(input);
        var now = Instant.now();

        // LinkedHashSet: hai nguồn bên dưới thường trỏ về cùng một phiên, và gọi revoke hai lần
        // cho cùng sessionId là mở thừa một transaction REQUIRES_NEW không làm gì.
        var sessionIds = new LinkedHashSet<UUID>();
        findSessionByRefreshToken(command.refreshToken()).ifPresent(sessionIds::add);
        sessionIds.addAll(findLiveSessionsOnDevice(command.deviceId()));

        for (var sessionId : sessionIds) {
            sessionManagerPort.revoke(sessionId, now);
        }

        return null;
    }

    private LogoutCommand normalize(LogoutCommand input) {
        return new LogoutCommand(
            StringNormalization.trimAndCollapseSpaces(input.refreshToken()),
            StringNormalization.trimAndCollapseSpaces(input.deviceId())
        );
    }

    /**
     * Dùng {@code findByTokenHash} chứ không phải bản {@code ForUpdate} như
     * {@link RefreshUseCase}: ở đây không ghi gì lên dòng token nên khoá bi quan chỉ tạo thêm chỗ
     * cho /logout và /refresh chờ nhau. Có chạy đua thì cũng vô hại -- token cũ và token vừa xoay
     * đều trỏ về CÙNG một sessionId, nên thu hồi phiên chặn được cả hai.
     *
     * <p>Token không tra ra gì (cookie cũ, cookie của môi trường khác) trả về rỗng chứ không phải
     * lỗi: xem lý do ở javadoc của lớp.
     */
    private Optional<UUID> findSessionByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }

        var tokenHash = sessionTokenManagerPort.hash(refreshToken);
        return refreshTokenRepository.findByTokenHash(tokenHash)
            .map(rt -> rt.getSessionId());
    }

    /**
     * Rỗng khi request không mang access token còn hạn -- lúc đó không biết người gọi là ai, và
     * đoán bừa theo mỗi deviceId thì một máy bất kỳ có thể thu hồi phiên của người khác trùng
     * deviceId. Cookie refresh_token vẫn lo phần thu hồi trong trường hợp đó.
     */
    private List<UUID> findLiveSessionsOnDevice(String deviceId) {
        return userContextPort.findCurrentAuthenticatedUserId()
            .map(userId -> deviceSessionRepository.findByUserId(userId).stream()
                .filter(session -> !session.isRevoked())
                .filter(session -> !session.isDeviceIdMismatches(deviceId))
                .map(session -> session.getId())
                .toList())
            .orElseGet(List::of);
    }
}
