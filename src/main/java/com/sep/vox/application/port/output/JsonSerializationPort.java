package com.sep.vox.application.port.output;

import java.util.List;
import java.util.Map;

public interface JsonSerializationPort {
    String toJson(Object value);

    Map<String, String> toStringMap(String json);

    List<String> toStringList(String json);

    List<Map<String, String>> toStringMapList(String json);
}
