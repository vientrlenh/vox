package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.order.CancelOrderCommand;
import com.sep.vox.application.port.input.command.order.CreateSubscriptionOrderCommand;
import com.sep.vox.application.port.input.command.order.CreateTopUpOrderCommand;
import com.sep.vox.application.port.input.command.subscription.RenewSchoolSubscriptionCommand;
import com.sep.vox.application.port.input.usecase.order.CancelOrderUseCase;
import com.sep.vox.application.port.input.usecase.order.CreateSubscriptionOrderUseCase;
import com.sep.vox.application.port.input.usecase.order.CreateTopUpOrderUseCase;
import com.sep.vox.application.port.input.usecase.subscription.RenewSchoolSubscriptionUseCase;
import com.sep.vox.interfaces.rest.dto.request.order.CreateSubscriptionOrderRequest;
import com.sep.vox.interfaces.rest.dto.request.order.CreateTopUpOrderRequest;
import com.sep.vox.interfaces.rest.dto.request.subscription.RenewSchoolSubscriptionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

/**
 * Orders of a school -- both buying/renewing a plan and topping up the balance. Kept apart from
 * RestSubscriptionController because a top-up order has nothing to do with the subscription: it
 * adds money to the SCHOOL-level wallet, which survives every plan change.
 *
 * <p>Every endpoint here is for SCHOOL_ADMIN only and does NOT accept a schoolId: the school is
 * taken from the token. If it came from the path, {@code hasRole('SCHOOL_ADMIN')} would not be
 * enough protection -- it answers "is this a school admin", not "is this a school admin OF THIS
 * SCHOOL".
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class RestOrderController {

    private final CreateSubscriptionOrderUseCase createSubscriptionOrderUseCase;
    private final CreateTopUpOrderUseCase createTopUpOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final RenewSchoolSubscriptionUseCase renewSchoolSubscriptionUseCase;

    /**
     * Đặt đơn mua một chu kỳ gói. Trả về id đơn ở trạng thái PENDING -- gói CHƯA được cấp, bước tiếp
     * theo là phát link thanh toán cho đơn này.
     */
    @PostMapping("/subscription")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> placeSubscriptionOrder(
            @Valid @RequestBody CreateSubscriptionOrderRequest request) {
        UUID data = createSubscriptionOrderUseCase.execute(
            new CreateSubscriptionOrderCommand(request.subscriptionPlanId()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đặt đơn đăng ký gói thành công", data));
    }

    /**
     * Đặt đơn nạp thêm số dư. Trả về id đơn ở trạng thái PENDING -- số dư CHƯA được cộng, chỉ cộng khi
     * tiền về thật (khác hẳn BuyTokensUseCase cũ, cộng hạn mức ngay lúc gọi).
     */
    @PostMapping("/topup")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> placeTopUpOrder(
            @Valid @RequestBody CreateTopUpOrderRequest request) {
        UUID data = createTopUpOrderUseCase.execute(new CreateTopUpOrderCommand(request.creditAmountVnd()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đặt đơn nạp thêm thành công", data));
    }

    /**
     * Places an order to renew the plan currently in use. It does not take the planId of the plan to
     * renew -- that is derived from the plan the school currently uses; what must be sent is
     * {@code acceptedPlanId}, the plan the school saw on the preview screen
     * (query {@code schoolSubscriptionRenewalPreview}).
     *
     * <p>It lives in RestOrderController and not in RestSubscriptionController because its result is an
     * ORDER: the plan is only granted when the money arrives, exactly like every other order.
     */
    @PostMapping("/renewal")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> placeRenewalOrder(
            @Valid @RequestBody RenewSchoolSubscriptionRequest request) {
        UUID data = renewSchoolSubscriptionUseCase.execute(new RenewSchoolSubscriptionCommand(request.acceptedPlanId()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đặt đơn gia hạn thành công", data));
    }

    /**
     * Hủy một đơn chưa trả tiền.
     *
     * <p>PATCH chứ không DELETE: đơn KHÔNG bị xóa, nó chuyển sang trạng thái CANCELLED và ở lại
     * trong lịch sử mua hàng của trường. DELETE sẽ hứa hẹn một thứ không xảy ra.
     *
     * <p>Có thể thất bại dù đơn còn PENDING -- xem CancelOrderUseCase: đơn đang có phiên thanh toán
     * sống ở cổng không cho hủy sớm (SePay) thì phải đợi hết hạn.
     */
    @PatchMapping("/{orderId}/cancellation")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> cancelOrder(@PathVariable(name = "orderId") UUID orderId) {
        UUID data = cancelOrderUseCase.execute(new CancelOrderCommand(orderId));
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", data));
    }
}
