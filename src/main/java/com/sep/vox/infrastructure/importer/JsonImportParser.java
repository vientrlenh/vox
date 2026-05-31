package com.sep.vox.infrastructure.importer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sep.vox.application.common.importer.ImportFileParser;
import com.sep.vox.application.common.importer.ImportRow;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JsonImportParser implements ImportFileParser {

    private final ObjectMapper objectMapper;

    public JsonImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ImportRow> parse(InputStream inputStream) {
        try {
            var root = objectMapper.readTree(inputStream);
            if (root == null || root.isNull()) {
                return List.of();
            }
            List<ImportRow> rows = new ArrayList<>();
            if (root.isArray()) {
                int index = 1;
                for (JsonNode node : root) {
                    Map<String, Object> map = toMap(node);
                    rows.add(new ImportRow(index, Map.of(), List.of(), map));
                    index++;
                }
            } else if (root.isObject()) {
                Map<String, Object> map = toMap(root);
                rows.add(new ImportRow(1, Map.of(), List.of(), map));
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode node) {
        return (Map<String, Object>) objectMapper.convertValue(node, Map.class);
    }
}
