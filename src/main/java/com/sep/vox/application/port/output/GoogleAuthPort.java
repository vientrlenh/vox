package com.sep.vox.application.port.output;

public interface GoogleAuthPort {
    GoogleUserInfo verifyToken(String idToken);

    // Record chứa dữ liệu trả về từ Google
    record GoogleUserInfo(String email, String fullName, String pictureUrl, boolean emailVerified) {}
}