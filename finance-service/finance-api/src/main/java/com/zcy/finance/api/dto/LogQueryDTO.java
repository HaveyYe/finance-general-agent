package com.zcy.finance.api.dto;

import java.io.Serializable;

public class LogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String toolName;
    private String status;
    private String channel;
    private String userId;
    private Integer pageNo;
    private Integer pageSize;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
