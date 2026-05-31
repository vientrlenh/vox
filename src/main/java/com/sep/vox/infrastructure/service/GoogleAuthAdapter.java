package com.sep.vox.infrastructure.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.GoogleAuthPort;

// ĐÂY LÀ IMPORT ĐÚNG CỦA SPRING BOOT
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleAuthAdapter implements GoogleAuthPort {

    private final GoogleIdTokenVerifier verifier;

    // Đặt @Value đúng vị trí và đúng ngoặc tròn
    public GoogleAuthAdapter(@Value("${app.google.client-id}") String clientId) {
        // Khởi tạo trình xác minh Token, chỉ chấp nhận token sinh ra bởi Client ID của dự án VOX
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleUserInfo verifyToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                return new GoogleUserInfo(
                        payload.getEmail(),
                        (String) payload.get("name"),
                        (String) payload.get("picture"),
                        payload.getEmailVerified()
                );
            } else {
                throw new UnauthorizedException("Google ID Token không hợp lệ hoặc đã hết hạn.");
            }
        } catch (Exception e) {
            throw new UnauthorizedException("Lỗi xác thực Google Token: " + e.getMessage());
        }
    }
}