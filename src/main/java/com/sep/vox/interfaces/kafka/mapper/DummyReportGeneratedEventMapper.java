package com.sep.vox.interfaces.kafka.mapper;

import com.sep.vox.interfaces.kafka.dto.ReportGeneratedPayload;

import tools.jackson.databind.JsonNode;

public final class DummyReportGeneratedEventMapper {

    private DummyReportGeneratedEventMapper() {}

    public static ReportGeneratedPayload fromJson(JsonNode payload) {
        return new ReportGeneratedPayload(
            textOf(payload, "reportId"),
            textOf(payload, "requestedBy"),
            textOf(payload, "status")
        );
    }

    private static String textOf(JsonNode node, String field) {
        if (node == null) return null;
        var child = node.get(field);
        return child == null || child.isNull() ? null : child.asString();
    }

}
