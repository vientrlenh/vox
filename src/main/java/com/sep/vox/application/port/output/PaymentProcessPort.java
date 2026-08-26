package com.sep.vox.application.port.output;

import java.util.Map;

import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.payment.PaymentProvider;

// Hợp đồng chung cho mọi cổng thanh toán. Nguyên tắc: không có kiểu dữ liệu hay quy ước nào của
// một cổng cụ thể được rò rỉ qua interface này — mã đơn là chuỗi (không phải orderCode dạng số
// riêng của PayOS), và việc bóc payload callback nằm hẳn bên trong adapter chứ không phải ở
// tầng application.
public interface PaymentProcessPort {

    // Cổng mà adapter này phục vụ — PaymentProcessResolver dùng để lập chỉ mục, thay cho việc
    // tra theo tên bean dạng chuỗi. Là PaymentProvider (PAYOS/SEPAY) chứ không phải PaymentMethod:
    // method giờ mang nghĩa người trả đã trả BẰNG GÌ (E_BANKING/CARD), không phải trả QUA ĐÂU.
    PaymentProvider provider();

    /**
     * Sinh mã đơn theo đúng quy ước của cổng. PayOS bắt buộc orderCode là số, SePay dùng chuỗi ở
     * order_invoice_number -- không cổng nào ép được cổng kia nên quyết định thuộc về adapter.
     *
     * <p>Không còn nhận invoiceNumber: hóa đơn giờ chỉ phát SAU khi tiền về, nên lúc phát link chưa
     * có số hóa đơn nào tồn tại. Đường đi ngược từ mã về đơn nằm ở payment_records.provider_order_ref.
     *
     * <p>MỖI LẦN GỌI PHẢI RA MỘT MÃ MỚI, kể cả cho cùng một đơn: PayOS từ chối orderCode trùng và
     * SePay đòi order_invoice_number duy nhất, nên lần thử lại luôn là một mã mới chứ không dùng lại.
     */
    String newOrderRef();

    PaymentCheckoutResult createPaymentLink(CreatePaymentLinkCommand command);

    PaymentLinkStatusResult getPaymentLinkStatus(String providerOrderRef);

    // Nhận raw bytes chứ không phải Map đã parse: nhiều cổng ký HMAC trên đúng chuỗi byte của
    // body, nên chỉ cần đi qua một vòng parse/serialize là chữ ký không còn khớp. headers để
    // phục vụ các cổng đặt chữ ký ở header thay vì trong body.
    CallbackVerificationResult verifyCallback(byte[] rawBody, Map<String, String> headers);
}
