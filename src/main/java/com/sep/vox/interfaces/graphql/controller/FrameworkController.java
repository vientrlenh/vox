package com.sep.vox.interfaces.graphql.controller;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.command.UpdateFrameworkVersionCommand;
import com.sep.vox.application.port.input.query.ViewFrameworkDetailsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.query.ViewFrameworksQuery;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworksUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.dto.FrameworkVersionDto;

@Controller("graphqlFrameworkController")
public class FrameworkController {

    private final ViewFrameworksUseCase viewFrameworksUseCase;
    private final ViewFrameworkDetailsUseCase viewFrameworkDetailsUseCase;
    private final ViewFrameworkVersionsUseCase viewFrameworkVersionsUseCase;
    private final ViewFrameworkVersionDetailsUseCase viewFrameworkVersionDetailsUseCase;
    private final UpdateFrameworkVersionUseCase updateFrameworkVersionUseCase;

    public FrameworkController(
            ViewFrameworksUseCase viewFrameworksUseCase,
            ViewFrameworkDetailsUseCase viewFrameworkDetailsUseCase,
            ViewFrameworkVersionsUseCase viewFrameworkVersionsUseCase,
            ViewFrameworkVersionDetailsUseCase viewFrameworkVersionDetailsUseCase,
            UpdateFrameworkVersionUseCase updateFrameworkVersionUseCase) {
        this.viewFrameworksUseCase = viewFrameworksUseCase;
        this.viewFrameworkDetailsUseCase = viewFrameworkDetailsUseCase;
        this.viewFrameworkVersionsUseCase = viewFrameworkVersionsUseCase;
        this.viewFrameworkVersionDetailsUseCase = viewFrameworkVersionDetailsUseCase;
        this.updateFrameworkVersionUseCase = updateFrameworkVersionUseCase;
    }

    @QueryMapping(name = "frameworks")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<FrameworkDto> frameworks(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang không hợp lệ");
        }
        return viewFrameworksUseCase.execute(new ViewFrameworksQuery(page, size));
    }

    @QueryMapping(name = "framework")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FrameworkDto framework(@Argument(name = "id") UUID id) {
        return viewFrameworkDetailsUseCase.execute(new ViewFrameworkDetailsQuery(id));
    }

    @QueryMapping(name = "frameworkVersions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<FrameworkVersionDto> frameworkVersions(
            @Argument(name = "frameworkId") UUID frameworkId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalArgumentException("Số trang hoặc kích thước trang không hợp lệ");
        }
        return viewFrameworkVersionsUseCase.execute(new ViewFrameworkVersionsQuery(frameworkId, page, size));
    }

    @QueryMapping(name = "frameworkVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FrameworkVersionDto frameworkVersion(@Argument(name = "id") UUID id) {
        return viewFrameworkVersionDetailsUseCase.execute(new ViewFrameworkVersionDetailsQuery(id));
    }

    @MutationMapping(name = "updateFrameworkVersion")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UUID updateFrameworkVersion(@Argument(name = "input") UpdateFrameworkVersionInput input) {
        var command = toCommand(input);
        return updateFrameworkVersionUseCase.execute(command);
    }

    private UpdateFrameworkVersionCommand toCommand(UpdateFrameworkVersionInput input) {
        List<UpdateFrameworkVersionCommand.CriterionInput> criteria = input.criteria() == null ? null
            : input.criteria().stream()
                .map(c -> new UpdateFrameworkVersionCommand.CriterionInput(
                    c.code(), c.name(), c.description(),
                    c.bands() == null ? null : c.bands().stream()
                        .map(b -> new UpdateFrameworkVersionCommand.CriterionBandInput(
                            b.resultBandCode(), b.descriptor(), b.positiveSignals(), b.negativeSignals()))
                        .toList()))
                .toList();

        List<UpdateFrameworkVersionCommand.ResultBandInput> resultBands = input.resultBands() == null ? null
            : input.resultBands().stream()
                .map(r -> new UpdateFrameworkVersionCommand.ResultBandInput(
                    r.code(), r.label(), r.description(),
                    r.scoreMin() != null ? BigDecimal.valueOf(r.scoreMin()) : null,
                    r.scoreMax() != null ? BigDecimal.valueOf(r.scoreMax()) : null,
                    r.order()))
                .toList();

        return new UpdateFrameworkVersionCommand(
            input.frameworkId(),
            input.versionId(),
            input.code(),
            input.name(),
            input.description(),
            input.effectiveFrom() != null ? OffsetDateTime.parse(input.effectiveFrom()) : null,
            input.effectiveTo() != null ? OffsetDateTime.parse(input.effectiveTo()) : null,
            criteria,
            resultBands
        );
    }
       // GraphQL input types (Spring GraphQL maps these from the schema input objects)
    public record UpdateFrameworkVersionInput(
        UUID frameworkId,
        UUID versionId,
        String code,
        String name,
        String description,
        String effectiveFrom,
        String effectiveTo,
        List<CriterionInput> criteria,
        List<ResultBandInput> resultBands
    ) {}

    public record CriterionInput(
        String code,
        String name,
        String description,
        List<CriterionBandInput> bands
    ) {}

    public record CriterionBandInput(
        String resultBandCode,
        String descriptor,
        String positiveSignals,
        String negativeSignals
    ) {}

    public record ResultBandInput(
        String code,
        String label,
        String description,
        Double scoreMin,
        Double scoreMax,
        int order
    ) {}
}
