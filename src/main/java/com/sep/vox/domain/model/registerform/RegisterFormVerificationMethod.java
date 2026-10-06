package com.sep.vox.domain.model.registerform;

public enum RegisterFormVerificationMethod {
    DOMAIN_OTP, 
    DOCUMENT;

    public static RegisterFormVerificationMethod from(String method) {
        return method == null ? null : RegisterFormVerificationMethod.valueOf(method);
    }

    public static String value(RegisterFormVerificationMethod method) {
        return method == null ? null : method.name();
    }
}
