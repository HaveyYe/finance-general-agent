package com.zcy.finance.api.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExpenseApprovalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String expenseNo;
    private String approvalStatus;
    private String riskLevel;
    private BigDecimal amount = BigDecimal.ZERO;
    private BigDecimal standardLimit = BigDecimal.ZERO;
    private BigDecimal budgetAfterSubmit = BigDecimal.ZERO;
    private boolean autoApproved;
    private String approvalOpinion;
    private List<ApprovalNode> approvalRoute = new ArrayList<ApprovalNode>();
    private List<RiskItem> riskItems = new ArrayList<RiskItem>();
    private List<RuleCitation> ruleCitations = new ArrayList<RuleCitation>();
    private List<String> advices = new ArrayList<String>();
    private List<String> nextActions = new ArrayList<String>();

    public String getExpenseNo() {
        return expenseNo;
    }

    public void setExpenseNo(String expenseNo) {
        this.expenseNo = expenseNo;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getStandardLimit() {
        return standardLimit;
    }

    public void setStandardLimit(BigDecimal standardLimit) {
        this.standardLimit = standardLimit;
    }

    public BigDecimal getBudgetAfterSubmit() {
        return budgetAfterSubmit;
    }

    public void setBudgetAfterSubmit(BigDecimal budgetAfterSubmit) {
        this.budgetAfterSubmit = budgetAfterSubmit;
    }

    public boolean isAutoApproved() {
        return autoApproved;
    }

    public void setAutoApproved(boolean autoApproved) {
        this.autoApproved = autoApproved;
    }

    public String getApprovalOpinion() {
        return approvalOpinion;
    }

    public void setApprovalOpinion(String approvalOpinion) {
        this.approvalOpinion = approvalOpinion;
    }

    public List<ApprovalNode> getApprovalRoute() {
        return approvalRoute;
    }

    public void setApprovalRoute(List<ApprovalNode> approvalRoute) {
        this.approvalRoute = approvalRoute;
    }

    public List<RiskItem> getRiskItems() {
        return riskItems;
    }

    public void setRiskItems(List<RiskItem> riskItems) {
        this.riskItems = riskItems;
    }

    public List<RuleCitation> getRuleCitations() {
        return ruleCitations;
    }

    public void setRuleCitations(List<RuleCitation> ruleCitations) {
        this.ruleCitations = ruleCitations;
    }

    public List<String> getAdvices() {
        return advices;
    }

    public void setAdvices(List<String> advices) {
        this.advices = advices;
    }

    public List<String> getNextActions() {
        return nextActions;
    }

    public void setNextActions(List<String> nextActions) {
        this.nextActions = nextActions;
    }

    public static class ApprovalNode implements Serializable {
        private static final long serialVersionUID = 1L;

        private String nodeCode;
        private String nodeName;
        private String approver;
        private String status;
        private String reason;

        public ApprovalNode() {
        }

        public ApprovalNode(String nodeCode, String nodeName, String approver, String status, String reason) {
            this.nodeCode = nodeCode;
            this.nodeName = nodeName;
            this.approver = approver;
            this.status = status;
            this.reason = reason;
        }

        public String getNodeCode() {
            return nodeCode;
        }

        public void setNodeCode(String nodeCode) {
            this.nodeCode = nodeCode;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getApprover() {
            return approver;
        }

        public void setApprover(String approver) {
            this.approver = approver;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class RiskItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String riskId;
        private String severity;
        private String title;
        private String description;
        private String suggestion;

        public RiskItem() {
        }

        public RiskItem(String riskId, String severity, String title, String description, String suggestion) {
            this.riskId = riskId;
            this.severity = severity;
            this.title = title;
            this.description = description;
            this.suggestion = suggestion;
        }

        public String getRiskId() {
            return riskId;
        }

        public void setRiskId(String riskId) {
            this.riskId = riskId;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }

    public static class RuleCitation implements Serializable {
        private static final long serialVersionUID = 1L;

        private String documentName;
        private Integer chunkNo;
        private String text;
        private BigDecimal score;

        public RuleCitation() {
        }

        public RuleCitation(String documentName, Integer chunkNo, String text, BigDecimal score) {
            this.documentName = documentName;
            this.chunkNo = chunkNo;
            this.text = text;
            this.score = score;
        }

        public String getDocumentName() {
            return documentName;
        }

        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }

        public Integer getChunkNo() {
            return chunkNo;
        }

        public void setChunkNo(Integer chunkNo) {
            this.chunkNo = chunkNo;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }
    }
}
