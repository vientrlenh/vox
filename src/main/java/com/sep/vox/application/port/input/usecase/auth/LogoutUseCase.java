package com.sep.vox.application.port.input.usecase.auth;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.auth.LogoutCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.SessionManagerPort;
import com.sep.vox.application.port.output.SessionTokenManagerPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.DeviceSessionRepository;
import com.sep.vox.domain.repository.RefreshTokenRepository;

/**
 * Revokes the device session when the user logs out.
 *
 * <p>Before this use case existed, {@link SessionManagerPort#revoke} was only called from
 * {@link RefreshUseCase} when it detected an anomaly (a reused refresh token, a mismatched deviceId).
 * That means a NORMAL logout revoked nothing: the client only deleted the token on the device, while the
 * server-side session lived on for the full 72 hours of the refresh_token cookie TTL -- whoever picked up
 * that cookie could still exchange it for a new access token. On a shared lab computer that is the session
 * of the student who just left the seat.
 *
 * <p>Does NOT throw when there is no session to revoke. Logout is a one-way operation: the client deletes
 * the token on the device whatever the server answers, so an exception here would only produce the worst
 * state -- the client has lost its token while the server keeps the session alive, and nobody can call
 * again to close it. Calling it a second time on an already revoked session is harmless too: the UPDATE
 * filters on {@code revokedAt IS NULL}, so it returns 0 rows and {@code DeviceSessionRevokedEvent} is not
 * emitted again.
 *
 * <p>Two sources for finding the session, deliberately MERGED rather than choosing one:
 * <ul>
 *   <li>The refresh_token cookie -- the most precise, because it is exactly the credential being returned,
 *       and the ONLY source still usable once the access token has expired (15 minutes,
 *       JWT_EXPIRATION_MS). An abandoned session -- the one that most needs revoking -- is almost always in
 *       that state.</li>
 *   <li>(logged-in userId, deviceId) -- catches sessions the cookie no longer points to.
 *       {@link LoginUseCase} creates a NEW DeviceSession on EVERY login, even on the same device, so one
 *       device can carry several live sessions at once. Logging out on that device must clean up all of
 *       them; any one left behind stays alive with no way for the user to see it and turn it off.</li>
 * </ul>
 *
 * <p>An access token that has ALREADY been issued cannot be revoked: {@code JwtAuthTokenProvider} puts no
 * session claim in the token and {@code JwtAuthenticationFilter} only checks the signature, so it stays
 * usable for up to 15 minutes after logout. Closing that gap completely would require adding a session claim
 * to the token and checking the revocation state on EVERY request -- a DB read for the whole API, far more
 * expensive than what it buys. The client side compensates: both web and desktop delete the access token
 * as soon as the user logs out.
 */
@Service
@RequiredArgsConstructor
public class LogoutUseCase implements IUseCase<LogoutCommand, Void> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceSessionRepository deviceSessionRepository;
    private final SessionTokenManagerPort sessionTokenManagerPort;
    private final SessionManagerPort sessionManagerPort;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Void execute(LogoutCommand input) {
        LogoutCommand command = normalize(input);
        Instant now = Instant.now();

        // LinkedHashSet: the two sources below usually point to the same session, and calling revoke twice
        // for the same sessionId opens a redundant REQUIRES_NEW transaction that does nothing.
        Set<UUID> sessionIds = new LinkedHashSet<UUID>();
        findSessionByRefreshToken(command.refreshToken()).ifPresent(sessionIds::add);
        sessionIds.addAll(findLiveSessionsOnDevice(command.deviceId()));

        for (UUID sessionId : sessionIds) {
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
     * Uses {@code findByTokenHash} rather than the {@code ForUpdate} variant that {@link RefreshUseCase} uses:
     * nothing is written to the token row here, so a pessimistic lock would only make /logout and /refresh wait
     * for each other. A race is harmless anyway -- the old token and the freshly rotated one both point to the
     * SAME sessionId, so revoking the session blocks both.
     *
     * <p>A token that resolves to nothing (an old cookie, a cookie from another environment) returns empty
     * rather than an error: see the class Javadoc for the reason.
     */
    private Optional<UUID> findSessionByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = sessionTokenManagerPort.hash(refreshToken);
        return refreshTokenRepository.findByTokenHash(tokenHash)
            .map(rt -> rt.getSessionId());
    }

    /**
     * Empty when the request carries no valid access token -- then the caller is unknown, and guessing by
     * deviceId alone would let any device revoke another person's session that has the same deviceId. The
     * refresh_token cookie still covers the revocation in that case.
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
