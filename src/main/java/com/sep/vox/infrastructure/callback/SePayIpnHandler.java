package com.sep.vox.infrastructure.callback;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sep.vox.application.port.input.service.CallbackHandlerService;

@Component("sepayCallback")
public class SePayIpnHandler implements CallbackHandlerService {

    @Override
    public void handle(byte[] rawBody, Map<String, String> headers) {
        // TODO(Phase 3): sau khi SePayPaymentProcessService.verifyCallback hoàn thiện, thân hàm này
        // sẽ trùng khớp hoàn toàn với PayOSWebhookHandler (verify -> tra invoice -> đối chiếu tiền
        // -> settle). Lúc đó gộp cả hai về một ProcessPaymentCallbackUseCase nhận PaymentMethod,
        // và bỏ luôn CallbackHandlerService cùng cơ chế tra handler theo tên bean.
        throw new UnsupportedOperationException("Chưa hỗ trợ callback SePay");
    }

}
