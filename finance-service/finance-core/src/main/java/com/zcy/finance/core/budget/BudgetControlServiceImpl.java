package com.zcy.finance.core.budget;

import com.zcy.finance.api.BudgetControlService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.BudgetControlQueryDTO;
import com.zcy.finance.api.vo.BudgetControlVO;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@DubboService(interfaceClass = BudgetControlService.class)
public class BudgetControlServiceImpl implements BudgetControlService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Override
    public Result<BudgetControlVO> evaluate(BudgetControlQueryDTO dto) {
        if (dto == null || isBlank(dto.getPeriod())) {
            return Result.failure("预算评估期间不能为空");
        }

        String department = defaultText(dto.getDepartment(), "销售部");
        String expenseType = defaultText(dto.getExpenseType(), "差旅");
        String projectCode = defaultText(dto.getProjectCode(), "PRJ-2026-SALES");
        BigDecimal requestedAmount = defaultAmount(dto.getRequestedAmount(), new BigDecimal("8600"));

        BudgetProfile profile = resolveProfile(department, expenseType, dto.getScenario());
        BigDecimal occupiedAfterRequest = profile.actualUsed.add(profile.committedAmount).add(requestedAmount);
        BigDecimal availableBudget = profile.periodBudget.subtract(profile.actualUsed).subtract(profile.committedAmount).subtract(requestedAmount);
        BigDecimal executionRate = percent(occupiedAfterRequest, profile.periodBudget);
        BigDecimal forecastAmount = Boolean.FALSE.equals(dto.getIncludeForecast())
                ? occupiedAfterRequest
                : profile.actualUsed.add(profile.committedAmount).add(requestedAmount).add(profile.forecastRemaining);
        BigDecimal forecastVariance = profile.periodBudget.subtract(forecastAmount);

        BudgetControlVO vo = new BudgetControlVO();
        vo.setBudgetNo("BUD-" + dto.getPeriod().replace("-", "") + "-" + codeOf(department));
        vo.setPeriod(dto.getPeriod());
        vo.setDepartment(department);
        vo.setProjectCode(projectCode);
        vo.setExpenseType(expenseType);
        vo.setAnnualBudget(profile.annualBudget);
        vo.setPeriodBudget(profile.periodBudget);
        vo.setActualUsed(profile.actualUsed);
        vo.setCommittedAmount(profile.committedAmount);
        vo.setRequestedAmount(requestedAmount);
        vo.setAvailableBudget(availableBudget);
        vo.setExecutionRate(executionRate);
        vo.setForecastAmount(forecastAmount);
        vo.setForecastVariance(forecastVariance);
        vo.setControlItems(buildControlItems(profile, requestedAmount, availableBudget, executionRate, forecastVariance));
        vo.setWarnings(buildWarnings(availableBudget, executionRate, forecastVariance));
        vo.setNextActions(buildNextActions(availableBudget, executionRate, forecastVariance));

        if (availableBudget.compareTo(BigDecimal.ZERO) < 0 || forecastVariance.compareTo(BigDecimal.ZERO) < 0) {
            vo.setControlStatus("BLOCKED");
            vo.setRiskLevel("HIGH");
            vo.setApprovalSuggestion("预算不足或预测超支，建议拦截提交并发起预算追加/调剂流程");
        } else if (executionRate.compareTo(new BigDecimal("90")) >= 0) {
            vo.setControlStatus("REVIEW_REQUIRED");
            vo.setRiskLevel("MEDIUM");
            vo.setApprovalSuggestion("预算执行率接近红线，建议部门负责人和财务BP复核后审批");
        } else if (executionRate.compareTo(new BigDecimal("75")) >= 0) {
            vo.setControlStatus("WARNING");
            vo.setRiskLevel("LOW");
            vo.setApprovalSuggestion("预算可用但需提示预算占用情况，可进入常规审批");
        } else {
            vo.setControlStatus("PASS");
            vo.setRiskLevel("LOW");
            vo.setApprovalSuggestion("预算充足，可按标准审批流继续处理");
        }
        return Result.success(vo);
    }

    private BudgetProfile resolveProfile(String department, String expenseType, String scenario) {
        BudgetProfile profile = new BudgetProfile();
        if ("研发部".equals(department) || contains(expenseType, "研发")) {
            profile.annualBudget = new BigDecimal("3600000");
            profile.periodBudget = new BigDecimal("300000");
            profile.actualUsed = new BigDecimal("226000");
            profile.committedAmount = new BigDecimal("41800");
            profile.forecastRemaining = new BigDecimal("52000");
        } else if ("制造部".equals(department) || contains(expenseType, "采购")) {
            profile.annualBudget = new BigDecimal("5200000");
            profile.periodBudget = new BigDecimal("430000");
            profile.actualUsed = new BigDecimal("318000");
            profile.committedAmount = new BigDecimal("74000");
            profile.forecastRemaining = new BigDecimal("68000");
        } else if ("行政部".equals(department)) {
            profile.annualBudget = new BigDecimal("960000");
            profile.periodBudget = new BigDecimal("80000");
            profile.actualUsed = new BigDecimal("45200");
            profile.committedAmount = new BigDecimal("6800");
            profile.forecastRemaining = new BigDecimal("12000");
        } else {
            profile.annualBudget = new BigDecimal("1800000");
            profile.periodBudget = new BigDecimal("150000");
            profile.actualUsed = new BigDecimal("98300");
            profile.committedAmount = new BigDecimal("21200");
            profile.forecastRemaining = new BigDecimal("28000");
        }

        if (contains(scenario, "stress") || contains(scenario, "紧张") || contains(scenario, "超支")) {
            profile.actualUsed = profile.actualUsed.add(new BigDecimal("18000"));
            profile.forecastRemaining = profile.forecastRemaining.add(new BigDecimal("22000"));
        }
        return profile;
    }

    private List<BudgetControlVO.ControlItem> buildControlItems(BudgetProfile profile,
                                                                BigDecimal requestedAmount,
                                                                BigDecimal availableBudget,
                                                                BigDecimal executionRate,
                                                                BigDecimal forecastVariance) {
        List<BudgetControlVO.ControlItem> items = new ArrayList<BudgetControlVO.ControlItem>();
        items.add(new BudgetControlVO.ControlItem(
                "AVAILABLE_BUDGET",
                "可用预算校验",
                availableBudget.compareTo(BigDecimal.ZERO) >= 0 ? "PASS" : "BLOCKED",
                "本次申请后可用预算 " + availableBudget.setScale(2, RoundingMode.HALF_UP) + " 元",
                availableBudget.compareTo(BigDecimal.ZERO) >= 0 ? "允许继续占用预算" : "预算余额不足，需预算追加或调剂"
        ));
        items.add(new BudgetControlVO.ControlItem(
                "EXECUTION_RATE",
                "预算执行率校验",
                executionRate.compareTo(new BigDecimal("90")) >= 0 ? "WARNING" : "PASS",
                "本次申请后预算执行率 " + executionRate + "%",
                executionRate.compareTo(new BigDecimal("90")) >= 0 ? "进入财务BP复核" : "按常规审批流处理"
        ));
        items.add(new BudgetControlVO.ControlItem(
                "FORECAST_VARIANCE",
                "预测超支校验",
                forecastVariance.compareTo(BigDecimal.ZERO) >= 0 ? "PASS" : "BLOCKED",
                "预计期末预算差额 " + forecastVariance.setScale(2, RoundingMode.HALF_UP) + " 元",
                forecastVariance.compareTo(BigDecimal.ZERO) >= 0 ? "持续跟踪预算消耗" : "建议冻结非刚性支出并发起预算调整"
        ));
        items.add(new BudgetControlVO.ControlItem(
                "SINGLE_AMOUNT",
                "单笔金额校验",
                requestedAmount.compareTo(profile.periodBudget.multiply(new BigDecimal("0.20"))) > 0 ? "WARNING" : "PASS",
                "本次申请金额占月度预算 " + percent(requestedAmount, profile.periodBudget) + "%",
                requestedAmount.compareTo(profile.periodBudget.multiply(new BigDecimal("0.20"))) > 0 ? "建议部门负责人补充业务说明" : "单笔金额处于合理区间"
        ));
        return items;
    }

    private List<String> buildWarnings(BigDecimal availableBudget, BigDecimal executionRate, BigDecimal forecastVariance) {
        List<String> warnings = new ArrayList<String>();
        if (availableBudget.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("本次申请后预算余额为负，存在硬性超预算风险");
        }
        if (executionRate.compareTo(new BigDecimal("90")) >= 0) {
            warnings.add("预算执行率已达到或超过90%预警线");
        }
        if (forecastVariance.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("按当前趋势预测期末将超出预算");
        }
        if (warnings.isEmpty()) {
            warnings.add("未发现硬性预算阻断项");
        }
        return warnings;
    }

    private List<String> buildNextActions(BigDecimal availableBudget, BigDecimal executionRate, BigDecimal forecastVariance) {
        List<String> actions = new ArrayList<String>();
        if (availableBudget.compareTo(BigDecimal.ZERO) < 0 || forecastVariance.compareTo(BigDecimal.ZERO) < 0) {
            actions.add("发起预算追加或预算调剂申请");
            actions.add("冻结同部门非刚性费用支出");
            actions.add("通知财务BP复核预算口径和项目归集");
        } else if (executionRate.compareTo(new BigDecimal("90")) >= 0) {
            actions.add("提交部门负责人和财务BP双重复核");
            actions.add("检查本月剩余预算占用单据");
        } else {
            actions.add("写入预算占用流水");
            actions.add("继续执行标准审批流");
        }
        return actions;
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(ONE_HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultAmount(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String codeOf(String department) {
        if ("研发部".equals(department)) {
            return "RD";
        }
        if ("制造部".equals(department)) {
            return "MFG";
        }
        if ("行政部".equals(department)) {
            return "ADM";
        }
        if ("财务部".equals(department)) {
            return "FIN";
        }
        return "SAL";
    }

    private static class BudgetProfile {
        private BigDecimal annualBudget;
        private BigDecimal periodBudget;
        private BigDecimal actualUsed;
        private BigDecimal committedAmount;
        private BigDecimal forecastRemaining;
    }
}
