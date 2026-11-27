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
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Subscription plan created successfully", data));
    }

    /**
     * Creates a new plan (DRAFT) and archives plan {id} IMMEDIATELY (it must be ACTIVE), pointing replacedByPlanId at the
     * new plan -- merging the two steps of creating a plan + retiring/choosing a replacement plan. See
     * CreateSubscriptionPlanReplacementUseCase for why the terms (price/cycle/quota) are only
     * checked at publish time, not here.
     */
    @PostMapping("/{id}/replacement-plan")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createPlanReplacement(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody CreateSubscriptionPlanRequest request) {
        UUID data = createSubscriptionPlanReplacementUseCase.execute(
            new CreateSubscriptionPlanReplacementCommand(id, CreateSubscriptionPlanRequest.toCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Replacement plan created successfully", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> archivePlan(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "replacedByPlanId", required = false) UUID replacedByPlanId) {
        UUID data = archiveSubscriptionPlanUseCase.execute(new ArchiveSubscriptionPlanCommand(id, replacedByPlanId));
        return ResponseEntity.ok(ApiResponse.success("Subscription plan archived successfully", data));
    }

    /**
     * Sets (or re-sets) the replacement plan of an ALREADY archived plan -- the repair path for plans that were archived
     * without choosing a replacement. See UpdateSubscriptionPlanReplacementUseCase.
     *
     * <p>replacedByPlanId is REQUIRED here, unlike the archive endpoint where it is optional: calling this
     * without a replacement plan leaves nothing to do.
     */
    @PatchMapping("/{id}/replacement")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> updatePlanReplacement(
            @PathVariable(name = "id") UUID id,
            @RequestParam(name = "replacedByPlanId") UUID replacedByPlanId) {
        UUID data = updateSubscriptionPlanReplacementUseCase.execute(
            new UpdateSubscriptionPlanReplacementCommand(id, replacedByPlanId));
        return ResponseEntity.ok(ApiResponse.success("Replacement plan updated successfully", data));
    }

    // PATCH rather than POST: publishing changes ONE status field of an existing plan (DRAFT ->
    // ACTIVE) and creates no new resource. It is also idempotent in the sense users care about --
    // pressing twice reports "only draft plans can be published" the second time and creates nothing more.
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> publishPlan(@PathVariable(name = "id") UUID id) {
        UUID data = publishSubscriptionPlanUseCase.execute(new PublishSubscriptionPlanCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Subscription plan published successfully", data));
    }

    @DeleteMapping("/{id}/draft")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDraftPlan(@PathVariable(name = "id") UUID id) {
        deleteDraftSubscriptionPlanUseCase.execute(new DeleteDraftSubscriptionPlanCommand(id));
        return ResponseEntity.ok(ApiResponse.success("Draft plan deleted successfully"));
    }

    /**
     * The school announces it will not buy again after the current term ends. Does NOT cut off access and does not
     * refund -- see CancelSchoolSubscriptionUseCase. Takes no id: the school's running term is derived
     * from the token.
     */
    @PatchMapping("/cancellation")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> cancelSubscription() {
        UUID data = cancelSchoolSubscriptionUseCase.execute(null);
        return ResponseEntity.ok(ApiResponse.success("Non-renewal request for the plan recorded", data));
    }

    // SYSTEM_ADMIN only -- unlike cancel/renew, this is an enforcement action, a School Admin may not do it themselves.
    /**
     * Enforces cutting off access IMMEDIATELY. PATCH because this changes the status of an existing plan, it does not
     * create a new resource. Takes no schoolId: the plan already knows which school it belongs to.
     */
    @PatchMapping("/{id}/suspension")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> suspendSubscription(
            @PathVariable(name = "id") UUID id,
            @RequestBody @Valid SuspendSubscriptionRequest request) {
        UUID data = forceSuspendSubscriptionUseCase.execute(
            new ForceSuspendSubscriptionCommand(id, request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Subscription suspended successfully", data));
    }

    @DeleteMapping("/{id}/suspension")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> unsuspendSubscription(
            @PathVariable(name = "id") UUID id,
            @RequestBody @Valid UnsuspendSubscriptionRequest request) {
        UUID data = unsuspendSubscriptionUseCase.execute(
            new UnsuspendSubscriptionCommand(id, request.note()));
        return ResponseEntity.ok(ApiResponse.success("Subscription unsuspended successfully", data));
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
        return ResponseEntity.ok(ApiResponse.success("Exam quota allocated to the teacher successfully", data));
    }

    @PutMapping("/schools/{schoolId}/students/practice-quota")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaUserAllocationSummaryResponse>> allocatePracticeQuotaToStudents(
            @PathVariable(name = "schoolId") UUID schoolId,
            @Valid @RequestBody AllocateQuotaRequest request) {
        QuotaUserAllocationSummaryResponse data = allocatePracticeQuotaToStudentsUseCase.execute(
            AllocateQuotaRequest.toAllocatePracticeQuotaCommand(schoolId, request));
        return ResponseEntity.ok(ApiResponse.success("Practice quota allocated to the student successfully", data));
    }


    /**
     * Sets the distribution cap for ONE quota type of a school.
     *
     * <p>The policy belongs to the SCHOOL, not to the subscription term -- so the path follows the school too, not
     * the subscription. The quota record is rebuilt every term, and putting the policy there would make it vanish
     * after every renewal (see V5).
     */
    /**
     * Moves money from the school's self-funded balance to the quota balance of {@code quotaType}.
     *
     * <p>POST rather than PUT: this is an ACCUMULATING operation, not setting a value. Calling it twice funds
     * twice -- unlike the quota split endpoint just above (which sets an absolute number, so pressing again is harmless)
     * and unlike the cap endpoint. The UI must block double clicks; there is no idempotency key at this layer yet.
     *
     * <p>The operation is ONE-WAY and cannot be refunded: money that has entered the quota balance does not return to the
     * self-funded balance, nor move to the other quota type. See FundQuotaFromBalanceUseCase.
     */
    @PostMapping("/schools/{schoolId}/quota-funding/{quotaType}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<QuotaFundingResponse>> fundQuotaFromBalance(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "quotaType") QuotaType quotaType,
            @Valid @RequestBody FundQuotaFromBalanceRequest request) {
        QuotaFundingResponse data = fundQuotaFromBalanceUseCase.execute(
            new FundQuotaFromBalanceCommand(schoolId, quotaType, request.amountVnd(), request.reason()));
        return ResponseEntity.ok(ApiResponse.success("Quota funded successfully", data));
    }

    @PutMapping("/schools/{schoolId}/quota-policies/{quotaType}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<BigDecimal>> setQuotaDistributionPolicy(
            @PathVariable(name = "schoolId") UUID schoolId,
            @PathVariable(name = "quotaType") QuotaType quotaType,
            @Valid @RequestBody SetQuotaDistributionPolicyRequest request) {
        BigDecimal data = setQuotaDistributionPolicyUseCase.execute(
            new SetQuotaDistributionPolicyCommand(schoolId, quotaType, request.distributableRatio()));
        return ResponseEntity.ok(ApiResponse.success("Quota distribution cap updated successfully", data));
    }
}
