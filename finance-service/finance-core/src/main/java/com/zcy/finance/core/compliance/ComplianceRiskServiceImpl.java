package com.zcy.finance.core.compliance;

import com.zcy.finance.api.ComplianceRiskService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.ComplianceRiskQueryDTO;
import com.zcy.finance.api.vo.BankTransactionVO;
import com.zcy.finance.api.vo.ComplianceRiskVO;
import com.zcy.finance.api.vo.ExpenseVO;
import com.zcy.finance.api.vo.InvoiceVO;
import com.zcy.finance.infra.bank.BankTransactionRepository;
import com.zcy.finance.infra.expense.ExpenseRepository;
import com.zcy.finance.infra.invoice.InvoiceRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = ComplianceRiskService.class)
public class ComplianceRiskServiceImpl implements ComplianceRiskService {

    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final BankTransactionRepository bankRepository;

    public ComplianceRiskServiceImpl(InvoiceRepository invoiceRepository, ExpenseRepository expenseRepository,
                                     BankTransactionRepository bankRepository) {
        this.invoiceRepository = invoiceRepository;
        this.expenseRepository = expenseRepository;
        this.bankRepository = bankRepository;
    }

    @Override
    public Result<ComplianceRiskVO> assess(ComplianceRiskQueryDTO dto) {
        String period = dto != null && hasText(dto.getPeriod()) ? dto.getPeriod() : "2026-05";
        String scenario = dto != null && hasText(dto.getScenario()) ? dto.getScenario() : "all";
        String minSeverity = dto != null && hasText(dto.getMinSeverity()) ? dto.getMinSeverity() : "LOW";

        List<ComplianceRiskVO.RiskItem> candidates = new ArrayList<ComplianceRiskVO.RiskItem>();
        collectInvoiceRisks(candidates);
        collectExpenseRisks(candidates);
        collectBankRisks(candidates);
        collectArRisks(candidates, period);

        List<ComplianceRiskVO.RiskItem> filtered = new ArrayList<ComplianceRiskVO.RiskItem>();
        for (ComplianceRiskVO.RiskItem item : candidates) {
            if (!matchesScenario(item, scenario)) {
                continue;
            }
            if (severityWeight(item.getSeverity()) < severityWeight(minSeverity)) {
                continue;
            }
            filtered.add(item);
        }

        ComplianceRiskVO vo = new ComplianceRiskVO();
        vo.setPeriod(period);
        vo.setScenario(scenario);
        vo.setRiskItems(filtered);
        vo.setTotalRiskCount(filtered.size());
        vo.setHighRiskCount(countHighRisks(filtered));
        vo.setRiskScore(score(filtered));
        vo.setOverallLevel(overallLevel(vo.getRiskScore(), vo.getHighRiskCount()));
        vo.setAdvices(Arrays.asList(
                "优先处理高风险发票、银行未达账和超规则报销，避免影响月结和税务申报。",
                "对待复核单据建立责任人和截止日期，超过 7 天自动升级给财务主管。",
                "将已确认风险项回写凭证审核规则，减少同类问题重复出现。"
        ));
        vo.setNextActions(Arrays.asList(
                "生成本期风险清单并分派责任人",
                "复核待验真发票和高金额报销",
                "对银行 RISK/UNMATCHED 流水发起凭证或审批补录"
        ));
        return Result.success(vo);
    }

    private void collectInvoiceRisks(List<ComplianceRiskVO.RiskItem> risks) {
        for (InvoiceVO invoice : invoiceRepository.findAll()) {
            if (!"已验真".equals(invoice.getVerifyStatus())) {
                risks.add(item(
                        "RISK-INV-" + invoice.getInvoiceNo(),
                        "invoice",
                        "HIGH",
                        "发票待复核",
                        "发票验真状态为" + invoice.getVerifyStatus() + "，入账前需要补充查验。",
                        invoice.getInvoiceNo(),
                        invoice.getAmount(),
                        "税务会计",
                        "2026-06-03",
                        "OPEN",
                        "先完成税务查验，再允许生成正式凭证。"
                ));
            }
        }
    }

    private void collectExpenseRisks(List<ComplianceRiskVO.RiskItem> risks) {
        for (ExpenseVO expense : expenseRepository.findAll()) {
            if ("PENDING".equals(expense.getStatus()) || "REJECTED".equals(expense.getStatus())) {
                risks.add(item(
                        "RISK-EXP-" + expense.getExpenseNo(),
                        "expense",
                        "PENDING".equals(expense.getStatus()) ? "MEDIUM" : "HIGH",
                        "报销单据异常",
                        expense.getRiskHint(),
                        expense.getExpenseNo(),
                        expense.getAmount(),
                        expense.getEmployeeName(),
                        "2026-06-04",
                        "OPEN",
                        "补齐附件和审批意见后再进入付款或凭证流程。"
                ));
            }
        }
    }

    private void collectBankRisks(List<ComplianceRiskVO.RiskItem> risks) {
        for (BankTransactionVO row : bankRepository.findAll()) {
            if ("UNMATCHED".equals(row.getStatus()) || "RISK".equals(row.getStatus())) {
                risks.add(item(
                        "RISK-BANK-" + row.getTransactionNo(),
                        "bank",
                        "RISK".equals(row.getStatus()) ? "HIGH" : "MEDIUM",
                        "银行流水未完成勾对",
                        row.getRiskHint(),
                        row.getTransactionNo(),
                        row.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 ? row.getDebitAmount() : row.getCreditAmount(),
                        "出纳",
                        "2026-06-02",
                        "OPEN",
                        "关联报销、发票或供应商付款单，确认后生成待复核凭证。"
                ));
            }
        }
    }

    private void collectArRisks(List<ComplianceRiskVO.RiskItem> risks, String period) {
        risks.add(item(
                "RISK-AR-OVERDUE-" + period,
                "ar",
                "MEDIUM",
                "90天以上应收余额偏高",
                "本期 90 天以上逾期应收为 218900.00，存在坏账和现金流压力。",
                "AR-DASHBOARD-" + period,
                new BigDecimal("218900.00"),
                "应收会计",
                "2026-06-05",
                "OPEN",
                "生成重点客户催收计划，并同步销售负责人跟进。"
        ));
    }

    private ComplianceRiskVO.RiskItem item(String riskId, String module, String severity, String title,
                                           String description, String relatedDocNo, BigDecimal amount,
                                           String owner, String dueDate, String status, String suggestion) {
        return new ComplianceRiskVO.RiskItem(riskId, module, severity, title, description, relatedDocNo,
                amount, owner, dueDate, status, suggestion);
    }

    private boolean matchesScenario(ComplianceRiskVO.RiskItem item, String scenario) {
        if (!hasText(scenario) || "all".equalsIgnoreCase(scenario)) {
            return true;
        }
        String normalized = scenario.toLowerCase();
        return item.getModule().equalsIgnoreCase(normalized) || item.getTitle().toLowerCase().contains(normalized);
    }

    private int countHighRisks(List<ComplianceRiskVO.RiskItem> risks) {
        int count = 0;
        for (ComplianceRiskVO.RiskItem item : risks) {
            if ("HIGH".equals(item.getSeverity())) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal score(List<ComplianceRiskVO.RiskItem> risks) {
        int total = 0;
        for (ComplianceRiskVO.RiskItem item : risks) {
            total += severityWeight(item.getSeverity()) * 12;
        }
        return new BigDecimal(Math.min(100, total));
    }

    private String overallLevel(BigDecimal riskScore, int highRiskCount) {
        if (highRiskCount >= 2 || riskScore.compareTo(new BigDecimal("70")) >= 0) {
            return "HIGH";
        }
        if (riskScore.compareTo(new BigDecimal("35")) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private int severityWeight(String severity) {
        if ("HIGH".equalsIgnoreCase(severity)) {
            return 3;
        }
        if ("MEDIUM".equalsIgnoreCase(severity)) {
            return 2;
        }
        return 1;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
