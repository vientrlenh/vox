package com.sep.vox.application.common.importer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonImportParser implements ImportFileParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ImportRow> parse(InputStream inputStream) {
        try {
            var root = objectMapper.readTree(inputStream);
            if (root == null || root.isNull()) {
                return Collections.emptyList();
            }
            List<ImportRow> rows = new ArrayList<>();
            if (root.isArray()) {
                int index = 1;
                for (JsonNode node : root) {
                    var map = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
                    rows.add(new ImportRow(index, Collections.emptyMap(), List.of(), map));
                    index++;
                }
            } else if (root.isObject()) {
                var map = objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
                rows.add(new ImportRow(1, Collections.emptyMap(), List.of(), map));
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file JSON", e);
        }
    }
}
