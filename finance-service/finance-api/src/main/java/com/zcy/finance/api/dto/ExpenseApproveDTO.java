package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExpenseApproveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String expenseNo;
    private String employeeId;
    private String employeeName;
    private String employeeLevel;
    private String department;
    private String projectCode;
    private String expenseType;
    private String cityTier;
    private String submitDate;
    private String invoiceNo;
    private String purchaseOrderNo;
    private String receiptNo;
    private String description;
    private BigDecimal amount;
    private BigDecimal invoiceAmount;
    private BigDecimal orderAmount;
    private BigDecimal receiptAmount;
    private BigDecimal availableBudget;
    private boolean invoiceVerified = true;
    private boolean duplicateInvoice;
    private boolean autoApproveEnabled;
    private List<RuleCitation> ruleCitations = new ArrayList<RuleCitation>();

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

    public String getEmployeeLevel() {
        return employeeLevel;
    }

    public void setEmployeeLevel(String employeeLevel) {
        this.employeeLevel = employeeLevel;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getCityTier() {
        return cityTier;
    }

    public void setCityTier(String cityTier) {
        this.cityTier = cityTier;
    }

    public String getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getPurchaseOrderNo() {
        return purchaseOrderNo;
    }

    public void setPurchaseOrderNo(String purchaseOrderNo) {
        this.purchaseOrderNo = purchaseOrderNo;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getInvoiceAmount() {
        return invoiceAmount;
    }

    public void setInvoiceAmount(BigDecimal invoiceAmount) {
        this.invoiceAmount = invoiceAmount;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getReceiptAmount() {
        return receiptAmount;
    }

    public void setReceiptAmount(BigDecimal receiptAmount) {
        this.receiptAmount = receiptAmount;
    }

    public BigDecimal getAvailableBudget() {
        return availableBudget;
    }

    public void setAvailableBudget(BigDecimal availableBudget) {
        this.availableBudget = availableBudget;
    }

    public boolean isInvoiceVerified() {
        return invoiceVerified;
    }

    public void setInvoiceVerified(boolean invoiceVerified) {
        this.invoiceVerified = invoiceVerified;
    }

    public boolean isDuplicateInvoice() {
        return duplicateInvoice;
    }

    public void setDuplicateInvoice(boolean duplicateInvoice) {
        this.duplicateInvoice = duplicateInvoice;
    }

    public boolean isAutoApproveEnabled() {
        return autoApproveEnabled;
    }

    public void setAutoApproveEnabled(boolean autoApproveEnabled) {
        this.autoApproveEnabled = autoApproveEnabled;
    }

    public List<RuleCitation> getRuleCitations() {
        return ruleCitations;
    }

    public void setRuleCitations(List<RuleCitation> ruleCitations) {
        this.ruleCitations = ruleCitations;
    }

    public static class RuleCitation implements Serializable {
        private static final long serialVersionUID = 1L;

        private String documentName;
        private Integer chunkNo;
        private String text;
        private BigDecimal score;

        public String getDocumentName() {
            return documentName;
        }

        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }

        public Integer getChunkNo() {
            return chunkNo;
        }

        public void setChunkNo(Integer chunkNo) {
            this.chunkNo = chunkNo;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }
    }
}
