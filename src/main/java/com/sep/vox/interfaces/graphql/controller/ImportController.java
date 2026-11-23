package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewImportRowsQuery;
import com.sep.vox.application.port.input.query.ViewImportSessionQuery;
import com.sep.vox.application.port.input.query.ViewImportSessionsQuery;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportRowsUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionUseCase;
import com.sep.vox.application.port.input.usecase.importfile.ViewImportSessionsUseCase;
import com.sep.vox.application.response.input.importfile.ImportRowResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.application.response.input.importfile.ImportSessionSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.interfaces.shared.PageArguments;

@Controller("graphqlImportController")
@RequiredArgsConstructor
public class ImportController {

    private final ViewImportSessionUseCase viewImportSessionUseCase;
    private final ViewImportSessionsUseCase viewImportSessionsUseCase;
    private final ViewImportRowsUseCase viewImportRowsUseCase;

    @QueryMapping(name = "importSession")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SYSTEM_ADMIN')")
    public ImportSessionDetailsResponse importSession(@Argument(name = "id") UUID id) {
        return viewImportSessionUseCase.execute(new ViewImportSessionQuery(id));
    }

    @QueryMapping(name = "importSessions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<ImportSessionSummaryResponse> importSessions(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "type") ImportType type,
            @Argument(name = "status") ImportSessionStatus status) {
        PageArguments.validate(page, size);
        return viewImportSessionsUseCase.execute(new ViewImportSessionsQuery(page, size, type, status));
    }

    @QueryMapping(name = "importRows")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN', 'TEACHER', 'SYSTEM_ADMIN')")
    public PageResult<ImportRowResponse> importRows(
            @Argument(name = "sessionId") UUID sessionId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size,
            @Argument(name = "status") ImportRowStatus status) {
        PageArguments.validate(page, size);
        return viewImportRowsUseCase.execute(new ViewImportRowsQuery(sessionId, page, size, status));
    }
}
