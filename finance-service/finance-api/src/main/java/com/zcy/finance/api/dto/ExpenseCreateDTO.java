package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExpenseCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String employeeId;
    private String employeeName;
    private String department;
    private String projectCode;
    private String expenseType;
    private String expenseDate;
    private String description;
    private BigDecimal amount = BigDecimal.ZERO;
    private List<String> invoiceNos = new ArrayList<String>();
    private List<String> attachments = new ArrayList<String>();
    private boolean submitForApproval;

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }
    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String expenseDate) { this.expenseDate = expenseDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public List<String> getInvoiceNos() { return invoiceNos; }
    public void setInvoiceNos(List<String> invoiceNos) { this.invoiceNos = invoiceNos; }
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
    public boolean isSubmitForApproval() { return submitForApproval; }
    public void setSubmitForApproval(boolean submitForApproval) { this.submitForApproval = submitForApproval; }
}
