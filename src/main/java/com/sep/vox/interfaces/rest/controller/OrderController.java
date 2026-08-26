package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.port.input.command.CancelOrderCommand;
import com.sep.vox.application.port.input.command.CreateSubscriptionOrderCommand;
import com.sep.vox.application.port.input.command.CreateTopUpOrderCommand;
import com.sep.vox.application.port.input.command.RenewSchoolSubscriptionCommand;
import com.sep.vox.application.port.input.usecase.order.CancelOrderUseCase;
import com.sep.vox.application.port.input.usecase.order.CreateSubscriptionOrderUseCase;
import com.sep.vox.application.port.input.usecase.order.CreateTopUpOrderUseCase;
import com.sep.vox.application.port.input.usecase.subscription.RenewSchoolSubscriptionUseCase;
import com.sep.vox.interfaces.rest.dto.request.CreateSubscriptionOrderRequest;
import com.sep.vox.interfaces.rest.dto.request.CreateTopUpOrderRequest;
import com.sep.vox.interfaces.rest.dto.request.RenewSchoolSubscriptionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

/**
 * Đơn hàng của trường -- gồm cả mua/gia hạn gói lẫn nạp thêm số dư. Tách khỏi SubscriptionController
 * vì đơn nạp thêm không dính gì tới subscription: nó cộng tiền vào ví cấp TRƯỜNG, sống xuyên qua mọi
 * lần đổi gói.
 *
 * <p>Mọi endpoint ở đây đều chỉ dành cho SCHOOL_ADMIN và KHÔNG nhận schoolId: trường lấy từ token.
 * Nhận từ đường dẫn thì {@code hasRole('SCHOOL_ADMIN')} không đủ để bảo vệ -- nó trả lời "có phải
 * school admin không", không trả lời "có phải school admin CỦA TRƯỜNG NÀY không".
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateSubscriptionOrderUseCase createSubscriptionOrderUseCase;
    private final CreateTopUpOrderUseCase createTopUpOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final RenewSchoolSubscriptionUseCase renewSchoolSubscriptionUseCase;

    public OrderController(
            CreateSubscriptionOrderUseCase createSubscriptionOrderUseCase,
            CreateTopUpOrderUseCase createTopUpOrderUseCase,
            CancelOrderUseCase cancelOrderUseCase,
            RenewSchoolSubscriptionUseCase renewSchoolSubscriptionUseCase) {
        this.createSubscriptionOrderUseCase = createSubscriptionOrderUseCase;
        this.createTopUpOrderUseCase = createTopUpOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.renewSchoolSubscriptionUseCase = renewSchoolSubscriptionUseCase;
    }

    /**
     * Đặt đơn mua một chu kỳ gói. Trả về id đơn ở trạng thái PENDING -- gói CHƯA được cấp, bước tiếp
     * theo là phát link thanh toán cho đơn này.
     */
    @PostMapping("/subscription")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> placeSubscriptionOrder(
            @Valid @RequestBody CreateSubscriptionOrderRequest request) {
        var data = createSubscriptionOrderUseCase.execute(
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
        var data = createTopUpOrderUseCase.execute(new CreateTopUpOrderCommand(request.creditAmountVnd()));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Đặt đơn nạp thêm thành công", data));
    }

    /**
     * Đặt đơn gia hạn gói đang dùng. Không nhận planId của gói cần gia hạn -- nó suy ra từ gói trường
     * đang dùng; thứ phải gửi lên là {@code acceptedPlanId}, tức gói trường đã nhìn thấy ở màn xem
     * trước (query {@code schoolSubscriptionRenewalPreview}).
     *
     * <p>Nằm ở OrderController chứ không phải SubscriptionController vì kết quả của nó là một ĐƠN
     * HÀNG: gói chỉ được cấp khi tiền về, giống hệt mọi đơn khác.
     */
    @PostMapping("/renewal")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> placeRenewalOrder(
            @Valid @RequestBody RenewSchoolSubscriptionRequest request) {
        var data = renewSchoolSubscriptionUseCase.execute(new RenewSchoolSubscriptionCommand(request.acceptedPlanId()));
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
        var data = cancelOrderUseCase.execute(new CancelOrderCommand(orderId));
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", data));
    }
}
