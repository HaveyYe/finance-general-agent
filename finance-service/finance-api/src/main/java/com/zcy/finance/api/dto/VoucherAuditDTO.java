package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VoucherAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String voucherNo;
    private String voucherDate;
    private String summary;
    private String businessType;
    private String period;
    private String documentDate;
    private String relatedInvoiceNo;
    private String purchaseOrderNo;
    private String receiptNo;
    private String supplierName;
    private String department;
    private String preparerId;
    private String reviewerId;
    private BigDecimal invoiceAmount;
    private BigDecimal orderAmount;
    private BigDecimal receiptAmount;
    private BigDecimal budgetAmount;
    private List<VoucherCreateDTO.Entry> entries = new ArrayList<VoucherCreateDTO.Entry>();

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public String getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(String voucherDate) {
        this.voucherDate = voucherDate;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(String documentDate) {
        this.documentDate = documentDate;
    }

    public String getRelatedInvoiceNo() {
        return relatedInvoiceNo;
    }

    public void setRelatedInvoiceNo(String relatedInvoiceNo) {
        this.relatedInvoiceNo = relatedInvoiceNo;
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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPreparerId() {
        return preparerId;
    }

    public void setPreparerId(String preparerId) {
        this.preparerId = preparerId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
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

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public List<VoucherCreateDTO.Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<VoucherCreateDTO.Entry> entries) {
        this.entries = entries;
    }
}
