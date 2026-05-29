package com.sep.vox.application.port.output;

public interface OneTimePasswordPort {
    String generate(int size);
    String hash(String otp);
}
