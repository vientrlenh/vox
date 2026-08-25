package com.sep.vox.application.exception;

public class QuotaExceededException extends RuntimeException {

    /** Ví nào cạn -- SCHOOL (hạn mức chung của trường) hay PERSONAL (phần trường cấp riêng cho
     * học sinh này). Null khi lỗi không gắn với một ví cụ thể (vd guard "không đủ cho 1 câu"
     * trước khi vào phiên, xem BuildPracticePaperUseCase). */
    public enum Scope { SCHOOL, PERSONAL }

    private final Scope scope;

    public QuotaExceededException(String message) {
        this(message, null);
    }

    public QuotaExceededException(String message, Scope scope) {
        super(message);
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }
}
