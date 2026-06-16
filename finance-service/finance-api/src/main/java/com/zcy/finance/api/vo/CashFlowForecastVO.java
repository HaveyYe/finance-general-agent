package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CashFlowForecastVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String startPeriod;
    private String scenario;
    private String currency;
    private BigDecimal currentCashBalance;
    private BigDecimal safetyCashLevel;
    private BigDecimal forecastEndingBalance;
    private BigDecimal lowestBalance;
    private String liquidityLevel;
    private List<ForecastRow> forecastRows = new ArrayList<ForecastRow>();
    private List<AccountPosition> accountPositions = new ArrayList<AccountPosition>();
    private List<String> alerts = new ArrayList<String>();
    private List<String> transferAdvices = new ArrayList<String>();
    private List<String> financingSuggestions = new ArrayList<String>();
    private List<String> externalFactors = new ArrayList<String>();

    public String getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(String startPeriod) {
        this.startPeriod = startPeriod;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getCurrentCashBalance() {
        return currentCashBalance;
    }

    public void setCurrentCashBalance(BigDecimal currentCashBalance) {
        this.currentCashBalance = currentCashBalance;
    }

    public BigDecimal getSafetyCashLevel() {
        return safetyCashLevel;
    }

    public void setSafetyCashLevel(BigDecimal safetyCashLevel) {
        this.safetyCashLevel = safetyCashLevel;
    }

    public BigDecimal getForecastEndingBalance() {
        return forecastEndingBalance;
    }

    public void setForecastEndingBalance(BigDecimal forecastEndingBalance) {
        this.forecastEndingBalance = forecastEndingBalance;
    }

    public BigDecimal getLowestBalance() {
        return lowestBalance;
    }

    public void setLowestBalance(BigDecimal lowestBalance) {
        this.lowestBalance = lowestBalance;
    }

    public String getLiquidityLevel() {
        return liquidityLevel;
    }

    public void setLiquidityLevel(String liquidityLevel) {
        this.liquidityLevel = liquidityLevel;
    }

    public List<ForecastRow> getForecastRows() {
        return forecastRows;
    }

    public void setForecastRows(List<ForecastRow> forecastRows) {
        this.forecastRows = forecastRows;
    }

    public List<AccountPosition> getAccountPositions() {
        return accountPositions;
    }

    public void setAccountPositions(List<AccountPosition> accountPositions) {
        this.accountPositions = accountPositions;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }

    public List<String> getTransferAdvices() {
        return transferAdvices;
    }

    public void setTransferAdvices(List<String> transferAdvices) {
        this.transferAdvices = transferAdvices;
    }

    public List<String> getFinancingSuggestions() {
        return financingSuggestions;
    }

    public void setFinancingSuggestions(List<String> financingSuggestions) {
        this.financingSuggestions = financingSuggestions;
    }

    public List<String> getExternalFactors() {
        return externalFactors;
    }

    public void setExternalFactors(List<String> externalFactors) {
        this.externalFactors = externalFactors;
    }

    public static class ForecastRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private String period;
        private BigDecimal openingBalance;
        private BigDecimal inflowAmount;
        private BigDecimal outflowAmount;
        private BigDecimal netCashFlow;
        private BigDecimal endingBalance;
        private String warningLevel;
        private String explanation;

        public ForecastRow() {
        }

        public ForecastRow(String period, BigDecimal openingBalance, BigDecimal inflowAmount,
                           BigDecimal outflowAmount, BigDecimal endingBalance, String warningLevel,
                           String explanation) {
            this.period = period;
            this.openingBalance = openingBalance;
            this.inflowAmount = inflowAmount;
            this.outflowAmount = outflowAmount;
            this.netCashFlow = inflowAmount.subtract(outflowAmount);
            this.endingBalance = endingBalance;
            this.warningLevel = warningLevel;
            this.explanation = explanation;
        }

        public String getPeriod() {
            return period;
        }

        public void setPeriod(String period) {
            this.period = period;
        }

        public BigDecimal getOpeningBalance() {
            return openingBalance;
        }

        public void setOpeningBalance(BigDecimal openingBalance) {
            this.openingBalance = openingBalance;
        }

        public BigDecimal getInflowAmount() {
            return inflowAmount;
        }

        public void setInflowAmount(BigDecimal inflowAmount) {
            this.inflowAmount = inflowAmount;
        }

        public BigDecimal getOutflowAmount() {
            return outflowAmount;
        }

        public void setOutflowAmount(BigDecimal outflowAmount) {
            this.outflowAmount = outflowAmount;
        }

        public BigDecimal getNetCashFlow() {
            return netCashFlow;
        }

        public void setNetCashFlow(BigDecimal netCashFlow) {
            this.netCashFlow = netCashFlow;
        }

        public BigDecimal getEndingBalance() {
            return endingBalance;
        }

        public void setEndingBalance(BigDecimal endingBalance) {
            this.endingBalance = endingBalance;
        }

        public String getWarningLevel() {
            return warningLevel;
        }

        public void setWarningLevel(String warningLevel) {
            this.warningLevel = warningLevel;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }

    public static class AccountPosition implements Serializable {
        private static final long serialVersionUID = 1L;

        private String accountNo;
        private String accountName;
        private BigDecimal balance;
        private BigDecimal availableAmount;
        private String status;
        private String advice;

        public AccountPosition() {
        }

        public AccountPosition(String accountNo, String accountName, BigDecimal balance,
                               BigDecimal availableAmount, String status, String advice) {
            this.accountNo = accountNo;
            this.accountName = accountName;
            this.balance = balance;
            this.availableAmount = availableAmount;
            this.status = status;
            this.advice = advice;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public void setAccountNo(String accountNo) {
            this.accountNo = accountNo;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public BigDecimal getAvailableAmount() {
            return availableAmount;
        }

        public void setAvailableAmount(BigDecimal availableAmount) {
            this.availableAmount = availableAmount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getAdvice() {
            return advice;
        }

        public void setAdvice(String advice) {
            this.advice = advice;
        }
    }
}
