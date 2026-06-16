package com.zcy.finance.api.dto;

import java.io.Serializable;

public class MonthEndCloseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String entityName;
    private String closeType;
    private Boolean includeChecklist;
    private Boolean forceClose;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getCloseType() {
        return closeType;
    }

    public void setCloseType(String closeType) {
        this.closeType = closeType;
    }

    public Boolean getIncludeChecklist() {
        return includeChecklist;
    }

    public void setIncludeChecklist(Boolean includeChecklist) {
        this.includeChecklist = includeChecklist;
    }

    public Boolean getForceClose() {
        return forceClose;
    }

    public void setForceClose(Boolean forceClose) {
        this.forceClose = forceClose;
    }
}
