package com.sep.vox.domain.dto;

import com.sep.vox.domain.model.invoice.Invoice;

/**
 * Chứng từ của một đơn đã thu đủ tiền. Chỉ có hai trường, và đó là toàn bộ những gì hóa đơn tự nó
 * biết -- số tiền, mua gì, trả bằng cách nào đều nằm ở Order và PaymentRecord. Vì vậy DTO này KHÔNG
 * đứng một mình được: nó luôn xuất hiện như một trường của đơn (xem {@code Order.invoice}).
 *
 * <p>KHÔNG có {@code orderId}: hóa đơn nằm lồng trong chính đơn của nó, chép lại id của cha là thừa.
 * Đây là chỗ khác {@link OrderItemDto} -- dòng hàng phải giữ {@code orderId} vì batch loader gom
 * nhóm theo nó, còn hóa đơn là quan hệ 1-1 nên gom bằng chính domain model trước khi map sang DTO.
 *
 * <p>KHÔNG có {@code paymentId}: người đọc không cần một con trỏ, họ cần các dữ kiện của lần thanh
 * toán -- {@code Order.payments} trả lời việc đó.
 *
 * <p>KHÔNG có {@code id}: {@code invoiceNumber} vốn đã duy nhất và là thứ người ta đọc cho nhau qua
 * điện thoại, nên nếu sau này có đường tải chứng từ thì khóa của nó nên là số hóa đơn
 * ({@code InvoiceRepository.findByInvoiceNumber} đã sẵn) chứ không phải một UUID.
 */
public record InvoiceDto(
    String invoiceNumber,
    String issueDate
) {

    public static InvoiceDto toDto(Invoice invoice) {
        return new InvoiceDto(
            invoice.getInvoiceNumber(),
            invoice.getIssueDate() == null ? null : invoice.getIssueDate().toString()
        );
    }
}
