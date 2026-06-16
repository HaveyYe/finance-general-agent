package com.zcy.finance.api.dto;

import java.io.Serializable;

public class ContractAssetQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String scope;
    private String department;
    private Integer reminderDays;

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getReminderDays() {
        return reminderDays;
    }

    public void setReminderDays(Integer reminderDays) {
        this.reminderDays = reminderDays;
    }
}
