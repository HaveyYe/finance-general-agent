package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class AgingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String partnerId;
    private String partnerName;
    private String type;
    private String baseDate;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal within30Days = BigDecimal.ZERO;
    private BigDecimal days31To60 = BigDecimal.ZERO;
    private BigDecimal days61To90 = BigDecimal.ZERO;
    private BigDecimal over90Days = BigDecimal.ZERO;
    private BigDecimal overdueAmount = BigDecimal.ZERO;
    private int oldestOverdueDays;
    private String riskLevel;
    private String suggestion;

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBaseDate() { return baseDate; }
    public void setBaseDate(String baseDate) { this.baseDate = baseDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getWithin30Days() { return within30Days; }
    public void setWithin30Days(BigDecimal within30Days) { this.within30Days = within30Days; }
    public BigDecimal getDays31To60() { return days31To60; }
    public void setDays31To60(BigDecimal days31To60) { this.days31To60 = days31To60; }
    public BigDecimal getDays61To90() { return days61To90; }
    public void setDays61To90(BigDecimal days61To90) { this.days61To90 = days61To90; }
    public BigDecimal getOver90Days() { return over90Days; }
    public void setOver90Days(BigDecimal over90Days) { this.over90Days = over90Days; }
    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }
    public int getOldestOverdueDays() { return oldestOverdueDays; }
    public void setOldestOverdueDays(int oldestOverdueDays) { this.oldestOverdueDays = oldestOverdueDays; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}
