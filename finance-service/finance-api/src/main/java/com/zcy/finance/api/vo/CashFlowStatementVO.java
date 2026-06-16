package com.zcy.finance.api.vo;

import java.math.BigDecimal;

public class CashFlowStatementVO extends ReportVO {

    private static final long serialVersionUID = 1L;

    private BigDecimal operatingNetCashFlow = BigDecimal.ZERO;
    private BigDecimal investingNetCashFlow = BigDecimal.ZERO;
    private BigDecimal financingNetCashFlow = BigDecimal.ZERO;
    private BigDecimal netIncreaseCash = BigDecimal.ZERO;

    public BigDecimal getOperatingNetCashFlow() { return operatingNetCashFlow; }
    public void setOperatingNetCashFlow(BigDecimal operatingNetCashFlow) { this.operatingNetCashFlow = operatingNetCashFlow; }
    public BigDecimal getInvestingNetCashFlow() { return investingNetCashFlow; }
    public void setInvestingNetCashFlow(BigDecimal investingNetCashFlow) { this.investingNetCashFlow = investingNetCashFlow; }
    public BigDecimal getFinancingNetCashFlow() { return financingNetCashFlow; }
    public void setFinancingNetCashFlow(BigDecimal financingNetCashFlow) { this.financingNetCashFlow = financingNetCashFlow; }
    public BigDecimal getNetIncreaseCash() { return netIncreaseCash; }
    public void setNetIncreaseCash(BigDecimal netIncreaseCash) { this.netIncreaseCash = netIncreaseCash; }
}
