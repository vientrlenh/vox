package com.sep.vox.infrastructure.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sep.vox.application.exception.UnauthorizedException;

@Component
public class GoogleIdTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierService(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId)
            throws GeneralSecurityException, IOException {
        this.verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verify(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new UnauthorizedException("Google ID token không hợp lệ");
        }
        if (idToken == null) {
            throw new UnauthorizedException("Google ID token không hợp lệ");
        }

        var payload = idToken.getPayload();
        if (payload.getEmailVerified() == null || !payload.getEmailVerified()) {
            throw new UnauthorizedException("Người dùng chưa được xác thực để đăng nhập");
        }
        return payload;
    }
}
