package com.sep.vox.application.response.output;

public record SessionToken(
    String rawToken, 
    String hashedToken, 
    long expirationMs
) {
    
}
