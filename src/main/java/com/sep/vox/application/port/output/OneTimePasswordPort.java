package com.sep.vox.application.port.output;

public interface OneTimePasswordPort {
    String generate(int size);
    boolean verify(String token);
}
