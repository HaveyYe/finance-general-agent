package com.zcy.finance.api.vo;

import java.math.BigDecimal;

public class IncomeStatementVO extends ReportVO {

    private static final long serialVersionUID = 1L;

    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal operatingCost = BigDecimal.ZERO;
    private BigDecimal totalProfit = BigDecimal.ZERO;
    private BigDecimal netProfit = BigDecimal.ZERO;

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    public BigDecimal getOperatingCost() { return operatingCost; }
    public void setOperatingCost(BigDecimal operatingCost) { this.operatingCost = operatingCost; }
    public BigDecimal getTotalProfit() { return totalProfit; }
    public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
}
