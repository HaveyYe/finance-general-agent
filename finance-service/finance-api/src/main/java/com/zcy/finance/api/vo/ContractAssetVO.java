package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ContractAssetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String scope;
    private int activeContractCount;
    private int dueMilestoneCount;
    private int overdueMilestoneCount;
    private int assetCount;
    private BigDecimal contractAmount;
    private BigDecimal monthlyDepreciation;
    private BigDecimal intangibleAmortization;
    private List<ContractSummary> contracts = new ArrayList<ContractSummary>();
    private List<PaymentMilestone> milestones = new ArrayList<PaymentMilestone>();
    private List<DepreciationRow> depreciationRows = new ArrayList<DepreciationRow>();
    private List<InventoryException> inventoryExceptions = new ArrayList<InventoryException>();
    private List<IntangibleReminder> intangibleReminders = new ArrayList<IntangibleReminder>();
    private List<String> advices = new ArrayList<String>();

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getActiveContractCount() {
        return activeContractCount;
    }

    public void setActiveContractCount(int activeContractCount) {
        this.activeContractCount = activeContractCount;
    }

    public int getDueMilestoneCount() {
        return dueMilestoneCount;
    }

    public void setDueMilestoneCount(int dueMilestoneCount) {
        this.dueMilestoneCount = dueMilestoneCount;
    }

    public int getOverdueMilestoneCount() {
        return overdueMilestoneCount;
    }

    public void setOverdueMilestoneCount(int overdueMilestoneCount) {
        this.overdueMilestoneCount = overdueMilestoneCount;
    }

    public int getAssetCount() {
        return assetCount;
    }

    public void setAssetCount(int assetCount) {
        this.assetCount = assetCount;
    }

    public BigDecimal getContractAmount() {
        return contractAmount;
    }

    public void setContractAmount(BigDecimal contractAmount) {
        this.contractAmount = contractAmount;
    }

    public BigDecimal getMonthlyDepreciation() {
        return monthlyDepreciation;
    }

    public void setMonthlyDepreciation(BigDecimal monthlyDepreciation) {
        this.monthlyDepreciation = monthlyDepreciation;
    }

    public BigDecimal getIntangibleAmortization() {
        return intangibleAmortization;
    }

    public void setIntangibleAmortization(BigDecimal intangibleAmortization) {
        this.intangibleAmortization = intangibleAmortization;
    }

    public List<ContractSummary> getContracts() {
        return contracts;
    }

    public void setContracts(List<ContractSummary> contracts) {
        this.contracts = contracts;
    }

    public List<PaymentMilestone> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<PaymentMilestone> milestones) {
        this.milestones = milestones;
    }

    public List<DepreciationRow> getDepreciationRows() {
        return depreciationRows;
    }

    public void setDepreciationRows(List<DepreciationRow> depreciationRows) {
        this.depreciationRows = depreciationRows;
    }

    public List<InventoryException> getInventoryExceptions() {
        return inventoryExceptions;
    }

    public void setInventoryExceptions(List<InventoryException> inventoryExceptions) {
        this.inventoryExceptions = inventoryExceptions;
    }

    public List<IntangibleReminder> getIntangibleReminders() {
        return intangibleReminders;
    }

    public void setIntangibleReminders(List<IntangibleReminder> intangibleReminders) {
        this.intangibleReminders = intangibleReminders;
    }

    public List<String> getAdvices() {
        return advices;
    }

    public void setAdvices(List<String> advices) {
        this.advices = advices;
    }

    public static class ContractSummary implements Serializable {
        private static final long serialVersionUID = 1L;

        private String contractNo;
        private String counterparty;
        private String contractType;
        private BigDecimal amount;
        private String startDate;
        private String endDate;
        private String status;
        private String confidence;
        private String riskHint;

        public ContractSummary() {
        }

        public ContractSummary(String contractNo, String counterparty, String contractType, BigDecimal amount,
                               String startDate, String endDate, String status, String confidence, String riskHint) {
            this.contractNo = contractNo;
            this.counterparty = counterparty;
            this.contractType = contractType;
            this.amount = amount;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.confidence = confidence;
            this.riskHint = riskHint;
        }

        public String getContractNo() { return contractNo; }
        public void setContractNo(String contractNo) { this.contractNo = contractNo; }
        public String getCounterparty() { return counterparty; }
        public void setCounterparty(String counterparty) { this.counterparty = counterparty; }
        public String getContractType() { return contractType; }
        public void setContractType(String contractType) { this.contractType = contractType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getConfidence() { return confidence; }
        public void setConfidence(String confidence) { this.confidence = confidence; }
        public String getRiskHint() { return riskHint; }
        public void setRiskHint(String riskHint) { this.riskHint = riskHint; }
    }

    public static class PaymentMilestone implements Serializable {
        private static final long serialVersionUID = 1L;

        private String milestoneNo;
        private String contractNo;
        private String milestoneType;
        private String dueDate;
        private BigDecimal amount;
        private String owner;
        private String status;
        private String reminderLevel;
        private String action;

        public PaymentMilestone() {
        }

        public PaymentMilestone(String milestoneNo, String contractNo, String milestoneType, String dueDate,
                                BigDecimal amount, String owner, String status, String reminderLevel, String action) {
            this.milestoneNo = milestoneNo;
            this.contractNo = contractNo;
            this.milestoneType = milestoneType;
            this.dueDate = dueDate;
            this.amount = amount;
            this.owner = owner;
            this.status = status;
            this.reminderLevel = reminderLevel;
            this.action = action;
        }

        public String getMilestoneNo() { return milestoneNo; }
        public void setMilestoneNo(String milestoneNo) { this.milestoneNo = milestoneNo; }
        public String getContractNo() { return contractNo; }
        public void setContractNo(String contractNo) { this.contractNo = contractNo; }
        public String getMilestoneType() { return milestoneType; }
        public void setMilestoneType(String milestoneType) { this.milestoneType = milestoneType; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReminderLevel() { return reminderLevel; }
        public void setReminderLevel(String reminderLevel) { this.reminderLevel = reminderLevel; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }

    public static class DepreciationRow implements Serializable {
        private static final long serialVersionUID = 1L;

        private String assetNo;
        private String assetName;
        private String category;
        private String department;
        private String depreciationMethod;
        private BigDecimal originalValue;
        private BigDecimal accumulatedDepreciation;
        private BigDecimal monthlyDepreciation;
        private BigDecimal netBookValue;
        private String voucherSuggestion;

        public DepreciationRow() {
        }

        public DepreciationRow(String assetNo, String assetName, String category, String department,
                               String depreciationMethod, BigDecimal originalValue, BigDecimal accumulatedDepreciation,
                               BigDecimal monthlyDepreciation, BigDecimal netBookValue, String voucherSuggestion) {
            this.assetNo = assetNo;
            this.assetName = assetName;
            this.category = category;
            this.department = department;
            this.depreciationMethod = depreciationMethod;
            this.originalValue = originalValue;
            this.accumulatedDepreciation = accumulatedDepreciation;
            this.monthlyDepreciation = monthlyDepreciation;
            this.netBookValue = netBookValue;
            this.voucherSuggestion = voucherSuggestion;
        }

        public String getAssetNo() { return assetNo; }
        public void setAssetNo(String assetNo) { this.assetNo = assetNo; }
        public String getAssetName() { return assetName; }
        public void setAssetName(String assetName) { this.assetName = assetName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getDepreciationMethod() { return depreciationMethod; }
        public void setDepreciationMethod(String depreciationMethod) { this.depreciationMethod = depreciationMethod; }
        public BigDecimal getOriginalValue() { return originalValue; }
        public void setOriginalValue(BigDecimal originalValue) { this.originalValue = originalValue; }
        public BigDecimal getAccumulatedDepreciation() { return accumulatedDepreciation; }
        public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciation) { this.accumulatedDepreciation = accumulatedDepreciation; }
        public BigDecimal getMonthlyDepreciation() { return monthlyDepreciation; }
        public void setMonthlyDepreciation(BigDecimal monthlyDepreciation) { this.monthlyDepreciation = monthlyDepreciation; }
        public BigDecimal getNetBookValue() { return netBookValue; }
        public void setNetBookValue(BigDecimal netBookValue) { this.netBookValue = netBookValue; }
        public String getVoucherSuggestion() { return voucherSuggestion; }
        public void setVoucherSuggestion(String voucherSuggestion) { this.voucherSuggestion = voucherSuggestion; }
    }

    public static class InventoryException implements Serializable {
        private static final long serialVersionUID = 1L;

        private String taskNo;
        private String assetNo;
        private String assetName;
        private String location;
        private String bookStatus;
        private String actualStatus;
        private String exceptionType;
        private String handler;
        private String action;

        public InventoryException() {
        }

        public InventoryException(String taskNo, String assetNo, String assetName, String location, String bookStatus,
                                  String actualStatus, String exceptionType, String handler, String action) {
            this.taskNo = taskNo;
            this.assetNo = assetNo;
            this.assetName = assetName;
            this.location = location;
            this.bookStatus = bookStatus;
            this.actualStatus = actualStatus;
            this.exceptionType = exceptionType;
            this.handler = handler;
            this.action = action;
        }

        public String getTaskNo() { return taskNo; }
        public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
        public String getAssetNo() { return assetNo; }
        public void setAssetNo(String assetNo) { this.assetNo = assetNo; }
        public String getAssetName() { return assetName; }
        public void setAssetName(String assetName) { this.assetName = assetName; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getBookStatus() { return bookStatus; }
        public void setBookStatus(String bookStatus) { this.bookStatus = bookStatus; }
        public String getActualStatus() { return actualStatus; }
        public void setActualStatus(String actualStatus) { this.actualStatus = actualStatus; }
        public String getExceptionType() { return exceptionType; }
        public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
        public String getHandler() { return handler; }
        public void setHandler(String handler) { this.handler = handler; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }

    public static class IntangibleReminder implements Serializable {
        private static final long serialVersionUID = 1L;

        private String itemNo;
        private String itemName;
        private String itemType;
        private BigDecimal originalValue;
        private BigDecimal monthlyAmortization;
        private String expiryDate;
        private String reminder;

        public IntangibleReminder() {
        }

        public IntangibleReminder(String itemNo, String itemName, String itemType, BigDecimal originalValue,
                                  BigDecimal monthlyAmortization, String expiryDate, String reminder) {
            this.itemNo = itemNo;
            this.itemName = itemName;
            this.itemType = itemType;
            this.originalValue = originalValue;
            this.monthlyAmortization = monthlyAmortization;
            this.expiryDate = expiryDate;
            this.reminder = reminder;
        }

        public String getItemNo() { return itemNo; }
        public void setItemNo(String itemNo) { this.itemNo = itemNo; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public BigDecimal getOriginalValue() { return originalValue; }
        public void setOriginalValue(BigDecimal originalValue) { this.originalValue = originalValue; }
        public BigDecimal getMonthlyAmortization() { return monthlyAmortization; }
        public void setMonthlyAmortization(BigDecimal monthlyAmortization) { this.monthlyAmortization = monthlyAmortization; }
        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
        public String getReminder() { return reminder; }
        public void setReminder(String reminder) { this.reminder = reminder; }
    }
}
