package com.sep.vox.infrastructure.shared;

import java.security.SecureRandom;
import java.util.stream.Collectors;

public class SecureRandomGenerator {

    private SecureRandomGenerator() {}

    private static final SecureRandom sr = new SecureRandom();
    
    public static String generateRaw(String seed, int length) {
        return sr.ints(length, 0, seed.length())
            .mapToObj(seed::charAt)
            .map(o -> o.toString())
            .collect(Collectors.joining());

    }
}
