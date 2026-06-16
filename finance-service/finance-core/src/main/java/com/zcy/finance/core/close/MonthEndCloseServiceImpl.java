package com.zcy.finance.core.close;

import com.zcy.finance.api.MonthEndCloseService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.MonthEndCloseDTO;
import com.zcy.finance.api.vo.MonthEndCloseVO;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@DubboService(interfaceClass = MonthEndCloseService.class)
public class MonthEndCloseServiceImpl implements MonthEndCloseService {

    @Override
    public Result<MonthEndCloseVO> runCloseCheck(MonthEndCloseDTO dto) {
        if (dto == null || isBlank(dto.getPeriod())) {
            return Result.failure("结账期间不能为空");
        }

        String entityName = defaultText(dto.getEntityName(), "杭州未来制造有限公司");
        String closeType = defaultText(dto.getCloseType(), "MONTHLY");
        boolean forceClose = Boolean.TRUE.equals(dto.getForceClose());
        List<MonthEndCloseVO.CloseChecklistItem> checklist = buildChecklist(dto.getPeriod(), forceClose);

        int blockerCount = countStatus(checklist, "BLOCKED");
        int warningCount = countStatus(checklist, "WARNING");
        int completedCount = countCompleted(checklist);
        BigDecimal progressRate = new BigDecimal(completedCount)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(checklist.size()), 2, RoundingMode.HALF_UP);

        MonthEndCloseVO vo = new MonthEndCloseVO();
        vo.setCloseNo("CLOSE-" + dto.getPeriod().replace("-", "") + "-001");
        vo.setPeriod(dto.getPeriod());
        vo.setEntityName(entityName);
        vo.setCloseType(closeType);
        vo.setTotalTaskCount(checklist.size());
        vo.setCompletedTaskCount(completedCount);
        vo.setBlockerCount(blockerCount);
        vo.setWarningCount(warningCount);
        vo.setProgressRate(progressRate);
        vo.setEstimatedCloseDate(forceClose ? "2026-06-02" : "2026-06-03");
        vo.setChecklist(Boolean.FALSE.equals(dto.getIncludeChecklist()) ? new ArrayList<MonthEndCloseVO.CloseChecklistItem>() : checklist);
        vo.setBlockers(buildBlockers(checklist));
        vo.setWarnings(buildWarnings(checklist));
        vo.setNextActions(buildNextActions(blockerCount, warningCount, forceClose));

        if (forceClose && blockerCount > 0) {
            vo.setCloseStatus("FORCED_CLOSE_REVIEW");
            vo.setReadyToClose(false);
        } else if (blockerCount > 0) {
            vo.setCloseStatus("BLOCKED");
            vo.setReadyToClose(false);
        } else if (warningCount > 0) {
            vo.setCloseStatus("READY_WITH_WARNINGS");
            vo.setReadyToClose(true);
        } else {
            vo.setCloseStatus("READY_TO_CLOSE");
            vo.setReadyToClose(true);
        }
        return Result.success(vo);
    }

    private List<MonthEndCloseVO.CloseChecklistItem> buildChecklist(String period, boolean forceClose) {
        List<MonthEndCloseVO.CloseChecklistItem> items = new ArrayList<MonthEndCloseVO.CloseChecklistItem>();
        String due = nextMonthFirstDay(period);
        items.add(item("发票", "INV_VERIFY", "发票验真与查重完成", "WARNING", "税务会计", due,
                "INV-202605-003 仍为待复查，重复票索引 1 条", "触发补验任务并确认是否影响本期抵扣"));
        items.add(item("凭证", "VOUCHER_AUDIT", "本期凭证审核通过", forceClose ? "WARNING" : "PASS", "总账会计", due,
                forceClose ? "存在强制结账复核凭证 1 张" : "本期凭证借贷平衡，红色异常 0 项", forceClose ? "强制结账后补充复核说明" : "允许进入报表汇总"));
        items.add(item("银行", "BANK_RECON", "银行对账和余额调节表完成", "BLOCKED", "出纳", due,
                "6222-0001 未匹配流水 2 条，差异 8,600 元", "完成流水认领或生成未达账说明"));
        items.add(item("应收应付", "AR_AP_CONFIRM", "往来余额和账龄确认", "WARNING", "应收会计", due,
                "90天以上应收余额偏高，C类客户催收计划已生成", "同步催收计划并确认坏账准备口径"));
        items.add(item("费用报销", "EXPENSE_APPROVAL", "报销单据全部审批入账", "PASS", "费用会计", due,
                "待审批低风险单据 0 张，高风险单据已拦截", "继续监控跨期报销"));
        items.add(item("税务", "TAX_RETURN", "税金计提和申报草稿生成", "PASS", "税务会计", due,
                "增值税申报表草稿 TAX-202605-VAT 已生成", "等待发票补验后锁定进项税额"));
        items.add(item("报表", "REPORT_GENERATE", "三大报表生成并校验勾稽", "BLOCKED", "报表会计", due,
                "银行对账阻断导致现金流量表无法最终锁定", "银行差异处理后重新生成报表"));
        items.add(item("合规", "COMPLIANCE_REVIEW", "合规风控扫描完成", "PASS", "内控专员", due,
                "本期综合风险 MEDIUM，高风险 0 项", "将预警项纳入月结工作底稿"));
        return items;
    }

    private MonthEndCloseVO.CloseChecklistItem item(String module, String code, String name, String status,
                                                     String owner, String dueDate, String evidence, String suggestion) {
        return new MonthEndCloseVO.CloseChecklistItem(module, code, name, status, owner, dueDate, evidence, suggestion);
    }

    private int countStatus(List<MonthEndCloseVO.CloseChecklistItem> items, String status) {
        int count = 0;
        for (MonthEndCloseVO.CloseChecklistItem item : items) {
            if (status.equals(item.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countCompleted(List<MonthEndCloseVO.CloseChecklistItem> items) {
        int count = 0;
        for (MonthEndCloseVO.CloseChecklistItem item : items) {
            if ("PASS".equals(item.getStatus()) || "WARNING".equals(item.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private List<String> buildBlockers(List<MonthEndCloseVO.CloseChecklistItem> checklist) {
        List<String> blockers = new ArrayList<String>();
        for (MonthEndCloseVO.CloseChecklistItem item : checklist) {
            if ("BLOCKED".equals(item.getStatus())) {
                blockers.add(item.getModule() + "：" + item.getEvidence() + "；建议：" + item.getSuggestion());
            }
        }
        if (blockers.isEmpty()) {
            blockers.add("无硬性关账阻断项");
        }
        return blockers;
    }

    private List<String> buildWarnings(List<MonthEndCloseVO.CloseChecklistItem> checklist) {
        List<String> warnings = new ArrayList<String>();
        for (MonthEndCloseVO.CloseChecklistItem item : checklist) {
            if ("WARNING".equals(item.getStatus())) {
                warnings.add(item.getModule() + "：" + item.getEvidence());
            }
        }
        if (warnings.isEmpty()) {
            warnings.add("无月结预警项");
        }
        return warnings;
    }

    private List<String> buildNextActions(int blockerCount, int warningCount, boolean forceClose) {
        List<String> actions = new ArrayList<String>();
        if (forceClose) {
            actions.add("生成强制关账复核底稿并记录审批人");
            actions.add("对阻断项建立结账后补处理任务");
            return actions;
        }
        if (blockerCount > 0) {
            actions.add("优先处理银行未匹配流水和现金流量表锁定问题");
            actions.add("重新运行月结检查并归档关账清单");
        }
        if (warningCount > 0) {
            actions.add("发票补验和往来催收结果进入月结工作底稿");
        }
        if (actions.isEmpty()) {
            actions.add("锁定本期凭证、生成最终报表并发起关账审批");
        }
        return actions;
    }

    private String nextMonthFirstDay(String period) {
        String[] parts = period.split("-");
        if (parts.length != 2) {
            return "2026-06-01";
        }
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        month++;
        if (month > 12) {
            year++;
            month = 1;
        }
        return year + "-" + (month < 10 ? "0" + month : String.valueOf(month)) + "-01";
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
