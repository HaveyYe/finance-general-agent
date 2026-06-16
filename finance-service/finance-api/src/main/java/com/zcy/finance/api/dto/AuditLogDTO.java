package com.zcy.finance.api.dto;

import java.io.Serializable;

public class AuditLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private String sessionId;
    private String channel;
    private String corpId;
    private String userId;
    private String userName;
    private String requestText;
    private String toolName;
    private String serviceName;
    private String requestArgs;
    private String responseSummary;
    private String status;
    private String errorMessage;
    private Long durationMs;
    private String occurredAt;

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getCorpId() { return corpId; }
    public void setCorpId(String corpId) { this.corpId = corpId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getRequestText() { return requestText; }
    public void setRequestText(String requestText) { this.requestText = requestText; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getRequestArgs() { return requestArgs; }
    public void setRequestArgs(String requestArgs) { this.requestArgs = requestArgs; }
    public String getResponseSummary() { return responseSummary; }
    public void setResponseSummary(String responseSummary) { this.responseSummary = responseSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
}
