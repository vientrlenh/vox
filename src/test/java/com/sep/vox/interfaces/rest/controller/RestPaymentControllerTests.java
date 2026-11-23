package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.command.payment.CreatePaymentCheckoutUrlCommand;
import com.sep.vox.application.port.input.usecase.payment.CreatePaymentCheckoutUrlUseCase;
import com.sep.vox.domain.model.payment.PaymentProvider;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(RestPaymentController.class)
@WithMockUser(roles = "SCHOOL_ADMIN")
class RestPaymentControllerTests {

    private static final String CHECKOUT_URI = "/api/v1/payments/checkout-url";

    private final UUID orderId = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private CreatePaymentCheckoutUrlUseCase createPaymentCheckoutUrlUseCase;

    private MvcTestResult checkout(String body) {
        return mvc.post().uri(CHECKOUT_URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    void checkout_should_pass_typed_provider_to_the_command() {
        MvcTestResult result = checkout("{\"orderId\": \"%s\", \"provider\": \"SEPAY\"}".formatted(orderId));

        assertThat(result).hasStatus(201);
        verify(createPaymentCheckoutUrlUseCase)
            .execute(new CreatePaymentCheckoutUrlCommand(orderId, PaymentProvider.SEPAY));
    }

    @Test
    void checkout_should_accept_a_lower_case_provider() {
        MvcTestResult result = checkout("{\"orderId\": \"%s\", \"provider\": \"payos\"}".formatted(orderId));

        assertThat(result).hasStatus(201);
        verify(createPaymentCheckoutUrlUseCase)
            .execute(new CreatePaymentCheckoutUrlCommand(orderId, PaymentProvider.PAYOS));
    }

    @Test
    void checkout_should_answer_400_listing_providers_for_an_unknown_provider() {
        MvcTestResult result = checkout("{\"orderId\": \"%s\", \"provider\": \"MOMO\"}".formatted(orderId));

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.provider").asString()
            .contains("PAYOS", "SEPAY").doesNotContain("com.sep");
        verifyNoInteractions(createPaymentCheckoutUrlUseCase);
    }

    @Test
    void checkout_should_answer_400_when_provider_is_missing() {
        MvcTestResult result = checkout("{\"orderId\": \"%s\"}".formatted(orderId));

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.provider").isNotNull();
        verifyNoInteractions(createPaymentCheckoutUrlUseCase);
    }

    @Test
    @WithAnonymousUser
    void checkout_should_answer_401_for_anonymous() {
        assertThat(checkout("{\"orderId\": \"%s\", \"provider\": \"PAYOS\"}".formatted(orderId))).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void checkout_should_answer_403_for_a_teacher() {
        assertThat(checkout("{\"orderId\": \"%s\", \"provider\": \"PAYOS\"}".formatted(orderId))).hasStatus(403);
        verifyNoInteractions(createPaymentCheckoutUrlUseCase);
    }
}
