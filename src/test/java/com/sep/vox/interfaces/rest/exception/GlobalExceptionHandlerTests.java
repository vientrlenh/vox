package com.sep.vox.interfaces.rest.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.sep.vox.application.exception.BusinessRuleViolationException;
import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.usecase.scoringrule.CreateSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.CreateSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.DeleteSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.DeleteSystemScoringRuleUseCase;
import com.sep.vox.interfaces.rest.controller.RestScoringRuleController;
import com.sep.vox.interfaces.rest.dto.response.ErrorResponse;
import com.sep.vox.interfaces.rest.dto.response.ValidationErrorResponse;
import com.sep.vox.support.RestControllerTest;

/**
 * Malformed-request answers of {@link GlobalExceptionHandler}, driven through
 * {@code RestScoringRuleController} because its request already uses enums and a primitive.
 */
@RestControllerTest(RestScoringRuleController.class)
@WithMockUser(roles = "SYSTEM_ADMIN")
class GlobalExceptionHandlerTests {

    private static final String RULE_URI = "/api/v1/assessment-policies/system/%s/scoring-rules";
    private static final String RULE_JSON = """
        {"code":"%s","name":"N","conditionType":"%s","conditionParams":{},"actionType":"CAP_FINAL_SCORE",
         "actionParams":{},"priority":%s,"severity":"INFO","stopProcessing":false,"isActive":true}""";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateSystemScoringRuleUseCase createSystemScoringRuleUseCase;
    @MockitoBean
    private CreateSchoolScoringRuleUseCase createSchoolScoringRuleUseCase;
    @MockitoBean
    private DeleteSystemScoringRuleUseCase deleteSystemScoringRuleUseCase;
    @MockitoBean
    private DeleteSchoolScoringRuleUseCase deleteSchoolScoringRuleUseCase;

    private MvcTestResult postRule(String policyId, String contentType, String body) {
        return mvc.post().uri(RULE_URI.formatted(policyId)).contentType(contentType).content(body).exchange();
    }

    private MvcTestResult postRuleJson(String body) {
        return postRule(UUID.randomUUID().toString(), MediaType.APPLICATION_JSON_VALUE, body);
    }

    @Test
    void brokenJsonAnswers400WithoutFrameworkText() {
        MvcTestResult result = postRuleJson("{not json");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("BAD_REQUEST");
        assertThat(result).bodyText().doesNotContain("com.sep").doesNotContain("Exception");
    }

    @Test
    void unknownEnumConstantAnswers400NamingFieldAndAcceptedValues() {
        MvcTestResult result = postRuleJson(RULE_JSON.formatted("C", "NOPE", 1));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.conditionType").asString()
            .contains("DURATION_LESS_THAN");
        assertThat(result).bodyText().doesNotContain("com.sep");
    }

    @Test
    void enumConstantInLowerCaseBindsInJsonBody() {
        MvcTestResult result = postRuleJson(RULE_JSON.formatted("C", "duration_less_than", 1));

        assertThat(result).hasStatus(HttpStatus.OK);
    }

    @Test
    void wrongTypedValueAnswers400NamingField() {
        MvcTestResult result = postRuleJson(RULE_JSON.formatted("C", "DURATION_LESS_THAN", "\"abc\""));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.priority").isNotNull();
    }

    @Test
    void nonUuidPathValueAnswers400NamingParameter() {
        String body = RULE_JSON.formatted("C", "DURATION_LESS_THAN", 1);

        MvcTestResult result = postRule("not-a-uuid", MediaType.APPLICATION_JSON_VALUE, body);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.policyId").isNotNull();
    }

    @Test
    void wrongContentTypeAnswers415() {
        MvcTestResult result = postRule(UUID.randomUUID().toString(), MediaType.TEXT_PLAIN_VALUE, "x");

        assertThat(result).hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void twoInvalidFieldsAreBothReportedByFieldName() {
        MvcTestResult result = postRuleJson(RULE_JSON.formatted("", "DURATION_LESS_THAN", 0));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors.code").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.priority").isNotNull();
    }

    @Test
    void missingRequestParameterAnswers400NamingParameter() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ValidationErrorResponse> response = handler.handleMissingParameter(
            new MissingServletRequestParameterException("assessmentId", "UUID"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).containsOnlyKeys("assessmentId");
    }

    @Test
    void unsupportedMediaTypeListsAcceptedTypes() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleUnsupportedMediaType(
            new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().message()).contains("application/json");
    }

    @Test
    void resourceNotFoundAnswers404WithNotFoundCode() {
        doThrow(new ResourceNotFoundException("missing")).when(deleteSystemScoringRuleUseCase).execute(any());

        MvcTestResult result = deleteSystemRule();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("NOT_FOUND");
        assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("missing");
    }

    @Test
    void resourceDuplicatedAnswers409WithDuplicatedCode() {
        doThrow(new ResourceDuplicatedException("exists")).when(deleteSystemScoringRuleUseCase).execute(any());

        MvcTestResult result = deleteSystemRule();

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("DUPLICATED");
        assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("exists");
    }

    @Test
    void businessRuleViolationAnswers422WithBusinessRuleViolationCode() {
        doThrow(new BusinessRuleViolationException("no quota")).when(deleteSystemScoringRuleUseCase).execute(any());

        MvcTestResult result = deleteSystemRule();

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("BUSINESS_RULE_VIOLATION");
        assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("no quota");
    }

    @Test
    void illegalStateExceptionAnswers500InternalServerErrorWithoutItsMessage() {
        doThrow(new IllegalStateException("secret internal detail")).when(deleteSystemScoringRuleUseCase).execute(any());

        MvcTestResult result = deleteSystemRule();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(result).bodyText().doesNotContain("secret internal detail");
    }

    @Test
    void illegalArgumentExceptionAnswers400WithIllegalArgumentCode() {
        doThrow(new IllegalArgumentException("bad request detail")).when(deleteSystemScoringRuleUseCase).execute(any());

        MvcTestResult result = deleteSystemRule();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.error").isEqualTo("ILLEGAL_ARGUMENT");
        assertThat(result).bodyJson().extractingPath("$.message").isEqualTo("bad request detail");
    }

    private MvcTestResult deleteSystemRule() {
        return mvc.delete().uri(RULE_URI.formatted(UUID.randomUUID()) + "/" + UUID.randomUUID()).exchange();
    }
}
