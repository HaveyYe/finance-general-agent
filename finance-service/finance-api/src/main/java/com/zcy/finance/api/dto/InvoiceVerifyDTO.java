package com.zcy.finance.api.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class InvoiceVerifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String invoiceNo;
    private String invoiceCode;
    private String invoiceDate;
    private String invoiceType;
    private String buyerName;
    private String sellerName;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private String fileHash;
    private boolean includeDeductionCheck = true;
    private boolean includeArchiveCheck = true;

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getInvoiceCode() {
        return invoiceCode;
    }

    public void setInvoiceCode(String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public boolean isIncludeDeductionCheck() {
        return includeDeductionCheck;
    }

    public void setIncludeDeductionCheck(boolean includeDeductionCheck) {
        this.includeDeductionCheck = includeDeductionCheck;
    }

    public boolean isIncludeArchiveCheck() {
        return includeArchiveCheck;
    }

    public void setIncludeArchiveCheck(boolean includeArchiveCheck) {
        this.includeArchiveCheck = includeArchiveCheck;
    }
}
