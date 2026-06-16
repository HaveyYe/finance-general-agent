package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AccountMappingRecommendationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mappingNo;
    private String documentType;
    private String businessScenario;
    private String summary;
    private String recommendationMode;
    private BigDecimal confidence;
    private Boolean autoApplicable;
    private Boolean manualReviewRequired;
    private String selectedAccountCode;
    private String selectedAccountName;
    private String selectedTaxAccountCode;
    private String selectedTaxAccountName;
    private String payableOrReceivableAccountCode;
    private String payableOrReceivableAccountName;
    private List<AccountCandidate> candidates = new ArrayList<AccountCandidate>();
    private List<VoucherPreviewEntry> voucherPreview = new ArrayList<VoucherPreviewEntry>();
    private List<String> evidence = new ArrayList<String>();
    private List<String> riskHints = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

    public String getMappingNo() { return mappingNo; }
    public void setMappingNo(String mappingNo) { this.mappingNo = mappingNo; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getBusinessScenario() { return businessScenario; }
    public void setBusinessScenario(String businessScenario) { this.businessScenario = businessScenario; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRecommendationMode() { return recommendationMode; }
    public void setRecommendationMode(String recommendationMode) { this.recommendationMode = recommendationMode; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public Boolean getAutoApplicable() { return autoApplicable; }
    public void setAutoApplicable(Boolean autoApplicable) { this.autoApplicable = autoApplicable; }
    public Boolean getManualReviewRequired() { return manualReviewRequired; }
    public void setManualReviewRequired(Boolean manualReviewRequired) { this.manualReviewRequired = manualReviewRequired; }
    public String getSelectedAccountCode() { return selectedAccountCode; }
    public void setSelectedAccountCode(String selectedAccountCode) { this.selectedAccountCode = selectedAccountCode; }
    public String getSelectedAccountName() { return selectedAccountName; }
    public void setSelectedAccountName(String selectedAccountName) { this.selectedAccountName = selectedAccountName; }
    public String getSelectedTaxAccountCode() { return selectedTaxAccountCode; }
    public void setSelectedTaxAccountCode(String selectedTaxAccountCode) { this.selectedTaxAccountCode = selectedTaxAccountCode; }
    public String getSelectedTaxAccountName() { return selectedTaxAccountName; }
    public void setSelectedTaxAccountName(String selectedTaxAccountName) { this.selectedTaxAccountName = selectedTaxAccountName; }
    public String getPayableOrReceivableAccountCode() { return payableOrReceivableAccountCode; }
    public void setPayableOrReceivableAccountCode(String payableOrReceivableAccountCode) { this.payableOrReceivableAccountCode = payableOrReceivableAccountCode; }
    public String getPayableOrReceivableAccountName() { return payableOrReceivableAccountName; }
    public void setPayableOrReceivableAccountName(String payableOrReceivableAccountName) { this.payableOrReceivableAccountName = payableOrReceivableAccountName; }
    public List<AccountCandidate> getCandidates() { return candidates; }
    public void setCandidates(List<AccountCandidate> candidates) { this.candidates = candidates; }
    public List<VoucherPreviewEntry> getVoucherPreview() { return voucherPreview; }
    public void setVoucherPreview(List<VoucherPreviewEntry> voucherPreview) { this.voucherPreview = voucherPreview; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    public List<String> getRiskHints() { return riskHints; }
    public void setRiskHints(List<String> riskHints) { this.riskHints = riskHints; }
    public List<String> getNextActions() { return nextActions; }
    public void setNextActions(List<String> nextActions) { this.nextActions = nextActions; }

    public static class AccountCandidate implements Serializable {
        private static final long serialVersionUID = 1L;

        private String accountCode;
        private String accountName;
        private String accountType;
        private BigDecimal confidence;
        private String reason;

        public AccountCandidate() {
        }

        public AccountCandidate(String accountCode, String accountName, String accountType, BigDecimal confidence, String reason) {
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.accountType = accountType;
            this.confidence = confidence;
            this.reason = reason;
        }

        public String getAccountCode() { return accountCode; }
        public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        public BigDecimal getConfidence() { return confidence; }
        public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class VoucherPreviewEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        private String accountCode;
        private String accountName;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private String explanation;

        public VoucherPreviewEntry() {
        }

        public VoucherPreviewEntry(String accountCode, String accountName, BigDecimal debitAmount, BigDecimal creditAmount, String explanation) {
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.debitAmount = debitAmount;
            this.creditAmount = creditAmount;
            this.explanation = explanation;
        }

        public String getAccountCode() { return accountCode; }
        public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public BigDecimal getDebitAmount() { return debitAmount; }
        public void setDebitAmount(BigDecimal debitAmount) { this.debitAmount = debitAmount; }
        public BigDecimal getCreditAmount() { return creditAmount; }
        public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
}
