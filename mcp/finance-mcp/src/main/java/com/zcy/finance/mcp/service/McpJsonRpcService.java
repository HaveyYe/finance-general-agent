package com.zcy.finance.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zcy.finance.mcp.config.FinanceDubboProperties;
import com.zcy.finance.mcp.config.FinanceMcpProperties;
import com.zcy.finance.mcp.dto.DubboGenericCallArgs;
import com.zcy.finance.mcp.dto.JsonRpcRequest;
import com.zcy.finance.mcp.dto.JsonRpcResponse;
import com.zcy.finance.mcp.dubbo.DubboGenericInvokeService;
import com.zcy.finance.mcp.tool.DubboMapping;
import com.zcy.finance.mcp.tool.ToolDefinition;
import com.zcy.finance.mcp.tool.ToolRegistry;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Service
public class McpJsonRpcService {

    private static final String ZCY_FINANCE_INTERFACE_PREFIX = "cn.gov.zcy.finance.";

    private static final Logger LOGGER = LoggerFactory.getLogger(McpJsonRpcService.class);

    private final DubboGenericInvokeService dubboService;
    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final FinanceDubboProperties dubboProperties;
    private final FinanceMcpProperties mcpProperties;

    public McpJsonRpcService(DubboGenericInvokeService dubboService,
                             ObjectMapper objectMapper,
                             ToolRegistry toolRegistry,
                             FinanceDubboProperties dubboProperties,
                             FinanceMcpProperties mcpProperties) {
        this.dubboService = dubboService;
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.dubboProperties = dubboProperties;
        this.mcpProperties = mcpProperties;
    }

    public JsonRpcResponse handle(JsonRpcRequest request) {
        return handle(request, new McpRequestContext(UUID.randomUUID().toString(), null, null, null));
    }

    public JsonRpcResponse handle(JsonRpcRequest request, McpRequestContext context) {
        if (request == null || request.getMethod() == null) {
            return JsonRpcResponse.err(null, -32600, "Invalid Request", "method is required");
        }

        String method = request.getMethod();

        try {
            if ("initialize".equals(method)) {
                return JsonRpcResponse.ok(request.getId(), initializeResult());
            }
            if ("tools/list".equals(method)) {
                return JsonRpcResponse.ok(request.getId(), toolsListResult());
            }
            if ("tools/call".equals(method)) {
                return handleToolsCall(request, context);
            }
            if ("ping".equals(method)) {
                return JsonRpcResponse.ok(request.getId(), Collections.singletonMap("pong", true));
            }
            return JsonRpcResponse.err(request.getId(), -32601, "Method not found", method);
        } catch (IllegalArgumentException e) {
            return JsonRpcResponse.err(request.getId(), -32602, "Invalid params", e.getMessage());
        } catch (Exception e) {
            return JsonRpcResponse.err(request.getId(), -32000, "Internal error", e.getMessage());
        }
    }

    private JsonRpcResponse handleToolsCall(JsonRpcRequest request, McpRequestContext context) {
        Map<String, Object> params = toMap(request.getParams());
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = new LinkedHashMap<String, Object>(toMap(params.get("arguments")));

        ToolDefinition toolDefinition = toolRegistry.get(toolName);
        if (toolDefinition != null) {
            return invokeBusinessTool(request, toolDefinition, arguments, context);
        }

        if ("dubbo_call".equals(toolName) && mcpProperties.isExposeGenericDubboCall()) {
            return invokeGenericDubboTool(request, arguments, context);
        }

        return JsonRpcResponse.err(request.getId(), -32602, "Invalid params", "unsupported tool: " + toolName);
    }

    private JsonRpcResponse invokeBusinessTool(JsonRpcRequest request,
                                               ToolDefinition toolDefinition,
                                               Map<String, Object> arguments,
                                               McpRequestContext context) {
        long startedAt = System.currentTimeMillis();
        String traceId = context == null ? UUID.randomUUID().toString() : context.getTraceId();

        try {
            validateRequiredArguments(toolDefinition, arguments);
            applySafetyPolicy(toolDefinition, arguments, context);
            applyAuthAndIdentity(toolDefinition, arguments, context);
            applyPermissionPrecheck(toolDefinition, arguments, context);

            Object invokeResult = invokeDubbo(toolDefinition, arguments);
            Map<String, Object> result = unifiedResult(toolDefinition.getName(), traceId, invokeResult);
            boolean success = Boolean.TRUE.equals(result.get("success"));
            audit(toolDefinition, context, traceId, arguments, success, null, startedAt);
            return JsonRpcResponse.ok(request.getId(), mcpToolResult(result, !success));
        } catch (ToolCallException e) {
            Map<String, Object> result = unifiedError(toolDefinition.getName(), traceId, e.getErrorCode(), e.getMessage(), e.getDetail());
            audit(toolDefinition, context, traceId, arguments, false, e.getErrorCode(), startedAt);
            return JsonRpcResponse.ok(request.getId(), mcpToolResult(result, true));
        } catch (RuntimeException e) {
            String errorCode = classifyError(e);
            Map<String, Object> result = unifiedError(toolDefinition.getName(), traceId, errorCode, cleanMessage(e), sanitizedDetail(e));
            audit(toolDefinition, context, traceId, arguments, false, errorCode, startedAt);
            return JsonRpcResponse.ok(request.getId(), mcpToolResult(result, true));
        }
    }

    private JsonRpcResponse invokeGenericDubboTool(JsonRpcRequest request,
                                                   Map<String, Object> arguments,
                                                   McpRequestContext context) {
        if (!isAdmin(context)) {
            Map<String, Object> result = unifiedError("dubbo_call", context.getTraceId(), "NO_PERMISSION", "通用 Dubbo 调用仅允许管理员使用", Collections.<String, Object>emptyMap());
            return JsonRpcResponse.ok(request.getId(), mcpToolResult(result, true));
        }

        try {
            DubboGenericCallArgs callArgs = applyDefaultDubboSettings(objectMapper.convertValue(arguments, DubboGenericCallArgs.class));
            Object invokeResult = dubboService.invoke(callArgs);
            return JsonRpcResponse.ok(request.getId(), mcpToolResult(unifiedResult("dubbo_call", context.getTraceId(), invokeResult), false));
        } catch (RuntimeException e) {
            return JsonRpcResponse.ok(request.getId(), mcpToolResult(
                    unifiedError("dubbo_call", context.getTraceId(), classifyError(e), cleanMessage(e), sanitizedDetail(e)),
                    true
            ));
        }
    }

    private Object invokeDubbo(ToolDefinition toolDefinition, Map<String, Object> arguments) {
        DubboMapping mapping = toolDefinition.getDubboMapping();
        DubboGenericCallArgs callArgs = new DubboGenericCallArgs();
        callArgs.setInterfaceName(mapping.getInterfaceName());
        callArgs.setMethodName(mapping.getMethodName());
        callArgs.setParameterTypes(mapping.getParameterTypes());
        callArgs.setArgs(mapping.getArgumentMapper().map(arguments));
        return dubboService.invoke(applyDefaultDubboSettings(callArgs));
    }

    private void applySafetyPolicy(ToolDefinition toolDefinition, Map<String, Object> arguments, McpRequestContext context) {
        if ("write".equalsIgnoreCase(toolDefinition.getOperation())) {
            FinanceMcpProperties.Safety safety = mcpProperties.getSafety();
            if (safety == null || !safety.isWriteEnabled()) {
                throw new ToolCallException("WRITE_DISABLED", "写操作未开启", Collections.<String, Object>emptyMap());
            }
        }
        if (toolDefinition.isAdminOnly() || "admin".equalsIgnoreCase(toolDefinition.getAuthMode())) {
            if (!isAdmin(context)) {
                throw new ToolCallException("NO_PERMISSION", "当前用户不在管理员白名单", Collections.<String, Object>emptyMap());
            }
        }
        if (toolDefinition.isConfirmRequired()) {
            FinanceMcpProperties.Safety safety = mcpProperties.getSafety();
            String expected = safety == null ? null : safety.getWriteConfirmToken();
            String actual = stringValue(arguments.get("confirmToken"));
            if (!StringUtils.hasText(expected) || !expected.equals(actual)) {
                throw new ToolCallException("CONFIRM_REQUIRED", "缺少或错误的二次确认口令", Collections.<String, Object>emptyMap());
            }
        }
    }

    private void applyAuthAndIdentity(ToolDefinition toolDefinition, Map<String, Object> arguments, McpRequestContext context) {
        String authMode = toolDefinition.getAuthMode();
        boolean tokenRequired = "token".equalsIgnoreCase(authMode)
                || "preCheckContractPermission".equalsIgnoreCase(authMode)
                || toolDefinition.isInjectToken();

        if (tokenRequired) {
            String token = token(context, arguments);
            if (!StringUtils.hasText(token)) {
                throw new ToolCallException("NO_LOGIN", "缺少合同系统登录 token", Collections.<String, Object>emptyMap());
            }
            if (toolDefinition.isInjectToken()) {
                arguments.put("token", token);
            }
        }

        if (toolDefinition.isInjectDigitUser() || "operator".equalsIgnoreCase(authMode)) {
            if (context == null || !StringUtils.hasText(context.getOperatorId())) {
                throw new ToolCallException("NO_LOGIN", "缺少操作人 ID", Collections.<String, Object>emptyMap());
            }
            if (toolDefinition.isInjectDigitUser()) {
                Map<String, Object> digitUser = new LinkedHashMap<String, Object>();
                digitUser.put("userId", context.getOperatorId());
                digitUser.put("userName", context.getOperatorName());
                digitUser.put("name", context.getOperatorName());
                arguments.put("digitUser", digitUser);
            }
        }
    }

    private void applyPermissionPrecheck(ToolDefinition toolDefinition, Map<String, Object> arguments, McpRequestContext context) {
        if (!StringUtils.hasText(toolDefinition.getPermissionPrecheckTool())) {
            return;
        }

        ToolDefinition precheckTool = toolRegistry.get(toolDefinition.getPermissionPrecheckTool());
        if (precheckTool == null) {
            throw new ToolCallException("NO_PERMISSION", "权限前置校验工具不存在", Collections.<String, Object>emptyMap());
        }

        String contractNo = stringValue(arguments.get(toolDefinition.getPermissionArgumentField()));
        if (!StringUtils.hasText(contractNo)) {
            throw new ToolCallException("INVALID_ARGUMENT", toolDefinition.getPermissionArgumentField() + " is required", Collections.<String, Object>emptyMap());
        }

        Map<String, Object> precheckArgs = new LinkedHashMap<String, Object>();
        precheckArgs.put(toolDefinition.getPermissionArgumentField(), contractNo);
        applyAuthAndIdentity(precheckTool, precheckArgs, context);
        Object result = invokeDubbo(precheckTool, precheckArgs);
        if (!isBusinessSuccess(sanitizeGenericResult(result))) {
            throw new ToolCallException("NO_PERMISSION", "当前用户无权限访问该合同", Collections.singletonMap("contractNo", contractNo));
        }
    }

    private String token(McpRequestContext context, Map<String, Object> arguments) {
        if (context != null && StringUtils.hasText(context.getFinanceToken())) {
            return context.getFinanceToken();
        }
        return stringValue(arguments.get("token"));
    }

    private boolean isAdmin(McpRequestContext context) {
        FinanceMcpProperties.Safety safety = mcpProperties.getSafety();
        if (context == null || !StringUtils.hasText(context.getOperatorId()) || safety == null || safety.getAdminUserIds() == null) {
            return false;
        }
        return safety.getAdminUserIds().contains(context.getOperatorId());
    }

    private void validateRequiredArguments(ToolDefinition toolDefinition, Map<String, Object> arguments) {
        Object required = toolDefinition.getInputSchema().get("required");
        if (!(required instanceof List)) {
            return;
        }
        for (Object item : (List<?>) required) {
            String name = String.valueOf(item);
            if (!arguments.containsKey(name) || arguments.get(name) == null) {
                throw new ToolCallException("INVALID_ARGUMENT", name + " is required", Collections.<String, Object>emptyMap());
            }
        }
    }

    private Map<String, Object> mcpToolResult(Map<String, Object> unifiedResult, boolean isError) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("content", buildContent(unifiedResult));
        result.put("structuredContent", Collections.singletonMap("result", unifiedResult));
        result.put("isError", isError);
        return result;
    }

    private Map<String, Object> unifiedResult(String toolName, String traceId, Object invokeResult) {
        Object sanitizedResult = sanitizeGenericResult(invokeResult);
        boolean success = isBusinessSuccess(sanitizedResult);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", success);
        result.put("tool", toolName);
        result.put("traceId", traceId);
        if (success) {
            result.put("data", extractData(sanitizedResult));
            result.put("message", extractMessage(sanitizedResult, "查询成功"));
            result.put("raw", sanitizedResult);
        } else {
            result.put("errorCode", extractErrorCode(sanitizedResult));
            result.put("message", extractMessage(sanitizedResult, "业务接口返回失败"));
            result.put("detail", sanitizedResult);
        }
        return result;
    }

    private Map<String, Object> unifiedError(String toolName, String traceId, String errorCode, String message, Object detail) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", false);
        result.put("tool", toolName);
        result.put("traceId", traceId);
        result.put("errorCode", errorCode);
        result.put("message", message);
        result.put("detail", sanitizeGenericResult(detail));
        return result;
    }

    private Object extractData(Object value) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.containsKey("result")) {
                return map.get("result");
            }
            if (map.containsKey("data")) {
                return map.get("data");
            }
        }
        return value;
    }

    private boolean isBusinessSuccess(Object value) {
        if (!(value instanceof Map)) {
            return true;
        }
        Map<?, ?> map = (Map<?, ?>) value;
        Object success = firstPresent(map, "success", "succeed", "isSuccess");
        if (success instanceof Boolean) {
            return (Boolean) success;
        }
        if (success != null) {
            return Boolean.parseBoolean(String.valueOf(success));
        }

        Object code = firstPresent(map, "code", "errCode", "errorCode");
        if (code == null) {
            return true;
        }
        String text = String.valueOf(code);
        return "0".equals(text) || "200".equals(text) || "SUCCESS".equalsIgnoreCase(text) || "OK".equalsIgnoreCase(text);
    }

    private String extractMessage(Object value, String defaultMessage) {
        if (value instanceof Map) {
            Object message = firstPresent((Map<?, ?>) value, "message", "msg", "errorMsg");
            if (message != null && StringUtils.hasText(String.valueOf(message))) {
                return String.valueOf(message);
            }
        }
        return defaultMessage;
    }

    private String extractErrorCode(Object value) {
        if (value instanceof Map) {
            Object code = firstPresent((Map<?, ?>) value, "errorCode", "errCode", "code");
            if (code != null && StringUtils.hasText(String.valueOf(code))) {
                return String.valueOf(code);
            }
        }
        return "BUSINESS_ERROR";
    }

    private Object firstPresent(Map<?, ?> map, String... names) {
        for (String name : names) {
            if (map.containsKey(name)) {
                return map.get(name);
            }
        }
        return null;
    }

    private Object sanitizeGenericResult(Object value) {
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            Map<String, Object> target = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if ("class".equals(key)) {
                    continue;
                }
                target.put(key, sanitizeGenericResult(entry.getValue()));
            }
            return target;
        }
        if (value instanceof List) {
            List<?> source = (List<?>) value;
            List<Object> target = new ArrayList<Object>();
            for (Object item : source) {
                target.add(sanitizeGenericResult(item));
            }
            return target;
        }
        return value;
    }

    private DubboGenericCallArgs applyDefaultDubboSettings(DubboGenericCallArgs callArgs) {
        boolean contractFacade = isContractFacade(callArgs.getInterfaceName());
        if (!contractFacade
                && !StringUtils.hasText(callArgs.getDirectUrl())
                && StringUtils.hasText(dubboProperties.getDirectUrl())) {
            callArgs.setDirectUrl(dubboProperties.getDirectUrl());
        }
        boolean directCall = StringUtils.hasText(callArgs.getDirectUrl());
        if (!StringUtils.hasText(callArgs.getDirectUrl())
                && !StringUtils.hasText(callArgs.getRegistryAddress())
                && StringUtils.hasText(dubboProperties.getRegistry())) {
            callArgs.setRegistryAddress(dubboProperties.getRegistry());
        }
        if (!StringUtils.hasText(callArgs.getGroup()) && StringUtils.hasText(dubboProperties.getGroup())) {
            callArgs.setGroup(dubboProperties.getGroup());
        }
        if (!directCall && !StringUtils.hasText(callArgs.getVersion()) && StringUtils.hasText(dubboProperties.getVersion())) {
            callArgs.setVersion(dubboProperties.getVersion());
        }
        if (callArgs.getTimeoutMs() == null && dubboProperties.getDefaultTimeoutMs() != null) {
            callArgs.setTimeoutMs(dubboProperties.getDefaultTimeoutMs());
        }
        if (!directCall && dubboProperties.getReferenceParameters() != null && !dubboProperties.getReferenceParameters().isEmpty()) {
            Map<String, String> parameters = new LinkedHashMap<String, String>(dubboProperties.getReferenceParameters());
            if (callArgs.getParameters() != null) {
                parameters.putAll(callArgs.getParameters());
            }
            callArgs.setParameters(parameters);
        }
        return callArgs;
    }

    private boolean isContractFacade(String interfaceName) {
        return StringUtils.hasText(interfaceName) && interfaceName.startsWith(ZCY_FINANCE_INTERFACE_PREFIX);
    }

    private Map<String, Object> initializeResult() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Collections.singletonMap("tools", new HashMap<String, Object>()));

        Map<String, Object> serverInfo = new LinkedHashMap<String, Object>();
        serverInfo.put("name", "finance-contract-mcp-server");
        serverInfo.put("version", "0.0.1");
        result.put("serverInfo", serverInfo);
        return result;
    }

    private Map<String, Object> toolsListResult() {
        List<Map<String, Object>> tools = new ArrayList<Map<String, Object>>(toolRegistry.listMcpTools());
        if (mcpProperties.isExposeGenericDubboCall()) {
            tools.add(dubboCallTool());
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tools", tools);
        return result;
    }

    private Map<String, Object> dubboCallTool() {
        Map<String, Object> tool = new LinkedHashMap<String, Object>();
        tool.put("name", "dubbo_call");
        tool.put("description", "管理员受控工具：按白名单配置开启后调用 Dubbo 泛化接口");
        tool.put("inputSchema", dubboToolSchema());
        return tool;
    }

    private Map<String, Object> dubboToolSchema() {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("interfaceName", stringField("Dubbo interface FQN"));
        properties.put("methodName", stringField("method name"));
        properties.put("group", stringField("Dubbo group"));
        properties.put("version", stringField("Dubbo version"));
        properties.put("registryAddress", stringField("registry address, e.g. zookeeper://127.0.0.1:2181"));
        properties.put("directUrl", stringField("direct provider url, e.g. dubbo://127.0.0.1:20880"));

        Map<String, Object> referenceParameters = new LinkedHashMap<String, Object>();
        referenceParameters.put("type", "object");
        referenceParameters.put("description", "Dubbo reference parameters, e.g. {\"env\":\"beijing\"}");
        properties.put("parameters", referenceParameters);

        Map<String, Object> timeout = new LinkedHashMap<String, Object>();
        timeout.put("type", "integer");
        timeout.put("description", "timeout in ms, optional");
        properties.put("timeoutMs", timeout);

        Map<String, Object> parameterTypes = new LinkedHashMap<String, Object>();
        parameterTypes.put("type", "array");
        parameterTypes.put("description", "Java parameter types");
        parameterTypes.put("items", Collections.singletonMap("type", "string"));
        properties.put("parameterTypes", parameterTypes);

        Map<String, Object> args = new LinkedHashMap<String, Object>();
        args.put("type", "array");
        args.put("description", "arguments corresponding to parameterTypes");
        args.put("items", Collections.singletonMap("type", "object"));
        properties.put("args", args);

        schema.put("properties", properties);
        schema.put("required", buildRequired());
        return schema;
    }

    private List<String> buildRequired() {
        List<String> required = new ArrayList<String>();
        required.add("interfaceName");
        required.add("methodName");
        required.add("parameterTypes");
        required.add("args");
        return required;
    }

    private Map<String, Object> stringField(String description) {
        Map<String, Object> field = new LinkedHashMap<String, Object>();
        field.put("type", "string");
        field.put("description", description);
        return field;
    }

    private List<Map<String, Object>> buildContent(Object invokeResult) {
        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("type", "text");
        text.put("text", objectToJson(invokeResult));
        return Collections.singletonList(text);
    }

    private String objectToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {
        });
    }

    private String classifyError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ToolCallException) {
                return ((ToolCallException) current).getErrorCode();
            }
            if (current instanceof IllegalArgumentException) {
                return "INVALID_ARGUMENT";
            }
            if (current instanceof RpcException && ((RpcException) current).isTimeout()) {
                return "DUBBO_TIMEOUT";
            }
            if (current instanceof org.apache.dubbo.remoting.TimeoutException
                    || current instanceof TimeoutException
                    || current instanceof SocketTimeoutException) {
                return "DUBBO_TIMEOUT";
            }
            if (current instanceof GenericException) {
                return "BUSINESS_ERROR";
            }
            current = current.getCause();
        }
        return "DUBBO_ERROR";
    }

    private String cleanMessage(Throwable error) {
        if (error == null || !StringUtils.hasText(error.getMessage())) {
            return "调用失败";
        }
        return error.getMessage();
    }

    private Object sanitizedDetail(Throwable error) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("type", error.getClass().getName());
        detail.put("message", cleanMessage(error));
        return detail;
    }

    private void audit(ToolDefinition toolDefinition,
                       McpRequestContext context,
                       String traceId,
                       Map<String, Object> arguments,
                       boolean success,
                       String errorCode,
                       long startedAt) {
        long costMs = System.currentTimeMillis() - startedAt;
        LOGGER.info("mcp_audit traceId={} toolName={} operatorId={} operatorName={} operation={} facade={} method={} paramsDigest={} success={} errorCode={} costMs={}",
                traceId,
                toolDefinition.getName(),
                context == null ? null : context.getOperatorId(),
                context == null ? null : context.getOperatorName(),
                toolDefinition.getOperation(),
                toolDefinition.getDubboMapping().getInterfaceName(),
                toolDefinition.getDubboMapping().getMethodName(),
                paramsDigest(arguments),
                success,
                errorCode,
                costMs);
    }

    private String paramsDigest(Map<String, Object> arguments) {
        String json = objectToJson(maskSensitive(arguments));
        if (json.length() > 1000) {
            return json.substring(0, 1000);
        }
        return json;
    }

    private Object maskSensitive(Object value) {
        if (value instanceof Map) {
            Map<?, ?> source = (Map<?, ?>) value;
            Map<String, Object> target = new LinkedHashMap<String, Object>();
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key)) {
                    target.put(key, "***");
                } else {
                    target.put(key, maskSensitive(entry.getValue()));
                }
            }
            return target;
        }
        if (value instanceof List) {
            List<?> source = (List<?>) value;
            List<Object> target = new ArrayList<Object>();
            for (Object item : source) {
                target.add(maskSensitive(item));
            }
            return target;
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        String lower = key == null ? "" : key.toLowerCase();
        return lower.contains("token")
                || lower.contains("cookie")
                || lower.contains("password")
                || lower.contains("passwd")
                || lower.contains("secret");
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static class ToolCallException extends RuntimeException {
        private final String errorCode;
        private final Object detail;

        ToolCallException(String errorCode, String message, Object detail) {
            super(message);
            this.errorCode = errorCode;
            this.detail = detail;
        }

        String getErrorCode() {
            return errorCode;
        }

        Object getDetail() {
            return detail;
        }
    }
}
