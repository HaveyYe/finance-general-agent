package com.zcy.finance.mcp.tool;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ToolDefinition {

    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
    private final DubboMapping dubboMapping;
    private final String operation;
    private final String authMode;
    private final boolean confirmRequired;
    private final boolean adminOnly;
    private final String permissionPrecheckTool;
    private final String permissionArgumentField;
    private final boolean injectToken;
    private final boolean injectDigitUser;

    public ToolDefinition(String name, String description, Map<String, Object> inputSchema, DubboMapping dubboMapping) {
        this(name, description, inputSchema, dubboMapping, "read", "none", false, false, null, "contractNo", false, false);
    }

    public ToolDefinition(String name,
                          String description,
                          Map<String, Object> inputSchema,
                          DubboMapping dubboMapping,
                          String operation,
                          String authMode,
                          boolean confirmRequired,
                          boolean adminOnly,
                          String permissionPrecheckTool,
                          String permissionArgumentField,
                          boolean injectToken,
                          boolean injectDigitUser) {
        this.name = name;
        this.description = description;
        this.inputSchema = Collections.unmodifiableMap(normalizeMap(inputSchema));
        this.dubboMapping = dubboMapping;
        this.operation = operation == null ? "read" : operation;
        this.authMode = authMode == null ? "none" : authMode;
        this.confirmRequired = confirmRequired;
        this.adminOnly = adminOnly;
        this.permissionPrecheckTool = permissionPrecheckTool;
        this.permissionArgumentField = permissionArgumentField == null ? "contractNo" : permissionArgumentField;
        this.injectToken = injectToken;
        this.injectDigitUser = injectDigitUser;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public DubboMapping getDubboMapping() {
        return dubboMapping;
    }

    public String getOperation() {
        return operation;
    }

    public String getAuthMode() {
        return authMode;
    }

    public boolean isConfirmRequired() {
        return confirmRequired;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public String getPermissionPrecheckTool() {
        return permissionPrecheckTool;
    }

    public String getPermissionArgumentField() {
        return permissionArgumentField;
    }

    public boolean isInjectToken() {
        return injectToken;
    }

    public boolean isInjectDigitUser() {
        return injectDigitUser;
    }

    public Map<String, Object> toMcpTool() {
        Map<String, Object> tool = new LinkedHashMap<String, Object>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", inputSchema);
        return tool;
    }

    private static Map<String, Object> normalizeMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), normalizeValue(entry.getValue()));
        }
        return result;
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            if (isNumericKeyMap(source)) {
                TreeMap<Integer, Object> ordered = new TreeMap<Integer, Object>();
                for (Map.Entry<?, ?> entry : source.entrySet()) {
                    ordered.put(Integer.valueOf(String.valueOf(entry.getKey())), entry.getValue());
                }
                List<Object> list = new ArrayList<Object>();
                for (Object item : ordered.values()) {
                    list.add(normalizeValue(item));
                }
                return list;
            }

            Map<String, Object> result = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                result.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
            }
            return result;
        }
        if (value instanceof List) {
            List<?> source = (List<?>) value;
            List<Object> result = new ArrayList<Object>();
            for (Object item : source) {
                result.add(normalizeValue(item));
            }
            return result;
        }
        return value;
    }

    private static boolean isNumericKeyMap(Map<?, ?> source) {
        if (source.isEmpty()) {
            return false;
        }
        for (Object key : source.keySet()) {
            if (!String.valueOf(key).matches("\\d+")) {
                return false;
            }
        }
        return true;
    }
}
