package com.sep.vox.application.port.input.service;

import java.util.Map;

// Nhận raw bytes chứ không phải payload đã parse: chữ ký/secret của cổng phải được xác thực trên
// đúng dữ liệu nhận được, và việc hiểu định dạng payload là việc của adapter từng cổng.
public interface CallbackHandlerService {
    void handle(byte[] rawBody, Map<String, String> headers);
}
