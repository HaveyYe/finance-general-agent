package com.zcy.finance.api.dto;

import java.io.Serializable;

public class CashFlowForecastDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String startPeriod;
    private Integer months;
    private String scenario;
    private String currency;

    public String getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(String startPeriod) {
        this.startPeriod = startPeriod;
    }

    public Integer getMonths() {
        return months;
    }

    public void setMonths(Integer months) {
        this.months = months;
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
}
