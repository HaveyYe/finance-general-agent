package com.zcy.finance.api.dto;

import java.io.Serializable;

public class VoucherQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String voucherNo;
    private String period;
    private String status;
    private String accountCode;
    private String summaryKeyword;
    private Integer pageNo;
    private Integer pageSize;

    public String getVoucherNo() { return voucherNo; }
    public void setVoucherNo(String voucherNo) { this.voucherNo = voucherNo; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public String getSummaryKeyword() { return summaryKeyword; }
    public void setSummaryKeyword(String summaryKeyword) { this.summaryKeyword = summaryKeyword; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
