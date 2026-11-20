package com.sep.vox.application.response.input.subscription;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;

/**
 * MỘT TRANG của màn chia hạn mức cá nhân, kèm hai con số tổng cho cả tập.
 *
 * <p>Thay cho {@code QuotaUserAllocationSummaryResponse} vốn trả về TOÀN BỘ người đủ điều kiện trong
 * một lượt: một trường có vài nghìn học sinh thì đó là vài nghìn dòng cho mỗi lần mở trang, và giao
 * diện cũng không dựng nổi ngần ấy hàng.
 *
 * <p><b>Vì sao phải có {@code distributedAmountVnd} ở đây.</b> Trước đây giao diện tự cộng cột phân
 * bổ của mọi dòng để biết trường đã chia hết bao nhiêu và còn lại bao nhiêu. Khi chỉ còn một trang
 * trong tay thì phép cộng đó cho ra số SAI -- nên tổng phải do phía máy chủ tính trên toàn bộ tập,
 * cùng khuôn với {@code SchoolBalanceSummaryResponse}.
 *
 * @param pool                   ví hạn mức cấp trường -- EXAM khi chia cho giáo viên, PRACTICE khi
 *                               chia cho học sinh
 * @param distributedAmountVnd   tổng đã chia cho những người CÒN đủ điều kiện, trên toàn bộ tập chứ
 *                               không phải chỉ trang này. Người đã nghỉ/ra trường KHÔNG được cộng vào
 *                               -- xem {@code orphanedAmountVnd}
 * @param orphanedAmountVnd      phần đang đứng tên những người KHÔNG còn đủ điều kiện (đã vô hiệu
 *                               hoá, rời trường, đổi vai trò). Không tính vào trần và không hiện ở
 *                               bất kỳ trang nào của bảng, nên phải nói thành lời ở đây: nếu không,
 *                               tổng trên màn hình và tổng các dòng lệch nhau mà không ai giải thích
 *                               được. Thường là 0
 * @param distributableRatio     trần phân phối của trường cho loại hạn mức này, 0..1
 * @param distributableAmountVnd phần ví được phép chia ra = pool x distributableRatio. Trả sẵn thay
 *                               vì để client tự nhân: đây là con số mà backend dùng để từ chối, nên
 *                               một phép nhân thứ hai ở client là một cơ hội để hai bên lệch nhau
 *                               vài phần triệu đồng rồi báo lỗi ở chỗ người dùng không hiểu nổi
 * @param spendableFundsVnd      số tiền trường THẬT SỰ còn trả được cho loại hạn mức này = hạn mức
 *                               kèm gói còn lại + số dư ví tự nạp (kẹp 0). KHÁC HẲN
 *                               distributableAmountVnd, vốn tính trên total_allocated và không hề
 *                               giảm khi ví bị tiêu: một trường đã tiêu cạn ví vẫn chia tiếp được
 *                               trong trần, và trần đó không nói lên điều gì về khả năng chi trả.
 *                               Giao diện dùng nó để CẢNH BÁO, không phải để chặn -- trần chi là
 *                               trần chi, còn chặn thì đã có cửa chặn thật lúc mở kỳ thi / dựng đề
 *                               luyện (ClassTestTokenQuotaGuardService, findPracticeSpendableFundsVnd)
 * @param walletBalanceVnd       phần ví TỰ NẠP của trường (school_balances) CÓ THỂ ăn thêm ngoài
 *                               distributableAmountVnd khi nới trần cá nhân của MỘT người -- không
 *                               phải một túi tiền dành riêng, và dùng CHUNG cho cả EXAM lẫn PRACTICE
 *                               (xem WalletDrawConfirmationRequiredException). Đã kẹp về 0, âm là nợ.
 * @param content                người đủ điều kiện ở trang đang xem, kể cả người chưa được chia gì
 */
public record QuotaUserAllocationPageResponse(
    SchoolSubscriptionQuotaRecordDto pool,
    BigDecimal distributedAmountVnd,
    BigDecimal orphanedAmountVnd,
    BigDecimal distributableRatio,
    BigDecimal distributableAmountVnd,
    BigDecimal spendableFundsVnd,
    BigDecimal walletBalanceVnd,
    List<Row> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    /**
     * Một người trong danh sách chia hạn mức.
     *
     * <p>KHÔNG mang tên người dùng: tên được nối vào ở tầng GraphQL qua data loader {@code userById},
     * nên một trang 20 dòng chỉ tốn thêm ĐÚNG MỘT truy vấn users thay vì 20. Đây cũng là chỗ sửa lỗi
     * cột "Họ tên" luôn hiện dấu gạch: {@code SchoolSubscriptionQuotaUserAllocationDto} chưa bao giờ
     * có trường tên, còn client thì khai sẵn một trường như thế và không bao giờ nhận được giá trị.
     *
     * <p><b>{@code allocatedAmountVnd} null = CHƯA CHIA, khác hẳn 0.</b> Hai trạng thái này cho ra
     * hành vi NGƯỢC NHAU và trước đây bị gộp thành cùng một số 0 trên màn hình:
     *
     * <ul>
     *   <li>chưa có dòng phân bổ (null) = KHÔNG bị chặn theo cá nhân. Với giáo viên nghĩa là tiêu
     *       thoải mái trong ví trường ({@code ClassTestTokenQuotaGuardService.remainingUserAllocation}
     *       trả null thì cửa chặn bỏ qua luôn); với học sinh thì ngược lại, LEAST(...) trong
     *       {@code findPracticeSpendableFundsVnd} kéo về 0 nên em không luyện được lượt nào.</li>
     *   <li>có dòng và bằng 0 = bị chặn hẳn, ở cả hai loại.</li>
     * </ul>
     *
     * <p>Nên một trường có nửa số giáo viên "0 ₫" có thể đang là nửa số giáo viên KHÔNG có trần chi
     * nào cả -- và trên bảng cũ thì hai ca đó trông y hệt nhau. Client phải hiện thành hai nhãn khác
     * nhau ("Chưa phân bổ" vs "0 ₫"), nên kiểu dữ liệu ở đây phải phân biệt được chúng.
     *
     * <p>{@code usedAmountVnd} thì luôn có số: chưa có dòng nghĩa là chưa tiêu đồng nào qua trần cá
     * nhân, và 0 ở đó không mơ hồ.
     */
    public record Row(
        UUID userId,
        BigDecimal allocatedAmountVnd,
        BigDecimal usedAmountVnd
    ) {
    }
}
