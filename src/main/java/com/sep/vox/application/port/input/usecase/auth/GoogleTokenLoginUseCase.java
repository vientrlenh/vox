package com.sep.vox.application.port.input.usecase.auth;

import com.sep.vox.application.response.output.VerifiedGoogleIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.auth.GoogleTokenLoginCommand;
import com.sep.vox.application.port.input.command.auth.OAuth2LoginCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.GoogleIdTokenVerifierPort;
import com.sep.vox.application.response.input.auth.LoginResponse;

/**
 * Google login for the NATIVE app: verifies the ID token, then hands over to
 * {@link OAuth2LoginUseCase}.
 *
 * <p>Deliberately THIN. All of the session building -- looking up the active user, creating the device
 * session, issuing the refresh token, collecting the roles -- already lives in {@link OAuth2LoginUseCase}
 * and is used by the web path every day. Copying it here would create two definitions of "successful
 * login", and the less used one would silently drift away from the other: a fix to the login session
 * rules would be applied to the web and forgotten for native.
 *
 * <p>So this class is responsible for exactly ONE thing the web path does not have to do: replacing the
 * protection that Spring Security provides on its own in the redirect flow. On the web path, by the time
 * {@code OAuth2AuthenticationSuccessHandler} runs the token has already been exchanged and checked by
 * Spring itself; here it has not, so {@link GoogleIdTokenVerifierPort} must do that part first.
 */
@Service
@RequiredArgsConstructor
public class GoogleTokenLoginUseCase implements IUseCase<GoogleTokenLoginCommand, LoginResponse> {

    /** The provider, recorded in the session. Equals the {@code registrationId} of the web path -- the same Google. */
    private static final String PROVIDER = "google";

    private final GoogleIdTokenVerifierPort googleIdTokenVerifierPort;
    private final OAuth2LoginUseCase oAuth2LoginUseCase;

    /**
     * An UNVERIFIED email is rejected right here, before the user lookup.
     *
     * <p>Same rule as {@code CustomOidcUserService} on the web path, and for the same reason: the email is the
     * lookup KEY of {@link OAuth2LoginUseCase}. A Google account without a verified email can carry someone
     * else's address, so dropping this check would turn "log in with Google" into "log in with any email I can
     * type".
     *
     * <p>The error message is kept word for word from the web path -- in the same situation the user must read
     * the same sentence, whether they are in a browser or in the app.
     */
    @Override
    @Transactional
    public LoginResponse execute(GoogleTokenLoginCommand input) {
        VerifiedGoogleIdentity identity = googleIdTokenVerifierPort.verify(input.idToken());

        if (!identity.emailVerified()) {
            throw new UnauthorizedException("The user has not been verified to log in");
        }

        return oAuth2LoginUseCase.execute(new OAuth2LoginCommand(
            PROVIDER,
            identity.subject(),
            identity.email(),
            identity.emailVerified(),
            identity.fullName(),
            identity.avatarUrl(),
            input.ipAddress(),
            input.userAgent(),
            input.device()
        ));
    }
}
