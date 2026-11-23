package com.sep.vox.interfaces.rest.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.application.port.input.command.subscription.ArchiveSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.subscription.CreateSubscriptionPlanReplacementCommand;
import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanReplacementCommand;
import com.sep.vox.application.port.input.command.subscription.ForceSuspendSubscriptionCommand;
import com.sep.vox.application.port.input.command.subscription.UnsuspendSubscriptionCommand;
import com.sep.vox.application.port.input.command.subscription.DeleteDraftSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.subscription.PublishSubscriptionPlanCommand;
import com.sep.vox.application.port.input.usecase.subscription.AllocateAssessmentQuotaToTeachersUseCase;
import com.sep.vox.application.port.input.usecase.subscription.AllocatePracticeQuotaToStudentsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ArchiveSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreateSubscriptionPlanReplacementUseCase;
import com.sep.vox.application.port.input.usecase.subscription.UpdateSubscriptionPlanReplacementUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CancelSchoolSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreateSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.DeleteDraftSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ForceSuspendSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.PublishSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.UnsuspendSubscriptionUseCase;
import com.sep.vox.application.port.input.command.subscription.SetQuotaDistributionPolicyCommand;
import com.sep.vox.application.port.input.command.subscription.FundQuotaFromBalanceCommand;
import com.sep.vox.application.port.input.usecase.subscription.SetQuotaDistributionPolicyUseCase;
import com.sep.vox.application.port.input.usecase.subscription.FundQuotaFromBalanceUseCase;
import com.sep.vox.application.response.input.subscription.QuotaFundingResponse;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.interfaces.rest.dto.request.subscription.FundQuotaFromBalanceRequest;
import com.sep.vox.interfaces.rest.dto.request.subscription.SetQuotaDistributionPolicyRequest;

import java.math.BigDecimal;
import com.sep.vox.interfaces.rest.dto.request.subscription.AllocateQuotaRequest;
import com.sep.vox.interfaces.rest.dto.request.subscription.CreateSubscriptionPlanRequest;
import com.sep.vox.interfaces.rest.dto.request.subscription.SuspendSubscriptionRequest;
import com.sep.vox.interfaces.rest.dto.request.subscription.UnsuspendSubscriptionRequest;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class RestSubscriptionController {

    private final CreateSubscriptionPlanUseCase createSubscriptionPlanUseCase;
    private final CreateSubscriptionPlanReplacementUseCase createSubscriptionPlanReplacementUseCase;
    private final ArchiveSubscriptionPlanUseCase archiveSubscriptionPlanUseCase;
    private final UpdateSubscriptionPlanReplacementUseCase updateSubscriptionPlanReplacementUseCase;
    private final PublishSubscriptionPlanUseCase publishSubscriptionPlanUseCase;
    private final DeleteDraftSubscriptionPlanUseCase deleteDraftSubscriptionPlanUseCase;
    private final CancelSchoolSubscriptionUseCase cancelSchoolSubscriptionUseCase;
    private final ForceSuspendSubscriptionUseCase forceSuspendSubscriptionUseCase;
    private final UnsuspendSubscriptionUseCase unsuspendSubscriptionUseCase;
    private final AllocateAssessmentQuotaToTeachersUseCase allocateAssessmentQuotaToTeachersUseCase;
    private final AllocatePracticeQuotaToStudentsUseCase allocatePracticeQuotaToStudentsUseCase;
    private final SetQuotaDistributionPolicyUseCase setQuotaDistributionPolicyUseCase;
    private final FundQuotaFromBalanceUseCase fundQuotaFromBalanceUseCase;

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createPlan(@Valid @RequestBody CreateSubscriptionPlanRequest request) {
        UUID data = createSubscriptionPlanUseCase.execute(CreateSubscriptionPlanRequest.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo gói đăng ký thành công", data));
    }

    /**
     * Tạo gói mới (DRAFT) và archive NGAY gói {id} (phải đang ACTIVE) trỏ replacedByPlanId sang gói
     * mới -- gộp hai bước tạo gói + ngừng bán/chọn gói thay thế. Xem
     * CreateSubscriptionPlanReplacementUseCase cho lý do điều khoản (giá/chu kỳ/hạn mức) chỉ được
     * kiểm lúc publish, không phải ở đây.
     */
    @PostMapping("/{id}/replacement-plan")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createPlanReplacement(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody CreateSubscriptionPlanRequest request) {
        UUID data = createSubscriptionPlanReplacementUseCase.execute(
            new CreateSubscriptionPlanReplacementCommand(id, CreateSubscriptionPlanRequest.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo gói thay thế thành công", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archivePlan(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "replacedByPlanId", required = false) UUID replacedByPlanId) {
        UUID data = archiveSubscriptionPlanUseCase.execute(new ArchiveSubscriptionPlanCommand(id, replacedByPlanId));
        return ResponseEntity.ok(ApiResponse.success("Lưu trữ gói đăng ký thành công", data));
    }

    /**
     * Chỉ (hoặc chỉ lại) gói thay thế cho một gói ĐÃ lưu trữ -- đường sửa cho những gói bị lưu trữ
     * mà quên chọn gói thay thế. Xem UpdateSubscriptionPlanReplacementUseCase.
     *
     * <p>replacedByPlanId BẮT BUỘC ở đây, khác với endpoint lưu trữ nơi nó tùy chọn: gọi vào đây mà
     * không kèm gói thay thế thì không có việc gì để làm.
     */
    @PatchMapping("/{id}/replacement")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> updatePlanReplacement(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "replacedByPlanId") UUID replacedByPlanId) {
        UUID data = updateSubscriptionPlanReplacementUseCase.execute(
            new UpdateSubscriptionPlanReplacementCommand(id, replacedByPlanId));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật gói thay thế thành công", data));
    }

    // PATCH chứ không phải POST: xuất bản là đổi MỘT trường status của gói đã tồn tại (DRAFT ->
    // ACTIVE), không tạo ra tài nguyên mới nào. Cũng vì thế mà idempotent theo nghĩa người dùng
    // quan tâm -- bấm hai lần thì lần sau báo "chỉ xuất bản được gói nháp" chứ không sinh thêm gì.
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> publishPlan(@PathVariable(name = "id") UUID id) {
        UUID data = publishSubscriptionPlanUseCase.execute(new PublishSubscriptionPlanCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xuất bản gói đăng ký thành công", data));
    }

    @DeleteMapping("/{id}/draft")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDraftPlan(@PathVariable(name = "id") UUID id) {
        deleteDraftSubscriptionPlanUseCase.execute(new DeleteDraftSubscriptionPlanCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Xóa gói nháp thành công"));
    }

    /**
     * Trường báo sẽ không mua tiếp sau khi kỳ hiện tại kết thúc. KHÔNG cắt quyền dùng và không hoàn
     * tiền -- xem CancelSchoolSubscriptionUseCase. Không nhận id nào: kỳ đang chạy của trường suy ra
     * từ token.
     */
    @PatchMapping("/cancellation")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> cancelSubscription() {
        UUID data = cancelSchoolSubscriptionUseCase.execute(null);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận yêu cầu không gia hạn gói", data));
    }

    // Chỉ SYSTEM_ADMIN -- khác cancel/renew, đây là hành động cưỡng chế, School Admin không được tự làm.
    /**
     * Cưỡng chế cắt quyền dùng NGAY. PATCH vì đây là đổi trạng thái của một gói đã tồn tại, không
     * tạo tài nguyên mới. Không nhận schoolId: gói đã biết nó thuộc trường nào.
     */
    @PatchMapping("/{id}/suspension")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> suspendSubscription(
            @PathVariable(name = "id") UUID id,
            @RequestBody @Valid SuspendSubscriptionRequest request) {
        UUID data = forceSuspendSubscriptionUseCase.execute(
            new ForceSuspendSubscriptionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Đình chỉ gói đăng ký thành công", data));
    }

    @DeleteMapping("/{id}/suspension")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> unsuspendSubscription(
            @PathVariable(name = "id") UUID id,
            @RequestBody @Valid UnsuspendSubscriptionRequest request) {
        UUID data = unsuspendSubscriptionUseCase.execute(
            new UnsuspendSubscriptionCommand(id, request.note()));
        return ResponseEntity.ok(ApiResponse.success("Gỡ đình chỉ gói đăng ký thành công", data));
    }

    // The two READ paths for allocated quota (GET .../teachers/assessment-quota and GET .../students/practice-quota)
    // have been removed: they moved to GraphQL as schoolAssessmentQuotaUserAllocations /
    // schoolPracticeQuotaUserAllocations, which have paging and can join user names through a data
    // loader. The WRITE path stays on REST following the convention "read through GraphQL, write through REST".

    @PutMapping("/schools/{schoolId}/teachers/assessment-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryResponse>> allocateAssessmentQuotaToTeachers(
            @PathVariable(name = "schoolId") UUID schoolId,
            @Valid @RequestBody AllocateQuotaRequest request) {
        QuotaUserAllocationSummaryResponse data = allocateAssessmentQuotaToTeachersUseCase.execute(
            AllocateQuotaRequest.toAllocateAssessmentQuotaCommand(schoolId, request));
        return ResponseEntity.ok(ApiResponse.success("Phân bổ hạn mức thi cho giáo viên thành công", data));
    }

    @PutMapping("/schools/{schoolId}/students/practice-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryResponse>> allocatePracticeQuotaToStudents(
            @PathVariable(name = "schoolId") UUID schoolId,
            @Valid @RequestBody AllocateQuotaRequest request) {
        QuotaUserAllocationSummaryResponse data = allocatePracticeQuotaToStudentsUseCase.execute(
            AllocateQuotaRequest.toAllocatePracticeQuotaCommand(schoolId, request));
        return ResponseEntity.ok(ApiResponse.success("Phân bổ hạn mức luyện tập cho học sinh thành công", data));
    }


    /**
     * Đặt trần phân phối cho MỘT loại hạn mức của trường.
     *
     * <p>Chính sách thuộc về TRƯỜNG, không thuộc kỳ đăng ký -- nên đường dẫn cũng theo trường, không
     * theo subscription. Bản ghi hạn mức được dựng lại mỗi kỳ, đặt chính sách ở đó là để nó biến mất
     * sau mỗi lần gia hạn (xem V5).
     */
    /**
     * Chuyển tiền từ ví tự nạp của trường sang ví hạn mức của {@code quotaType}.
     *
     * <p>POST chứ không PUT: đây là một phép CỘNG DỒN, không phải đặt một giá trị. Gọi hai lần là nạp
     * hai lần -- khác hẳn endpoint chia hạn mức ngay trên (đặt số tuyệt đối nên bấm lại vô hại) và
     * khác endpoint đặt trần. Giao diện phải chặn bấm đúp; chưa có khoá idempotency ở tầng này.
     *
     * <p>Thao tác MỘT CHIỀU và không hoàn lại được: tiền đã vào ví hạn mức thì không quay về ví tự
     * nạp, cũng không chuyển sang loại quota kia. Xem FundQuotaFromBalanceUseCase.
     */
    @PostMapping("/schools/{schoolId}/quota-funding/{quotaType}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaFundingResponse>> fundQuotaFromBalance(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "quotaType") QuotaType quotaType,
            @Valid @RequestBody FundQuotaFromBalanceRequest request) {
        QuotaFundingResponse data = fundQuotaFromBalanceUseCase.execute(
            new FundQuotaFromBalanceCommand(schoolId, quotaType, request.amountVnd(), request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Nạp tiền vào hạn mức thành công", data));
    }

    @PutMapping("/schools/{schoolId}/quota-policies/{quotaType}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<BigDecimal>> setQuotaDistributionPolicy(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "quotaType") QuotaType quotaType,
            @Valid @RequestBody SetQuotaDistributionPolicyRequest request) {
        BigDecimal data = setQuotaDistributionPolicyUseCase.execute(
            new SetQuotaDistributionPolicyCommand(schoolId, quotaType, request.distributableRatio()));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trần phân phối hạn mức thành công", data));
    }
}
