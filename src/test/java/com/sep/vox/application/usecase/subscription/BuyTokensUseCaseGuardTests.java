package com.sep.vox.application.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.BuyTokensCommand;
import com.sep.vox.application.port.input.command.TokenPurchaseItemInput;
import com.sep.vox.application.port.input.usecase.subscription.BuyTokensUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.TokenPurchase;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.InvoiceRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.TokenPurchaseItemRepository;
import com.sep.vox.domain.repository.TokenPurchaseRepository;

/**
 * Use case này cấp quota NGAY và ghi hóa đơn PAID mà không cổng nào xác nhận tiền, nên nó chỉ được
 * phép mang nhãn "thu ngoài hệ thống". Cho gắn nhãn PAYOS/SEPAY ở đây sẽ sinh ra bản ghi
 * {@code payment_method='PAYOS'} kèm {@code provider_order_ref=NULL} — không tra ngược được sang
 * dashboard cổng, mà vẫn lẫn giữa các giao dịch cổng thật trong báo cáo tài chính.
 *
 * <p>Guard phải chặn TRƯỚC khi ghi bất cứ thứ gì: nếu nó nằm sau bước tạo TokenPurchase thì ta
 * phải trông cậy vào rollback, còn ở đây thì đơn giản là không có gì được viết ra.
 */
class BuyTokensUseCaseGuardTests {

    private SchoolSubscriptionRepository schoolSubscriptionRepository;
    private PlanQuotaRepository planQuotaRepository;
    private SubscriptionQuotaRepository subscriptionQuotaRepository;
    private TokenPurchaseRepository tokenPurchaseRepository;
    private TokenPurchaseItemRepository tokenPurchaseItemRepository;
    private InvoiceRepository invoiceRepository;
    private FinancialEventRepository financialEventRepository;
    private UserContextPort userContextPort;
    private BuyTokensUseCase useCase;

    private final UUID schoolId = UUID.randomUUID();
    private final UUID subscriptionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        schoolSubscriptionRepository = mock(SchoolSubscriptionRepository.class);
        planQuotaRepository = mock(PlanQuotaRepository.class);
        subscriptionQuotaRepository = mock(SubscriptionQuotaRepository.class);
        tokenPurchaseRepository = mock(TokenPurchaseRepository.class);
        tokenPurchaseItemRepository = mock(TokenPurchaseItemRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        financialEventRepository = mock(FinancialEventRepository.class);
        userContextPort = mock(UserContextPort.class);

        useCase = new BuyTokensUseCase(
            schoolSubscriptionRepository,
            planQuotaRepository,
            subscriptionQuotaRepository,
            tokenPurchaseRepository,
            tokenPurchaseItemRepository,
            invoiceRepository,
            financialEventRepository,
            userContextPort
        );

        when(userContextPort.isSystemAdmin()).thenReturn(true);
    }

    private BuyTokensCommand command(String paymentMethod) {
        return new BuyTokensCommand(
            schoolId,
            subscriptionId,
            List.of(new TokenPurchaseItemInput(QuotaType.GRADING, BigDecimal.valueOf(10))),
            paymentMethod
        );
    }

    @Test
    void rejectsPayosBecauseNoGatewayEverConfirmedThisPayment() {
        assertThatThrownBy(() -> useCase.execute(command("PAYOS")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("payment-link");

        verifyNoInteractions(tokenPurchaseRepository, invoiceRepository, financialEventRepository);
        verify(subscriptionQuotaRepository, never()).addAllocation(any(), any());
    }

    @Test
    void rejectsSepayForTheSameReason() {
        assertThatThrownBy(() -> useCase.execute(command("SEPAY")))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(tokenPurchaseRepository, invoiceRepository, financialEventRepository);
    }

    @Test
    void rejectsUnknownMethod() {
        assertThatThrownBy(() -> useCase.execute(command("MOMO")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MOMO");

        verifyNoInteractions(tokenPurchaseRepository, invoiceRepository, financialEventRepository);
    }

    /**
     * MANUAL phải đi qua được guard. Test dừng lại ngay sau đó (không tìm thấy gói đăng ký) — điều
     * cần khẳng định là nó KHÔNG bị chặn bởi guard phương thức thanh toán.
     */
    @Test
    void acceptsManualAndProceedsPastTheGuard() {
        when(tokenPurchaseRepository.save(any(TokenPurchase.class)))
            .thenThrow(new IllegalStateException("đã đi qua guard"));

        assertThatCode(() -> useCase.execute(command("MANUAL")))
            .isNotInstanceOf(IllegalArgumentException.class);
    }
}
