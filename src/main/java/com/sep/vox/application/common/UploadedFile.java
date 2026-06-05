package com.sep.vox.application.common;


public record UploadedFile(
    String fileName, 
    String contentType, 
    long size, 
    byte[] content
) {
    public static UploadedFile upload(String fileName, String contentType, long size, byte[] content) {
        return new UploadedFile(fileName, contentType, size, content);
    }
}
