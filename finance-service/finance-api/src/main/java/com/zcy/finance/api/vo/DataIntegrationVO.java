package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DataIntegrationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String period;
    private String systemType;
    private int connectorCount;
    private int healthyConnectorCount;
    private int warningConnectorCount;
    private int failedConnectorCount;
    private BigDecimal overallQualityScore;
    private long totalRecords;
    private long successRecords;
    private long failedRecords;
    private List<ConnectorStatus> connectors = new ArrayList<ConnectorStatus>();
    private List<EtlJobStatus> etlJobs = new ArrayList<EtlJobStatus>();
    private List<QualityMetric> qualityMetrics = new ArrayList<QualityMetric>();
    private List<MasterDataMapping> masterDataMappings = new ArrayList<MasterDataMapping>();
    private List<RetryTask> retryTasks = new ArrayList<RetryTask>();
    private List<String> alerts = new ArrayList<String>();
    private List<String> advices = new ArrayList<String>();

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getSystemType() { return systemType; }
    public void setSystemType(String systemType) { this.systemType = systemType; }
    public int getConnectorCount() { return connectorCount; }
    public void setConnectorCount(int connectorCount) { this.connectorCount = connectorCount; }
    public int getHealthyConnectorCount() { return healthyConnectorCount; }
    public void setHealthyConnectorCount(int healthyConnectorCount) { this.healthyConnectorCount = healthyConnectorCount; }
    public int getWarningConnectorCount() { return warningConnectorCount; }
    public void setWarningConnectorCount(int warningConnectorCount) { this.warningConnectorCount = warningConnectorCount; }
    public int getFailedConnectorCount() { return failedConnectorCount; }
    public void setFailedConnectorCount(int failedConnectorCount) { this.failedConnectorCount = failedConnectorCount; }
    public BigDecimal getOverallQualityScore() { return overallQualityScore; }
    public void setOverallQualityScore(BigDecimal overallQualityScore) { this.overallQualityScore = overallQualityScore; }
    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }
    public long getSuccessRecords() { return successRecords; }
    public void setSuccessRecords(long successRecords) { this.successRecords = successRecords; }
    public long getFailedRecords() { return failedRecords; }
    public void setFailedRecords(long failedRecords) { this.failedRecords = failedRecords; }
    public List<ConnectorStatus> getConnectors() { return connectors; }
    public void setConnectors(List<ConnectorStatus> connectors) { this.connectors = connectors; }
    public List<EtlJobStatus> getEtlJobs() { return etlJobs; }
    public void setEtlJobs(List<EtlJobStatus> etlJobs) { this.etlJobs = etlJobs; }
    public List<QualityMetric> getQualityMetrics() { return qualityMetrics; }
    public void setQualityMetrics(List<QualityMetric> qualityMetrics) { this.qualityMetrics = qualityMetrics; }
    public List<MasterDataMapping> getMasterDataMappings() { return masterDataMappings; }
    public void setMasterDataMappings(List<MasterDataMapping> masterDataMappings) { this.masterDataMappings = masterDataMappings; }
    public List<RetryTask> getRetryTasks() { return retryTasks; }
    public void setRetryTasks(List<RetryTask> retryTasks) { this.retryTasks = retryTasks; }
    public List<String> getAlerts() { return alerts; }
    public void setAlerts(List<String> alerts) { this.alerts = alerts; }
    public List<String> getAdvices() { return advices; }
    public void setAdvices(List<String> advices) { this.advices = advices; }

    public static class ConnectorStatus implements Serializable {
        private static final long serialVersionUID = 1L;

        private String connectorId;
        private String systemType;
        private String systemName;
        private String adapterType;
        private String syncMode;
        private String lastSyncTime;
        private String status;
        private long records;
        private long failedRecords;
        private String latency;
        private String message;

        public ConnectorStatus() {
        }

        public ConnectorStatus(String connectorId, String systemType, String systemName, String adapterType,
                               String syncMode, String lastSyncTime, String status, long records,
                               long failedRecords, String latency, String message) {
            this.connectorId = connectorId;
            this.systemType = systemType;
            this.systemName = systemName;
            this.adapterType = adapterType;
            this.syncMode = syncMode;
            this.lastSyncTime = lastSyncTime;
            this.status = status;
            this.records = records;
            this.failedRecords = failedRecords;
            this.latency = latency;
            this.message = message;
        }

        public String getConnectorId() { return connectorId; }
        public void setConnectorId(String connectorId) { this.connectorId = connectorId; }
        public String getSystemType() { return systemType; }
        public void setSystemType(String systemType) { this.systemType = systemType; }
        public String getSystemName() { return systemName; }
        public void setSystemName(String systemName) { this.systemName = systemName; }
        public String getAdapterType() { return adapterType; }
        public void setAdapterType(String adapterType) { this.adapterType = adapterType; }
        public String getSyncMode() { return syncMode; }
        public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
        public String getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(String lastSyncTime) { this.lastSyncTime = lastSyncTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getRecords() { return records; }
        public void setRecords(long records) { this.records = records; }
        public long getFailedRecords() { return failedRecords; }
        public void setFailedRecords(long failedRecords) { this.failedRecords = failedRecords; }
        public String getLatency() { return latency; }
        public void setLatency(String latency) { this.latency = latency; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class EtlJobStatus implements Serializable {
        private static final long serialVersionUID = 1L;

        private String jobId;
        private String jobName;
        private String triggerType;
        private String currentStep;
        private String status;
        private BigDecimal progress;
        private String checkpoint;
        private String nextRunTime;
        private String message;

        public EtlJobStatus() {
        }

        public EtlJobStatus(String jobId, String jobName, String triggerType, String currentStep, String status,
                            BigDecimal progress, String checkpoint, String nextRunTime, String message) {
            this.jobId = jobId;
            this.jobName = jobName;
            this.triggerType = triggerType;
            this.currentStep = currentStep;
            this.status = status;
            this.progress = progress;
            this.checkpoint = checkpoint;
            this.nextRunTime = nextRunTime;
            this.message = message;
        }

        public String getJobId() { return jobId; }
        public void setJobId(String jobId) { this.jobId = jobId; }
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        public String getTriggerType() { return triggerType; }
        public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public BigDecimal getProgress() { return progress; }
        public void setProgress(BigDecimal progress) { this.progress = progress; }
        public String getCheckpoint() { return checkpoint; }
        public void setCheckpoint(String checkpoint) { this.checkpoint = checkpoint; }
        public String getNextRunTime() { return nextRunTime; }
        public void setNextRunTime(String nextRunTime) { this.nextRunTime = nextRunTime; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class QualityMetric implements Serializable {
        private static final long serialVersionUID = 1L;

        private String dimension;
        private BigDecimal score;
        private BigDecimal threshold;
        private String status;
        private String issue;
        private String action;

        public QualityMetric() {
        }

        public QualityMetric(String dimension, BigDecimal score, BigDecimal threshold, String status, String issue, String action) {
            this.dimension = dimension;
            this.score = score;
            this.threshold = threshold;
            this.status = status;
            this.issue = issue;
            this.action = action;
        }

        public String getDimension() { return dimension; }
        public void setDimension(String dimension) { this.dimension = dimension; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public BigDecimal getThreshold() { return threshold; }
        public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getIssue() { return issue; }
        public void setIssue(String issue) { this.issue = issue; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }

    public static class MasterDataMapping implements Serializable {
        private static final long serialVersionUID = 1L;

        private String mappingType;
        private String sourceSystem;
        private long sourceCount;
        private long mappedCount;
        private long conflictCount;
        private BigDecimal matchRate;
        private String strategy;

        public MasterDataMapping() {
        }

        public MasterDataMapping(String mappingType, String sourceSystem, long sourceCount, long mappedCount,
                                 long conflictCount, BigDecimal matchRate, String strategy) {
            this.mappingType = mappingType;
            this.sourceSystem = sourceSystem;
            this.sourceCount = sourceCount;
            this.mappedCount = mappedCount;
            this.conflictCount = conflictCount;
            this.matchRate = matchRate;
            this.strategy = strategy;
        }

        public String getMappingType() { return mappingType; }
        public void setMappingType(String mappingType) { this.mappingType = mappingType; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
        public long getSourceCount() { return sourceCount; }
        public void setSourceCount(long sourceCount) { this.sourceCount = sourceCount; }
        public long getMappedCount() { return mappedCount; }
        public void setMappedCount(long mappedCount) { this.mappedCount = mappedCount; }
        public long getConflictCount() { return conflictCount; }
        public void setConflictCount(long conflictCount) { this.conflictCount = conflictCount; }
        public BigDecimal getMatchRate() { return matchRate; }
        public void setMatchRate(BigDecimal matchRate) { this.matchRate = matchRate; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
    }

    public static class RetryTask implements Serializable {
        private static final long serialVersionUID = 1L;

        private String taskId;
        private String connectorId;
        private String failedStep;
        private String reason;
        private int retryCount;
        private String nextRetryTime;
        private String owner;

        public RetryTask() {
        }

        public RetryTask(String taskId, String connectorId, String failedStep, String reason,
                         int retryCount, String nextRetryTime, String owner) {
            this.taskId = taskId;
            this.connectorId = connectorId;
            this.failedStep = failedStep;
            this.reason = reason;
            this.retryCount = retryCount;
            this.nextRetryTime = nextRetryTime;
            this.owner = owner;
        }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getConnectorId() { return connectorId; }
        public void setConnectorId(String connectorId) { this.connectorId = connectorId; }
        public String getFailedStep() { return failedStep; }
        public void setFailedStep(String failedStep) { this.failedStep = failedStep; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        public String getNextRetryTime() { return nextRetryTime; }
        public void setNextRetryTime(String nextRetryTime) { this.nextRetryTime = nextRetryTime; }
        public String getOwner() { return owner; }
        public void setOwner(String owner) { this.owner = owner; }
    }
}
