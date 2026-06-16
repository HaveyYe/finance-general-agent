package com.zcy.finance.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "finance.mcp")
public class FinanceMcpProperties {

    private boolean includeDefaultTools = true;
    private boolean exposeGenericDubboCall = false;
    private List<Tool> tools = new ArrayList<Tool>();
    private Auth auth = new Auth();
    private Identity identity = new Identity();
    private Safety safety = new Safety();

    public boolean isIncludeDefaultTools() {
        return includeDefaultTools;
    }

    public void setIncludeDefaultTools(boolean includeDefaultTools) {
        this.includeDefaultTools = includeDefaultTools;
    }

    public boolean isExposeGenericDubboCall() {
        return exposeGenericDubboCall;
    }

    public void setExposeGenericDubboCall(boolean exposeGenericDubboCall) {
        this.exposeGenericDubboCall = exposeGenericDubboCall;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public Safety getSafety() {
        return safety;
    }

    public void setSafety(Safety safety) {
        this.safety = safety;
    }

    public static class Auth {
        private boolean enabled = false;
        private String token;
        private String headerName = "Authorization";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }

    public static class Identity {
        private String tokenHeader = "X-Finance-Token";
        private String operatorIdHeader = "X-Operator-Id";
        private String operatorNameHeader = "X-Operator-Name";

        public String getTokenHeader() {
            return tokenHeader;
        }

        public void setTokenHeader(String tokenHeader) {
            this.tokenHeader = tokenHeader;
        }

        public String getOperatorIdHeader() {
            return operatorIdHeader;
        }

        public void setOperatorIdHeader(String operatorIdHeader) {
            this.operatorIdHeader = operatorIdHeader;
        }

        public String getOperatorNameHeader() {
            return operatorNameHeader;
        }

        public void setOperatorNameHeader(String operatorNameHeader) {
            this.operatorNameHeader = operatorNameHeader;
        }
    }

    public static class Safety {
        private boolean writeEnabled = false;
        private String writeConfirmToken;
        private List<String> adminUserIds = new ArrayList<String>();

        public boolean isWriteEnabled() {
            return writeEnabled;
        }

        public void setWriteEnabled(boolean writeEnabled) {
            this.writeEnabled = writeEnabled;
        }

        public String getWriteConfirmToken() {
            return writeConfirmToken;
        }

        public void setWriteConfirmToken(String writeConfirmToken) {
            this.writeConfirmToken = writeConfirmToken;
        }

        public List<String> getAdminUserIds() {
            return adminUserIds;
        }

        public void setAdminUserIds(List<String> adminUserIds) {
            this.adminUserIds = adminUserIds;
        }
    }

    public static class Tool {
        private String name;
        private String description;
        private String operation = "read";
        private String authMode = "none";
        private boolean confirmRequired = false;
        private boolean adminOnly = false;
        private String permissionPrecheckTool;
        private String permissionArgumentField = "contractNo";
        private boolean injectToken = false;
        private boolean injectDigitUser = false;
        private Map<String, Object> inputSchema = new LinkedHashMap<String, Object>();
        private Dubbo dubbo = new Dubbo();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getAuthMode() {
            return authMode;
        }

        public void setAuthMode(String authMode) {
            this.authMode = authMode;
        }

        public boolean isConfirmRequired() {
            return confirmRequired;
        }

        public void setConfirmRequired(boolean confirmRequired) {
            this.confirmRequired = confirmRequired;
        }

        public boolean isAdminOnly() {
            return adminOnly;
        }

        public void setAdminOnly(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }

        public String getPermissionPrecheckTool() {
            return permissionPrecheckTool;
        }

        public void setPermissionPrecheckTool(String permissionPrecheckTool) {
            this.permissionPrecheckTool = permissionPrecheckTool;
        }

        public String getPermissionArgumentField() {
            return permissionArgumentField;
        }

        public void setPermissionArgumentField(String permissionArgumentField) {
            this.permissionArgumentField = permissionArgumentField;
        }

        public boolean isInjectToken() {
            return injectToken;
        }

        public void setInjectToken(boolean injectToken) {
            this.injectToken = injectToken;
        }

        public boolean isInjectDigitUser() {
            return injectDigitUser;
        }

        public void setInjectDigitUser(boolean injectDigitUser) {
            this.injectDigitUser = injectDigitUser;
        }

        public Map<String, Object> getInputSchema() {
            return inputSchema;
        }

        public void setInputSchema(Map<String, Object> inputSchema) {
            this.inputSchema = inputSchema;
        }

        public Dubbo getDubbo() {
            return dubbo;
        }

        public void setDubbo(Dubbo dubbo) {
            this.dubbo = dubbo;
        }
    }

    public static class Dubbo {
        private String interfaceName;
        private String methodName;
        private List<String> parameterTypes = new ArrayList<String>();
        private String argumentMode = "object";
        private List<String> argumentFields = new ArrayList<String>();
        private String argumentField;

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public List<String> getParameterTypes() {
            return parameterTypes;
        }

        public void setParameterTypes(List<String> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        public String getArgumentMode() {
            return argumentMode;
        }

        public void setArgumentMode(String argumentMode) {
            this.argumentMode = argumentMode;
        }

        public List<String> getArgumentFields() {
            return argumentFields;
        }

        public void setArgumentFields(List<String> argumentFields) {
            this.argumentFields = argumentFields;
        }

        public String getArgumentField() {
            return argumentField;
        }

        public void setArgumentField(String argumentField) {
            this.argumentField = argumentField;
        }
    }
}
