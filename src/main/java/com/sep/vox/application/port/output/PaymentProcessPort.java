package com.sep.vox.application.port.output;

import java.math.BigDecimal;
import java.util.Map;

import com.sep.vox.application.response.output.PaymentLinkRemoteStatus;
import com.sep.vox.application.response.output.PaymentLinkResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.subscription.PaymentMethod;

// Hợp đồng chung cho mọi cổng thanh toán. Nguyên tắc: không có kiểu dữ liệu hay quy ước nào của
// một cổng cụ thể được rò rỉ qua interface này — mã đơn là chuỗi (không phải orderCode dạng số
// riêng của PayOS), và việc bóc payload callback nằm hẳn bên trong adapter chứ không phải ở
// tầng application.
public interface PaymentProcessPort {

    // returnUrl/cancelUrl cố tình KHÔNG nằm ở đây: mỗi adapter tự đọc từ config của mình, vì
    // hiện chưa có luồng nào cần URL khác nhau theo từng hóa đơn.
    record CreatePaymentLinkCommand(
        String orderRef,
        BigDecimal amount,
        String description
    ) {
    }

    // Kết quả đã chuẩn hoá của một callback: adapter chịu trách nhiệm xác thực chữ ký VÀ dịch
    // payload riêng của cổng về đây, nên handler ở tầng trên dùng chung được cho mọi cổng.
    // amount để đối chiếu với số tiền trên hóa đơn trước khi chốt; null nếu cổng không gửi kèm.
    record CallbackVerificationResult(
        boolean valid,
        String providerOrderRef,
        PaymentLinkRemoteStatus status,
        BigDecimal amount
    ) {
        public static CallbackVerificationResult invalid() {
            return new CallbackVerificationResult(false, null, null, null);
        }
    }

    // Cổng mà adapter này phục vụ — PaymentProcessResolver dùng để lập chỉ mục, thay cho việc
    // tra theo tên bean dạng chuỗi.
    PaymentMethod provider();

    PaymentLinkResult createPaymentLink(CreatePaymentLinkCommand command);

    PaymentLinkStatusResult getPaymentLinkStatus(String providerOrderRef);

    // Nhận raw bytes chứ không phải Map đã parse: nhiều cổng ký HMAC trên đúng chuỗi byte của
    // body, nên chỉ cần đi qua một vòng parse/serialize là chữ ký không còn khớp. headers để
    // phục vụ các cổng đặt chữ ký ở header thay vì trong body.
    CallbackVerificationResult verifyCallback(byte[] rawBody, Map<String, String> headers);
}
