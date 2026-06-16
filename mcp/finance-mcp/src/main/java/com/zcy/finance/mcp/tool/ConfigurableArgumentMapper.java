package com.zcy.finance.mcp.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConfigurableArgumentMapper implements ArgumentMapper {

    private final String mode;
    private final String field;
    private final List<String> fields;

    public ConfigurableArgumentMapper(String mode, String field, List<String> fields) {
        this.mode = mode == null ? "object" : mode;
        this.field = field;
        this.fields = fields == null ? Collections.<String>emptyList() : fields;
    }

    @Override
    public List<Object> map(Map<String, Object> arguments) {
        if ("field".equals(mode)) {
            return Collections.<Object>singletonList(requiredValue(arguments, field, "argument-field is required when argument-mode is field"));
        }
        if ("fields".equals(mode)) {
            List<Object> result = new ArrayList<Object>();
            for (String name : fields) {
                result.add(requiredValue(arguments, name, name + " is required"));
            }
            return result;
        }
        if ("object".equals(mode)) {
            return Collections.<Object>singletonList(arguments);
        }
        throw new IllegalArgumentException("unsupported argument-mode: " + mode);
    }

    private Object requiredValue(Map<String, Object> arguments, String name, String message) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        Object value = arguments.get(name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
