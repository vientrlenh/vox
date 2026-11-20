package com.sep.vox.application.port.input.service;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;

/**
 * Một gói thay thế có được phép thay cho gói bị lưu trữ hay không.
 *
 * <p>Ràng buộc ở đây chặt bất thường, và lý do nằm ở chỗ trường KHÔNG CÓ QUYỀN TỪ CHỐI: lúc gia hạn,
 * SubscriptionPlanResolver tự đi theo replacedByPlanId và trường nhận gói mới mà không được hỏi. Vì
 * vậy mọi thứ quyết định "trường trả bao nhiêu để nhận được gì" đều phải giữ nguyên hoặc tốt lên --
 * nếu không, System Admin có thể đơn phương hạ chất lượng dịch vụ của một trường đang trả tiền.
 *
 * <p>Tách khỏi ArchiveSubscriptionPlanUseCase vì có HAI đường vào cùng cần đúng bộ luật này: lúc lưu
 * trữ, và lúc sửa lại gói thay thế sau khi đã lưu trữ (UpdateSubscriptionPlanReplacementUseCase).
 * Để mỗi bên tự kiểm là kiểu hôm nào đó sửa một bên quên bên kia.
 */
@Service
public class SubscriptionPlanReplacementValidator {

    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    public SubscriptionPlanReplacementValidator(SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository) {
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
    }

    public void requireValidReplacement(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        if (replacement.getId().equals(replaced.getId())) {
            throw new IllegalArgumentException("Gói thay thế không được trùng với chính gói đang lưu trữ");
        }
        if (replacement.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalArgumentException("Gói thay thế phải đang ở trạng thái hoạt động");
        }

        requireCompatibleTerms(replacement, replaced);
    }

    /**
     * Chỉ phần so điều khoản (chu kỳ/giá/thời lượng/hạn mức) -- KHÔNG đòi replacement đang ACTIVE.
     *
     * <p>Tách riêng cho luồng "tạo gói thay thế" (CreateSubscriptionPlanReplacementUseCase): gói mới
     * tạo ra còn DRAFT nên requireValidReplacement không gọi được ngay; điều khoản chỉ được chốt lúc
     * publish (PublishSubscriptionPlanUseCase), thời điểm gói mới thật sự nhận vai trò thay thế.
     */
    public void requireCompatibleTerms(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        requireSamePeriod(replacement, replaced);
        requireSamePrice(replacement, replaced);
        requireNotLessAttemptTime(replacement, replaced);
        requireNotLessQuota(replacement, replaced);
    }

    /**
     * Cả ĐƠN VỊ lẫn SỐ LƯỢNG chu kỳ. Thiếu vế đơn vị thì một gói "12 DAY" thay được cho gói
     * "12 MONTH" ở đúng giá cũ -- trường trả nguyên tiền và nhận về 1/30 thời hạn.
     */
    private void requireSamePeriod(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        // equals() chứ không phải !=: periodCount là Integer (bọc), nên != so sánh THAM CHIẾU. Hiện
        // nó vẫn chạy đúng chỉ vì Java cache các Integer từ -128..127 -- một gói chu kỳ 200 là sai
        // ngay, mà không có gì báo.
        if (replacement.getPeriodType() != replaced.getPeriodType()
                || !replacement.getPeriodCount().equals(replaced.getPeriodCount())) {
            throw new IllegalArgumentException(
                "Chu kỳ của gói thay thế phải giống hệt gói cần thay thế ("
                    + replaced.getPeriodCount() + " " + replaced.getPeriodType() + ")");
        }
    }

    private void requireSamePrice(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        if (replacement.getPriceVnd().compareTo(replaced.getPriceVnd()) != 0) {
            throw new IllegalArgumentException("Gói thay thế phải có giá bằng đúng giá gói đang cần được thay thế");
        }
    }

    /** Cho phép NHIỀU hơn: nâng chất lượng cho trường thì không cần ai đồng ý. */
    private void requireNotLessAttemptTime(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        if (replacement.getMaxTimePerAttemptMin() < replaced.getMaxTimePerAttemptMin()) {
            throw new IllegalArgumentException(
                "Thời lượng tối đa mỗi lượt của gói thay thế không được thấp hơn gói cần thay thế ("
                    + replaced.getMaxTimePerAttemptMin() + " phút)");
        }
    }

    /**
     * Hạn mức kèm gói là phần GIÁ TRỊ chính của một gói, nên đây là chỗ dễ tụt nhất mà giá vẫn nhìn
     * y hệt. Soi theo TỪNG QuotaType: thiếu hẳn một loại cũng tính là tụt, vì trường đang dùng loại
     * đó sẽ mất sạch hạn mức ở kỳ sau.
     */
    private void requireNotLessQuota(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        var replacementQuotas = subscriptionPlanQuotaRepository
            .findBySubscriptionPlanId(replacement.getId()).stream()
            .collect(Collectors.toMap(q -> q.getQuotaType(), q -> q.getIncludedAmountVnd()));

        for (var replacedQuota : subscriptionPlanQuotaRepository.findBySubscriptionPlanId(replaced.getId())) {
            var replacementAmount = replacementQuotas.get(replacedQuota.getQuotaType());
            if (replacementAmount == null) {
                throw new IllegalArgumentException(
                    "Gói thay thế thiếu hạn mức " + replacedQuota.getQuotaType() + " mà gói cũ đang có");
            }
            if (replacementAmount.compareTo(replacedQuota.getIncludedAmountVnd()) < 0) {
                throw new IllegalArgumentException(
                    "Hạn mức " + replacedQuota.getQuotaType() + " của gói thay thế thấp hơn gói cần thay thế ("
                        + replacedQuota.getIncludedAmountVnd() + " VND)");
            }
        }
    }
}
