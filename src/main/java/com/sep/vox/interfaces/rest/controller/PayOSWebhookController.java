package com.sep.vox.interfaces.rest.controller;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.usecase.subscription.HandlePayOSWebhookUseCase;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1/webhooks/payos")
public class PayOSWebhookController {

    private final HandlePayOSWebhookUseCase handlePayOSWebhookUseCase;
    private final JsonMapper jsonMapper;

    public PayOSWebhookController(HandlePayOSWebhookUseCase handlePayOSWebhookUseCase, JsonMapper jsonMapper) {
        this.handlePayOSWebhookUseCase = handlePayOSWebhookUseCase;
        this.jsonMapper = jsonMapper;
    }

    // Đọc raw bytes + decode UTF-8 kiểu lenient (thay ký tự lỗi thay vì crash) vì payload test/webhook
    // của PayOS đôi khi chứa byte UTF-8 không hợp lệ ở phần text tiếng Việt (desc), làm Jackson mặc định
    // ném StreamReadException nếu bind thẳng qua @RequestBody Map.
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> handleWebhook(@RequestBody byte[] rawBody) {
        var decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        String json;
        try {
            json = decoder.decode(ByteBuffer.wrap(rawBody)).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Payload webhook không hợp lệ");
        }

        Map<String, Object> body = jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        handlePayOSWebhookUseCase.execute(body);
        return ResponseEntity.ok(ApiResponse.success("OK"));
    }
}