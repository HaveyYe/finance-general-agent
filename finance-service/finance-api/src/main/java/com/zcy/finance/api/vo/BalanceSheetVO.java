package com.zcy.finance.api.vo;

import java.math.BigDecimal;

public class BalanceSheetVO extends ReportVO {

    private static final long serialVersionUID = 1L;

    private BigDecimal totalAssets = BigDecimal.ZERO;
    private BigDecimal totalLiabilities = BigDecimal.ZERO;
    private BigDecimal totalEquity = BigDecimal.ZERO;
    private boolean balanced;

    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
    public BigDecimal getTotalEquity() { return totalEquity; }
    public void setTotalEquity(BigDecimal totalEquity) { this.totalEquity = totalEquity; }
    public boolean isBalanced() { return balanced; }
    public void setBalanced(boolean balanced) { this.balanced = balanced; }
}
