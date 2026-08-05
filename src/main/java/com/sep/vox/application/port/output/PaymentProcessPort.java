package com.sep.vox.application.port.output;

import java.util.Map;

import com.sep.vox.application.response.output.CallbackVerificationResult;
import com.sep.vox.application.response.output.CreatePaymentLinkCommand;
import com.sep.vox.application.response.output.PaymentCheckoutResult;
import com.sep.vox.application.response.output.PaymentLinkStatusResult;
import com.sep.vox.domain.model.subscription.PaymentMethod;

// Hợp đồng chung cho mọi cổng thanh toán. Nguyên tắc: không có kiểu dữ liệu hay quy ước nào của
// một cổng cụ thể được rò rỉ qua interface này — mã đơn là chuỗi (không phải orderCode dạng số
// riêng của PayOS), và việc bóc payload callback nằm hẳn bên trong adapter chứ không phải ở
// tầng application.
public interface PaymentProcessPort {

    // Cổng mà adapter này phục vụ — PaymentProcessResolver dùng để lập chỉ mục, thay cho việc
    // tra theo tên bean dạng chuỗi.
    PaymentMethod provider();

    // Sinh mã đơn theo đúng quy ước của cổng, từ số hóa đơn nội bộ. Trước đây use case tự sinh một
    // mã dạng số kiểu PayOS rồi ép mọi cổng dùng chung — nhưng SePay PG định danh đơn bằng
    // order_invoice_number dạng chuỗi và đã có sẵn invoiceNumber để dùng thẳng, còn PayOS thì bắt
    // buộc orderCode phải là số. Không cổng nào ép được cổng kia, nên quyết định thuộc về adapter.
    String newOrderRef(String invoiceNumber);

    PaymentCheckoutResult createPaymentLink(CreatePaymentLinkCommand command);

    PaymentLinkStatusResult getPaymentLinkStatus(String providerOrderRef);

    // Nhận raw bytes chứ không phải Map đã parse: nhiều cổng ký HMAC trên đúng chuỗi byte của
    // body, nên chỉ cần đi qua một vòng parse/serialize là chữ ký không còn khớp. headers để
    // phục vụ các cổng đặt chữ ký ở header thay vì trong body.
    CallbackVerificationResult verifyCallback(byte[] rawBody, Map<String, String> headers);
}
