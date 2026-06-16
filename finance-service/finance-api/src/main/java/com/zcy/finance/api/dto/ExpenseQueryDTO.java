package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExpenseQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String employeeId;
    private String status;
    private List<String> dateRange = new ArrayList<String>();

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getDateRange() {
        return dateRange;
    }

    public void setDateRange(List<String> dateRange) {
        this.dateRange = dateRange;
    }
}
