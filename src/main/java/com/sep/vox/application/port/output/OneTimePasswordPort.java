package com.sep.vox.application.port.output;

public interface OneTimePasswordPort {
    String generateOtp(int size);
    String hash(String otp);
}
