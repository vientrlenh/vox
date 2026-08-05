package com.sep.vox.domain.model.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Ranh giới giữa hai nhóm nghiệp vụ đối lập, và là lý do tồn tại của {@code isOnlineGateway()}:
 *
 * <ul>
 *   <li>Cổng trực tuyến: tạo được link, có mã đơn để đối soát ngược, chỉ PAID khi cổng xác nhận.</li>
 *   <li>Thu ngoài hệ thống: ghi nhận PAID ngay bởi con người, không có mã đơn nào để đối soát.</li>
 * </ul>
 *
 * <p>Trộn hai nhóm này tạo ra bản ghi tài chính tự mâu thuẫn — vd {@code payment_method='PAYOS'}
 * kèm {@code provider_order_ref=NULL}, không tra ngược được sang dashboard cổng nhưng vẫn nằm lẫn
 * giữa các giao dịch cổng thật trong báo cáo.
 */
class PaymentMethodTests {

    @Test
    void classifiesGatewaysAndOfflineCollectionApart() {
        assertThat(PaymentMethod.PAYOS.isOnlineGateway()).isTrue();
        assertThat(PaymentMethod.SEPAY.isOnlineGateway()).isTrue();
        assertThat(PaymentMethod.MANUAL.isOnlineGateway()).isFalse();
    }

    @Test
    void resolveAcceptsKnownMethods() {
        assertThat(PaymentMethod.resolve("PAYOS")).isEqualTo(PaymentMethod.PAYOS);
        assertThat(PaymentMethod.resolve("MANUAL")).isEqualTo(PaymentMethod.MANUAL);
    }

    @Test
    void resolveRejectsUnknownMethodAndKeepsCause() {
        assertThatThrownBy(() -> PaymentMethod.resolve("MOMO"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MOMO")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveOnlineGatewayAcceptsGateways() {
        assertThat(PaymentMethod.resolveOnlineGateway("PAYOS")).isEqualTo(PaymentMethod.PAYOS);
        assertThat(PaymentMethod.resolveOnlineGateway("SEPAY")).isEqualTo(PaymentMethod.SEPAY);
    }

    /** Chốt chặn cho 3 use case tạo link: MANUAL không bao giờ tạo được link thanh toán. */
    @Test
    void resolveOnlineGatewayRejectsOfflineCollection() {
        assertThatThrownBy(() -> PaymentMethod.resolveOnlineGateway("MANUAL"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MANUAL");
    }
}
