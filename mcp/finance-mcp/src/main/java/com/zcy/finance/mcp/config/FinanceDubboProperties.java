package com.zcy.finance.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "finance.dubbo")
public class FinanceDubboProperties {

    private String applicationName = "finance-general-mcp";
    private String registry;
    private String directUrl;
    private String group;
    private String version;
    private Integer defaultTimeoutMs = 5000;
    private Map<String, String> referenceParameters = new LinkedHashMap<String, String>();
    private Interfaces interfaces = new Interfaces();

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getDirectUrl() {
        return directUrl;
    }

    public void setDirectUrl(String directUrl) {
        this.directUrl = directUrl;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(Integer defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public Map<String, String> getReferenceParameters() {
        return referenceParameters;
    }

    public void setReferenceParameters(Map<String, String> referenceParameters) {
        this.referenceParameters = referenceParameters;
    }

    public Interfaces getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(Interfaces interfaces) {
        this.interfaces = interfaces;
    }

    public static class Interfaces {
        private String arDashboard = "com.zcy.finance.api.ArDashboardService";
        private String arAp = "com.zcy.finance.api.ArApService";
        private String accountMapping = "com.zcy.finance.api.AccountMappingService";
        private String voucher = "com.zcy.finance.api.VoucherService";
        private String invoice = "com.zcy.finance.api.InvoiceService";
        private String report = "com.zcy.finance.api.ReportService";
        private String analysis = "com.zcy.finance.api.AnalysisService";
        private String budget = "com.zcy.finance.api.BudgetControlService";
        private String expense = "com.zcy.finance.api.ExpenseService";
        private String tax = "com.zcy.finance.api.TaxService";
        private String bank = "com.zcy.finance.api.BankReconciliationService";
        private String compliance = "com.zcy.finance.api.ComplianceRiskService";
        private String cash = "com.zcy.finance.api.CashManagementService";
        private String contractAsset = "com.zcy.finance.api.ContractAssetService";
        private String dataIntegration = "com.zcy.finance.api.DataIntegrationService";
        private String monthEndClose = "com.zcy.finance.api.MonthEndCloseService";
        private String auditLog = "com.zcy.finance.api.AuditLogService";
        private String financeContractMobile = "cn.gov.zcy.finance.contract.facade.FinanceContractMobileFacade";
        private String financeContract = "cn.gov.zcy.finance.contract.facade.FinanceContractFacade";
        private String digitContract = "cn.gov.zcy.finance.contract.facade.DigitContractFacade";
        private String receipt = "cn.gov.zcy.finance.receipt.facade.ReceiptFacade";
        private String contractCommon = "cn.gov.zcy.finance.contract.facade.ContractCommonFacade";
        private String financeDepartment = "cn.gov.zcy.finance.contract.facade.FinanceDepartmentFacade";
        private String financeProduct = "cn.gov.zcy.finance.contract.facade.FinanceProductFacade";
        private String contractProdLine = "cn.gov.zcy.finance.contract.facade.ContractProdLineFacade";
        private String projectLibrary = "cn.gov.zcy.finance.contract.facade.ProjectLibraryFacade";
        private String bankTransaction = "cn.gov.zcy.finance.contract.facade.BankTransactionFacade";

        public String getArDashboard() {
            return arDashboard;
        }

        public void setArDashboard(String arDashboard) {
            this.arDashboard = arDashboard;
        }

        public String getArAp() {
            return arAp;
        }

        public void setArAp(String arAp) {
            this.arAp = arAp;
        }

        public String getAccountMapping() {
            return accountMapping;
        }

        public void setAccountMapping(String accountMapping) {
            this.accountMapping = accountMapping;
        }

        public String getVoucher() {
            return voucher;
        }

        public void setVoucher(String voucher) {
            this.voucher = voucher;
        }

        public String getInvoice() {
            return invoice;
        }

        public void setInvoice(String invoice) {
            this.invoice = invoice;
        }

        public String getReport() {
            return report;
        }

        public void setReport(String report) {
            this.report = report;
        }

        public String getAnalysis() {
            return analysis;
        }

        public void setAnalysis(String analysis) {
            this.analysis = analysis;
        }

        public String getBudget() {
            return budget;
        }

        public void setBudget(String budget) {
            this.budget = budget;
        }

        public String getExpense() {
            return expense;
        }

        public void setExpense(String expense) {
            this.expense = expense;
        }

        public String getTax() {
            return tax;
        }

        public void setTax(String tax) {
            this.tax = tax;
        }

        public String getBank() {
            return bank;
        }

        public void setBank(String bank) {
            this.bank = bank;
        }

        public String getCompliance() {
            return compliance;
        }

        public void setCompliance(String compliance) {
            this.compliance = compliance;
        }

        public String getCash() {
            return cash;
        }

        public void setCash(String cash) {
            this.cash = cash;
        }

        public String getContractAsset() {
            return contractAsset;
        }

        public void setContractAsset(String contractAsset) {
            this.contractAsset = contractAsset;
        }

        public String getDataIntegration() {
            return dataIntegration;
        }

        public void setDataIntegration(String dataIntegration) {
            this.dataIntegration = dataIntegration;
        }

        public String getMonthEndClose() {
            return monthEndClose;
        }

        public void setMonthEndClose(String monthEndClose) {
            this.monthEndClose = monthEndClose;
        }

        public String getAuditLog() {
            return auditLog;
        }

        public void setAuditLog(String auditLog) {
            this.auditLog = auditLog;
        }

        public String getFinanceContractMobile() {
            return financeContractMobile;
        }

        public void setFinanceContractMobile(String financeContractMobile) {
            this.financeContractMobile = financeContractMobile;
        }

        public String getFinanceContract() {
            return financeContract;
        }

        public void setFinanceContract(String financeContract) {
            this.financeContract = financeContract;
        }

        public String getDigitContract() {
            return digitContract;
        }

        public void setDigitContract(String digitContract) {
            this.digitContract = digitContract;
        }

        public String getReceipt() {
            return receipt;
        }

        public void setReceipt(String receipt) {
            this.receipt = receipt;
        }

        public String getContractCommon() {
            return contractCommon;
        }

        public void setContractCommon(String contractCommon) {
            this.contractCommon = contractCommon;
        }

        public String getFinanceDepartment() {
            return financeDepartment;
        }

        public void setFinanceDepartment(String financeDepartment) {
            this.financeDepartment = financeDepartment;
        }

        public String getFinanceProduct() {
            return financeProduct;
        }

        public void setFinanceProduct(String financeProduct) {
            this.financeProduct = financeProduct;
        }

        public String getContractProdLine() {
            return contractProdLine;
        }

        public void setContractProdLine(String contractProdLine) {
            this.contractProdLine = contractProdLine;
        }

        public String getProjectLibrary() {
            return projectLibrary;
        }

        public void setProjectLibrary(String projectLibrary) {
            this.projectLibrary = projectLibrary;
        }

        public String getBankTransaction() {
            return bankTransaction;
        }

        public void setBankTransaction(String bankTransaction) {
            this.bankTransaction = bankTransaction;
        }
    }
}
