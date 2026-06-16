package com.zcy.finance.core.expense;

import com.zcy.finance.api.ExpenseService;
import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.ExpenseApproveDTO;
import com.zcy.finance.api.dto.ExpenseCreateDTO;
import com.zcy.finance.api.dto.ExpenseQueryDTO;
import com.zcy.finance.api.vo.BudgetRemainingVO;
import com.zcy.finance.api.vo.ExpenseApprovalVO;
import com.zcy.finance.api.vo.ExpenseVO;
import com.zcy.finance.infra.expense.ExpenseRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = ExpenseService.class)
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseServiceImpl(ExpenseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<ExpenseVO> createExpense(ExpenseCreateDTO dto) {
        if (dto == null || isBlank(dto.getEmployeeId()) || dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure("报销人和正数报销金额不能为空");
        }
        String date = defaultString(dto.getExpenseDate(), "2026-05-31");
        ExpenseVO vo = new ExpenseVO();
        vo.setExpenseNo(repository.nextExpenseNo(date.substring(0, 7)));
        vo.setEmployeeId(dto.getEmployeeId());
        vo.setEmployeeName(defaultString(dto.getEmployeeName(), "演示员工"));
        vo.setDepartment(defaultString(dto.getDepartment(), "销售部"));
        vo.setProjectCode(defaultString(dto.getProjectCode(), "PRJ-2026-SALES"));
        vo.setExpenseType(defaultString(dto.getExpenseType(), "差旅"));
        vo.setExpenseDate(date);
        vo.setDescription(defaultString(dto.getDescription(), "员工费用报销"));
        vo.setAmount(dto.getAmount());
        vo.setInvoiceCount(dto.getInvoiceNos() == null ? 0 : dto.getInvoiceNos().size());
        vo.setAttachmentCount(dto.getAttachments() == null ? 0 : dto.getAttachments().size());
        vo.setStatus(dto.isSubmitForApproval() ? "PENDING" : "DRAFT");
        if (vo.getInvoiceCount() == 0) {
            vo.setRiskHint("报销单已创建，但缺少发票，提交审批前需补充。");
        } else if (vo.getAttachmentCount() == 0) {
            vo.setRiskHint("报销单已创建，建议补充业务附件。");
        } else {
            vo.setRiskHint("报销单资料完整，可进入审批。");
        }
        repository.insert(vo);
        return Result.success(vo);
    }

    @Override
    public Result<PageResult<ExpenseVO>> query(ExpenseQueryDTO dto) {
        List<ExpenseVO> rows = repository.findAll();
        if (dto != null && dto.getStatus() != null && dto.getStatus().trim().length() > 0) {
            List<ExpenseVO> filtered = new ArrayList<ExpenseVO>();
            for (ExpenseVO row : rows) {
                if (dto.getStatus().equals(row.getStatus())) {
                    filtered.add(row);
                }
            }
            rows = filtered;
        }
        if (dto != null && dto.getEmployeeId() != null && dto.getEmployeeId().trim().length() > 0) {
            List<ExpenseVO> filtered = new ArrayList<ExpenseVO>();
            for (ExpenseVO row : rows) {
                if (dto.getEmployeeId().equals(row.getEmployeeId())) {
                    filtered.add(row);
                }
            }
            rows = filtered;
        }
        if (dto != null && dto.getDateRange() != null && dto.getDateRange().size() >= 2) {
            List<ExpenseVO> filtered = new ArrayList<ExpenseVO>();
            String start = dto.getDateRange().get(0);
            String end = dto.getDateRange().get(1);
            for (ExpenseVO row : rows) {
                if ((isBlank(start) || row.getExpenseDate().compareTo(start) >= 0)
                        && (isBlank(end) || row.getExpenseDate().compareTo(end) <= 0)) {
                    filtered.add(row);
                }
            }
            rows = filtered;
        }
        return Result.success(PageResult.of(rows, rows.size(), 1, rows.size()));
    }

    @Override
    public Result<ExpenseApprovalVO> approve(ExpenseApproveDTO dto) {
        if (dto == null || dto.getAmount() == null) {
            return Result.failure("报销金额不能为空");
        }

        ExpenseApprovalVO vo = new ExpenseApprovalVO();
        vo.setExpenseNo(defaultString(dto.getExpenseNo(), "EXP-AUTO-0001"));
        vo.setAmount(dto.getAmount());
        vo.setStandardLimit(resolveStandardLimit(dto));
        BigDecimal budgetAfterSubmit = zeroIfNull(dto.getAvailableBudget()).subtract(dto.getAmount());
        vo.setBudgetAfterSubmit(budgetAfterSubmit);

        List<ExpenseApprovalVO.RiskItem> risks = new ArrayList<ExpenseApprovalVO.RiskItem>();
        auditStandard(risks, dto, vo.getStandardLimit());
        auditInvoice(risks, dto);
        auditTripleMatch(risks, dto);
        auditBudget(risks, dto, budgetAfterSubmit);
        auditExpensePattern(risks, dto);
        vo.setRiskItems(risks);
        vo.setApprovalRoute(buildApprovalRoute(dto, risks));
        vo.setRuleCitations(toRuleCitations(dto));
        summarize(vo);
        applyAutoApproval(dto, vo);
        if (!isBlank(vo.getExpenseNo()) && repository.exists(vo.getExpenseNo())) {
            repository.updateApprovalStatus(vo.getExpenseNo(), vo.getApprovalStatus(), approvalRiskHint(vo));
        }
        return Result.success(vo);
    }

    @Override
    public Result<ExpenseApprovalVO> approveExpense(ExpenseApproveDTO dto) {
        return approve(dto);
    }

    @Override
    public Result<BudgetRemainingVO> queryBudgetRemaining(String department, String period) {
        if (isBlank(department) || isBlank(period)) {
            return Result.failure("部门和预算期间不能为空");
        }
        BigDecimal[] summary = repository.budgetSummary(department, period);
        BigDecimal budget = summary[0];
        BigDecimal used = summary[1];
        BigDecimal occupied = summary[2];

        BudgetRemainingVO vo = new BudgetRemainingVO();
        vo.setBudgetNo("BUD-" + period.replace("-", "") + "-" + department);
        vo.setDepartment(department);
        vo.setPeriod(period);
        vo.setBudgetAmount(budget);
        vo.setUsedAmount(used);
        vo.setOccupiedAmount(occupied);
        vo.setRemainingAmount(budget.subtract(used).subtract(occupied));
        vo.setExecutionRate(budget.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : used.add(occupied).multiply(new BigDecimal("100")).divide(budget, 2, RoundingMode.HALF_UP));
        if (vo.getExecutionRate().compareTo(new BigDecimal("90")) >= 0) {
            vo.setRiskLevel("HIGH");
            vo.setSuggestion("预算接近耗尽，新报销单需进入预算追加或强控审批。");
        } else if (vo.getExecutionRate().compareTo(new BigDecimal("75")) >= 0) {
            vo.setRiskLevel("MEDIUM");
            vo.setSuggestion("预算执行率较高，建议控制非刚性费用并关注在途占用。");
        } else {
            vo.setRiskLevel("LOW");
            vo.setSuggestion("预算余额充足，可按正常流程提交报销。");
        }
        return Result.success(vo);
    }

    private String approvalRiskHint(ExpenseApprovalVO vo) {
        if (vo.isAutoApproved()) {
            return "智能审批低风险校验通过，系统已自动通过。";
        }
        if ("REJECTED".equals(vo.getApprovalStatus())) {
            return "智能审批发现红色风险，报销单已退回整改。";
        }
        if ("NEED_REVIEW".equals(vo.getApprovalStatus())) {
            return "智能审批发现黄色风险，报销单已进入人工复核。";
        }
        return "智能审批校验通过，报销单已进入审批流。";
    }

    private void applyAutoApproval(ExpenseApproveDTO dto, ExpenseApprovalVO vo) {
        boolean hasRuleCitation = vo.getRuleCitations() != null && !vo.getRuleCitations().isEmpty();
        boolean lowRisk = "APPROVED_ROUTE_READY".equals(vo.getApprovalStatus())
                && "LOW".equals(vo.getRiskLevel())
                && !hasSeverity(vo.getRiskItems(), "RED")
                && !hasSeverity(vo.getRiskItems(), "YELLOW")
                && dto.isInvoiceVerified()
                && !dto.isDuplicateInvoice()
                && vo.getBudgetAfterSubmit().compareTo(BigDecimal.ZERO) >= 0;

        if (dto.isAutoApproveEnabled() && hasRuleCitation && lowRisk) {
            vo.setApprovalStatus("APPROVED");
            vo.setAutoApproved(true);
            vo.setApprovalOpinion("审批意见：符合费用标准、发票、预算和规则引用要求，已自动通过。");
            vo.setNextActions(Arrays.asList("自动通过完成", "可进入付款和凭证生成任务"));
            return;
        }

        vo.setAutoApproved(false);
        if (dto.isAutoApproveEnabled() && !hasRuleCitation) {
            vo.setApprovalOpinion("审批意见：未找到可引用的报销制度规则，本次不自动通过，需人工复核。");
        } else if (dto.isAutoApproveEnabled() && !lowRisk) {
            vo.setApprovalOpinion("审批意见：存在风险项或预算/发票条件不满足，本次不自动通过。");
        } else if ("APPROVED_ROUTE_READY".equals(vo.getApprovalStatus())) {
            vo.setApprovalOpinion("审批意见：规则校验通过，可进入审批流。");
        } else if ("NEED_REVIEW".equals(vo.getApprovalStatus())) {
            vo.setApprovalOpinion("审批意见：存在黄色风险，建议人工复核后再继续审批。");
        } else if ("REJECTED".equals(vo.getApprovalStatus())) {
            vo.setApprovalOpinion("审批意见：存在红色风险，建议退回整改。");
        }
    }

    private List<ExpenseApprovalVO.RuleCitation> toRuleCitations(ExpenseApproveDTO dto) {
        List<ExpenseApprovalVO.RuleCitation> result = new ArrayList<ExpenseApprovalVO.RuleCitation>();
        if (dto.getRuleCitations() == null) {
            return result;
        }
        for (ExpenseApproveDTO.RuleCitation citation : dto.getRuleCitations()) {
            if (citation == null || isBlank(citation.getDocumentName())) {
                continue;
            }
            result.add(new ExpenseApprovalVO.RuleCitation(
                    citation.getDocumentName(),
                    citation.getChunkNo(),
                    citation.getText(),
                    citation.getScore()
            ));
        }
        return result;
    }

    private void auditStandard(List<ExpenseApprovalVO.RiskItem> risks, ExpenseApproveDTO dto, BigDecimal standardLimit) {
        if (dto.getAmount().compareTo(standardLimit) > 0) {
            risks.add(risk("EXP-STD-001", "YELLOW", "超出费用标准",
                    "报销金额 " + dto.getAmount() + " 超过 " + defaultString(dto.getEmployeeLevel(), "普通员工") + " 在 " + defaultString(dto.getCityTier(), "默认城市") + " 的 " + defaultString(dto.getExpenseType(), "费用") + " 标准 " + standardLimit + "。",
                    "超标部分标红，需部门经理说明或员工调整金额。"));
        }
    }

    private void auditInvoice(List<ExpenseApprovalVO.RiskItem> risks, ExpenseApproveDTO dto) {
        if (isBlank(dto.getInvoiceNo())) {
            risks.add(risk("EXP-INV-001", "RED", "缺少发票附件",
                    "报销单未关联发票，无法完成税前扣除和归档。",
                    "补充发票后重新提交。"));
            return;
        }
        if (!dto.isInvoiceVerified()) {
            risks.add(risk("EXP-INV-002", "RED", "发票验真未通过",
                    "发票 " + dto.getInvoiceNo() + " 未通过税务查验。",
                    "拦截审批流，转人工复核或要求重新上传。"));
        }
        if (dto.isDuplicateInvoice() || "INV-202605-001".equals(dto.getInvoiceNo())) {
            risks.add(risk("EXP-INV-003", "RED", "疑似重复报销",
                    "发票 " + dto.getInvoiceNo() + " 已被历史报销单或电子档案使用。",
                    "拦截本次报销，引用原报销单或提交重复使用说明。"));
        }
        if (dto.getInvoiceAmount() != null && dto.getAmount().compareTo(dto.getInvoiceAmount()) > 0) {
            risks.add(risk("EXP-INV-004", "YELLOW", "报销金额超过发票金额",
                    "报销金额 " + dto.getAmount() + " 高于发票金额 " + dto.getInvoiceAmount() + "。",
                    "核对补贴、税额拆分或拆分多张发票。"));
        }
    }

    private void auditTripleMatch(List<ExpenseApprovalVO.RiskItem> risks, ExpenseApproveDTO dto) {
        if (!"purchase".equalsIgnoreCase(defaultString(dto.getExpenseType(), "")) && !contains(defaultString(dto.getDescription(), ""), "采购")) {
            return;
        }
        if (isBlank(dto.getPurchaseOrderNo()) || isBlank(dto.getReceiptNo())) {
            risks.add(risk("EXP-3WM-001", "YELLOW", "采购类报销缺少三单匹配资料",
                    "采购报销未完整关联采购订单和入库单。",
                    "补齐采购订单、入库单后自动重试匹配。"));
        }
        if (!amountEqual(dto.getInvoiceAmount(), dto.getOrderAmount()) || !amountEqual(dto.getInvoiceAmount(), dto.getReceiptAmount())) {
            risks.add(risk("EXP-3WM-002", "RED", "采购三单金额不一致",
                    "发票、采购订单、入库单金额不一致。",
                    "拦截审批流，推送采购和仓储复核任务。"));
        }
    }

    private void auditBudget(List<ExpenseApprovalVO.RiskItem> risks, ExpenseApproveDTO dto, BigDecimal budgetAfterSubmit) {
        if (dto.getAvailableBudget() == null) {
            risks.add(risk("EXP-BUD-001", "BLUE", "未传预算余额",
                    "当前报销未关联预算余额，无法计算预算占用率。",
                    "补充部门/项目预算后可启用强控。"));
            return;
        }
        if (budgetAfterSubmit.compareTo(BigDecimal.ZERO) < 0) {
            risks.add(risk("EXP-BUD-002", "RED", "预算不足",
                    "提交后预算余额为 " + budgetAfterSubmit + "，低于0。",
                    "拦截报销或走预算追加流程。"));
        } else if (budgetAfterSubmit.compareTo(dto.getAvailableBudget().multiply(new BigDecimal("0.05"))) <= 0) {
            risks.add(risk("EXP-BUD-003", "YELLOW", "预算接近耗尽",
                    "提交后预算余额较低，可能触发95%预算预警。",
                    "通知部门负责人关注预算执行。"));
        }
    }

    private void auditExpensePattern(List<ExpenseApprovalVO.RiskItem> risks, ExpenseApproveDTO dto) {
        String text = defaultString(dto.getDescription(), "") + defaultString(dto.getExpenseType(), "");
        if (contains(text, "餐饮") && dto.getAmount().compareTo(new BigDecimal("5000")) > 0) {
            risks.add(risk("EXP-PAT-001", "YELLOW", "大额餐饮报销",
                    "餐饮/招待类费用金额较高，需确认招待对象和业务事由。",
                    "转财务经理复核并补充招待清单。"));
        }
        if (contains(text, "研发") && contains(text, "招待")) {
            risks.add(risk("EXP-PAT-002", "YELLOW", "项目费用类型受限",
                    "研发项目疑似列支招待费，可能不符合项目费用规则。",
                    "调整费用类型或补充项目负责人审批。"));
        }
    }

    private List<ExpenseApprovalVO.ApprovalNode> buildApprovalRoute(ExpenseApproveDTO dto, List<ExpenseApprovalVO.RiskItem> risks) {
        List<ExpenseApprovalVO.ApprovalNode> route = new ArrayList<ExpenseApprovalVO.ApprovalNode>();
        route.add(new ExpenseApprovalVO.ApprovalNode("SUBMIT", "员工提交", defaultString(dto.getEmployeeName(), dto.getEmployeeId()), "DONE", "报销单已提交"));
        route.add(new ExpenseApprovalVO.ApprovalNode("DEPT_MANAGER", "部门经理审批", defaultString(dto.getDepartment(), "业务部门") + "负责人", "PENDING", "部门费用真实性确认"));
        if (dto.getAmount().compareTo(new BigDecimal("5000")) >= 0 || hasSeverity(risks, "YELLOW")) {
            route.add(new ExpenseApprovalVO.ApprovalNode("FINANCE_MANAGER", "财务经理审批", "财务经理", "PENDING", "金额、票据、预算和税务合规复核"));
        }
        if (dto.getAmount().compareTo(new BigDecimal("20000")) >= 0) {
            route.add(new ExpenseApprovalVO.ApprovalNode("VP", "分管领导审批", "分管领导", "PENDING", "大额报销审批"));
        }
        if (hasSeverity(risks, "RED")) {
            route.add(new ExpenseApprovalVO.ApprovalNode("COMPLIANCE", "合规专员复核", "合规专员", "BLOCKED", "存在红色风险，审批流自动拦截"));
        }
        return route;
    }

    private void summarize(ExpenseApprovalVO vo) {
        if (hasSeverity(vo.getRiskItems(), "RED")) {
            vo.setApprovalStatus("REJECTED");
            vo.setRiskLevel("HIGH");
            vo.setAdvices(Arrays.asList("存在红色风险，报销审批已自动拦截。", "优先处理发票验真、重复报销、预算不足或三单不一致问题。"));
            vo.setNextActions(Arrays.asList("退回报销人补充材料", "创建异常处理任务", "整改后重新提交审批"));
        } else if (hasSeverity(vo.getRiskItems(), "YELLOW")) {
            vo.setApprovalStatus("NEED_REVIEW");
            vo.setRiskLevel("MEDIUM");
            vo.setAdvices(Arrays.asList("存在黄色风险，需人工复核后继续审批。", "系统已自动追加财务经理审批节点。"));
            vo.setNextActions(Arrays.asList("进入财务复核队列", "补充业务说明或预算说明"));
        } else {
            vo.setApprovalStatus("APPROVED_ROUTE_READY");
            vo.setRiskLevel("LOW");
            vo.setAdvices(Arrays.asList("费用标准、发票、预算和审批路由校验通过。"));
            vo.setNextActions(Arrays.asList("进入审批流", "审批通过后自动生成付款和凭证任务"));
        }
    }

    private BigDecimal resolveStandardLimit(ExpenseApproveDTO dto) {
        String type = defaultString(dto.getExpenseType(), "");
        String level = defaultString(dto.getEmployeeLevel(), "staff");
        String city = defaultString(dto.getCityTier(), "default");
        if (contains(type, "交通")) {
            return contains(level, "executive") || contains(level, "高管") ? new BigDecimal("500") : new BigDecimal("200");
        }
        if (contains(type, "住宿")) {
            if (contains(city, "一线")) {
                return contains(level, "manager") || contains(level, "经理") ? new BigDecimal("900") : new BigDecimal("600");
            }
            return contains(level, "manager") || contains(level, "经理") ? new BigDecimal("650") : new BigDecimal("450");
        }
        if (contains(type, "餐饮") || contains(type, "招待")) {
            return contains(level, "manager") || contains(level, "经理") ? new BigDecimal("5000") : new BigDecimal("2000");
        }
        if (contains(type, "purchase") || contains(type, "采购")) {
            return new BigDecimal("100000");
        }
        return new BigDecimal("5000");
    }

    private boolean hasSeverity(List<ExpenseApprovalVO.RiskItem> risks, String severity) {
        for (ExpenseApprovalVO.RiskItem risk : risks) {
            if (severity.equals(risk.getSeverity())) {
                return true;
            }
        }
        return false;
    }

    private ExpenseApprovalVO.RiskItem risk(String riskId, String severity, String title, String description, String suggestion) {
        return new ExpenseApprovalVO.RiskItem(riskId, severity, title, description, suggestion);
    }

    private boolean amountEqual(BigDecimal left, BigDecimal right) {
        return left == null || right == null || left.compareTo(right) == 0;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
