package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class ExpenseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String expenseNo;
    private String employeeId;
    private String employeeName;
    private String department;
    private String projectCode;
    private String expenseType;
    private String expenseDate;
    private BigDecimal amount;
    private String status;
    private String description;
    private int invoiceCount;
    private int attachmentCount;
    private String riskHint;

    public String getExpenseNo() {
        return expenseNo;
    }

    public void setExpenseNo(String expenseNo) {
        this.expenseNo = expenseNo;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }

    public String getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(String expenseDate) {
        this.expenseDate = expenseDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getInvoiceCount() { return invoiceCount; }
    public void setInvoiceCount(int invoiceCount) { this.invoiceCount = invoiceCount; }
    public int getAttachmentCount() { return attachmentCount; }
    public void setAttachmentCount(int attachmentCount) { this.attachmentCount = attachmentCount; }

    public String getRiskHint() {
        return riskHint;
    }

    public void setRiskHint(String riskHint) {
        this.riskHint = riskHint;
    }
}
