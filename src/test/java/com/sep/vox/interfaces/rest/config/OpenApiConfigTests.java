package com.sep.vox.interfaces.rest.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.service.OpenAPIService;
import org.springframework.web.method.HandlerMethod;

import com.sep.vox.interfaces.rest.controller.RestAssessmentController;
import com.sep.vox.interfaces.rest.controller.internal.RestAssessmentTurnInternalController;

import io.swagger.v3.oas.models.Operation;

class OpenApiConfigTests {

    private final OperationCustomizer customizer = new OpenApiConfig().unprefixedRestTagCustomizer();

    @Test
    void restControllerKeepsItsTagWithoutTheRestPrefix() {
        Operation operation = operationTaggedLikeSpringdoc(RestAssessmentController.class);

        Operation customized = customizer.customize(operation, handlerMethodOf(RestAssessmentController.class));

        assertThat(customized.getTags()).containsExactly("assessment-controller");
    }

    @Test
    void internalRestControllerKeepsItsTagWithoutTheRestPrefix() {
        Operation operation = operationTaggedLikeSpringdoc(RestAssessmentTurnInternalController.class);

        Operation customized = customizer.customize(operation,
            handlerMethodOf(RestAssessmentTurnInternalController.class));

        assertThat(customized.getTags()).containsExactly("assessment-turn-internal-controller");
    }

    @Test
    void explicitTagOfARestControllerIsLeftAlone() {
        Operation operation = new Operation().tags(List.of("rest-explicit-tag"));

        Operation customized = customizer.customize(operation, handlerMethodOf(RestAssessmentController.class));

        assertThat(customized.getTags()).containsExactly("rest-explicit-tag");
    }

    private Operation operationTaggedLikeSpringdoc(Class<?> controllerType) {
        String generatedTag = OpenAPIService.splitCamelCase(controllerType.getSimpleName());
        assertThat(generatedTag).startsWith("rest-");
        return new Operation().tags(List.of(generatedTag));
    }

    private HandlerMethod handlerMethodOf(Class<?> controllerType) {
        Method method = Arrays.stream(controllerType.getDeclaredMethods())
            .filter(candidate -> Modifier.isPublic(candidate.getModifiers()))
            .findFirst()
            .orElseThrow();
        return new HandlerMethod(mock(controllerType), method);
    }
}
