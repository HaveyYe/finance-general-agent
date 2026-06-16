package com.zcy.finance.mcp.service;

import org.springframework.util.StringUtils;

import java.util.UUID;

public class McpRequestContext {

    private final String traceId;
    private final String financeToken;
    private final String operatorId;
    private final String operatorName;

    public McpRequestContext(String traceId, String financeToken, String operatorId, String operatorName) {
        this.traceId = StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString();
        this.financeToken = financeToken;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getFinanceToken() {
        return financeToken;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }
}
