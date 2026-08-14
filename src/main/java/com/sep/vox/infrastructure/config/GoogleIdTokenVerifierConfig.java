package com.sep.vox.infrastructure.config;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/// `GoogleIdTokenVerifier` xác thực idToken lấy native trên app di động (Google
/// Sign-In SDK, không đi qua luồng redirect `/oauth2/google/start`). Dùng chung
/// `GOOGLE_CLIENT_ID` với `spring.security.oauth2.client.registration.google.client-id`
/// (application.yaml:127) -- cùng client OAuth2, chỉ khác cách lấy token.
///
/// `com.google.api-client:google-api-client` đã có sẵn trên classpath qua
/// `firebase-admin`, không cần thêm dependency mới.
@Configuration
public class GoogleIdTokenVerifierConfig {

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(@Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId)
            throws GeneralSecurityException, IOException {
        return new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

}
