package com.zcy.finance.core.analysis;

import com.zcy.finance.api.AnalysisService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.AnalysisQueryDTO;
import com.zcy.finance.api.dto.VarianceDiagnosisDTO;
import com.zcy.finance.api.vo.AnalysisVO;
import com.zcy.finance.api.vo.AnomalyVO;
import com.zcy.finance.api.vo.FinancialRatioVO;
import com.zcy.finance.api.vo.TrendAnalysisVO;
import com.zcy.finance.api.vo.VarianceDiagnosisVO;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = AnalysisService.class)
public class AnalysisServiceImpl implements AnalysisService {

    @Override
    public Result<AnalysisVO> analyze(AnalysisQueryDTO dto) {
        if (dto == null || dto.getPeriod() == null) {
            return Result.failure("分析期间不能为空");
        }

        AnalysisVO vo = new AnalysisVO();
        vo.setPeriod(dto.getPeriod());
        vo.setMetrics(Arrays.asList(
                new AnalysisVO.Metric("流动比率", new BigDecimal("1.85"), "倍", "健康"),
                new AnalysisVO.Metric("回款率", new BigDecimal("65.78"), "%", "需关注"),
                new AnalysisVO.Metric("费用率", new BigDecimal("18.20"), "%", "稳定")
        ));
        vo.setAnomalies(Arrays.asList("90天以上应收余额偏高", "销售费用本期环比上升12%"));
        vo.setAdvices(Arrays.asList("对逾期客户生成催收计划", "复核销售费用中的招待和差旅明细"));
        return Result.success(vo);
    }

    @Override
    public Result<List<FinancialRatioVO>> calculateRatios(String period) {
        if (isBlank(period)) {
            return Result.failure("分析期间不能为空");
        }
        return Result.success(Arrays.asList(
                ratio(period, "流动比率", "偿债能力", "1.85", "倍", "流动资产 / 流动负债", "1.5 - 2.5", "短期偿债能力健康", "HEALTHY", "流动资产能够覆盖短期负债。"),
                ratio(period, "速动比率", "偿债能力", "1.42", "倍", "速动资产 / 流动负债", "1.0 - 1.8", "短期流动性健康", "HEALTHY", "剔除存货后仍有充足偿债能力。"),
                ratio(period, "资产负债率", "资本结构", "23.17", "%", "负债总额 / 资产总额", "30% - 60%", "杠杆水平较低", "LOW_RISK", "债务压力较低，但可评估适度使用低成本融资。"),
                ratio(period, "净利率", "盈利能力", "14.63", "%", "净利润 / 营业收入", "10% - 20%", "盈利能力健康", "HEALTHY", "收入增长能够转化为稳定利润。"),
                ratio(period, "应收账款周转率", "营运能力", "4.28", "次", "营业收入 / 平均应收账款", "5 次以上", "周转速度需关注", "ATTENTION", "90 天以上应收余额拉低了周转效率。")
        ));
    }

    @Override
    public Result<TrendAnalysisVO> analyzeTrend(String startDate, String endDate, String metric) {
        if (isBlank(startDate) || isBlank(endDate) || isBlank(metric)) {
            return Result.failure("起止期间和分析指标不能为空");
        }

        YearMonth start;
        YearMonth end;
        try {
            start = YearMonth.parse(startDate);
            end = YearMonth.parse(endDate);
        } catch (DateTimeParseException ex) {
            return Result.failure("起止期间格式必须为 yyyy-MM");
        }
        if (start.isAfter(end)) {
            return Result.failure("开始期间不能晚于结束期间");
        }

        List<TrendAnalysisVO.DataPoint> points = new ArrayList<TrendAnalysisVO.DataPoint>();
        BigDecimal previous = null;
        BigDecimal base = trendBase(metric);
        BigDecimal increment = trendIncrement(metric);
        YearMonth cursor = start;
        int index = 0;
        while (!cursor.isAfter(end) && index < 24) {
            BigDecimal value = base.add(increment.multiply(BigDecimal.valueOf(index)));
            BigDecimal changeRate = previous == null ? BigDecimal.ZERO : percent(value.subtract(previous), previous);
            points.add(new TrendAnalysisVO.DataPoint(cursor.toString(), value, changeRate));
            previous = value;
            cursor = cursor.plusMonths(1);
            index++;
        }

        BigDecimal firstValue = points.get(0).getValue();
        BigDecimal lastValue = points.get(points.size() - 1).getValue();
        BigDecimal totalChangeRate = percent(lastValue.subtract(firstValue), firstValue);
        String direction = totalChangeRate.compareTo(BigDecimal.ZERO) > 0 ? "UP"
                : totalChangeRate.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "STABLE";

        TrendAnalysisVO vo = new TrendAnalysisVO();
        vo.setMetric(metric);
        vo.setStartPeriod(startDate);
        vo.setEndPeriod(points.get(points.size() - 1).getPeriod());
        vo.setUnit(trendUnit(metric));
        vo.setTrendDirection(direction);
        vo.setChangeRate(totalChangeRate);
        vo.setDescription(metric + "在分析区间内整体" + ("UP".equals(direction) ? "上升" : "DOWN".equals(direction) ? "下降" : "稳定") + "。");
        vo.setDataPoints(points);
        vo.setInsights(Arrays.asList(
                metric + "累计变动 " + totalChangeRate + "%。",
                "最近一个月环比变动 " + points.get(points.size() - 1).getChangeRate() + "%。",
                trendInsight(metric)
        ));
        return Result.success(vo);
    }

    @Override
    public Result<List<AnomalyVO>> detectAnomalies(String period) {
        if (isBlank(period)) {
            return Result.failure("检测期间不能为空");
        }
        return Result.success(Arrays.asList(
                anomaly(period, "001", "AR_AGING", "90天以上应收余额", "HIGH", "218900.00", "150000.00", "45.93",
                        "90天以上应收余额高于风险阈值。", "对高龄应收生成分级催收计划，并升级重点客户跟进。",
                        "客户A逾期35天且信用等级为C", "本期高龄应收占应收余额比例上升"),
                anomaly(period, "002", "EXPENSE_SURGE", "销售费用", "MEDIUM", "286000.00", "255000.00", "12.16",
                        "销售费用环比增长超过10%。", "复核差旅、市场活动和招待费明细，并评估预算调整。",
                        "销售差旅类报销环比增加18%", "市场活动合同提前验收"),
                anomaly(period, "003", "DUPLICATE_INVOICE", "重复发票数量", "HIGH", "2", "0", "100",
                        "发现疑似重复发票，存在重复报销风险。", "暂停相关报销单，执行发票查重和人工复核。",
                        "发票号码与历史台账重复", "电子发票文件哈希一致")
        ));
    }

    @Override
    public Result<VarianceDiagnosisVO> diagnoseVariance(VarianceDiagnosisDTO dto) {
        if (dto == null || dto.getPeriod() == null) {
            return Result.failure("诊断期间不能为空");
        }

        BigDecimal actual = defaultAmount(dto.getActualAmount(), new BigDecimal("286000.00"));
        BigDecimal budget = defaultAmount(dto.getBudgetAmount(), new BigDecimal("240000.00"));
        BigDecimal previous = defaultAmount(dto.getPreviousAmount(), new BigDecimal("255000.00"));
        BigDecimal budgetVariance = actual.subtract(budget);
        BigDecimal momVariance = actual.subtract(previous);

        VarianceDiagnosisVO vo = new VarianceDiagnosisVO();
        vo.setDiagnosisNo("VAR-" + dto.getPeriod().replace("-", "") + "-001");
        vo.setPeriod(dto.getPeriod());
        vo.setMetricName(defaultText(dto.getMetricName(), "销售费用"));
        vo.setDepartment(defaultText(dto.getDepartment(), "销售部"));
        vo.setActualAmount(actual);
        vo.setBudgetAmount(budget);
        vo.setPreviousAmount(previous);
        vo.setBudgetVariance(budgetVariance);
        vo.setBudgetVarianceRate(percent(budgetVariance, budget));
        vo.setMomVariance(momVariance);
        vo.setMomVarianceRate(percent(momVariance, previous));
        if (vo.getBudgetVarianceRate().compareTo(new BigDecimal("15")) >= 0) {
            vo.setSeverity("HIGH");
        } else if (vo.getBudgetVarianceRate().compareTo(new BigDecimal("5")) >= 0) {
            vo.setSeverity("MEDIUM");
        } else {
            vo.setSeverity("LOW");
        }
        vo.setConclusion(vo.getMetricName() + "较预算超支 " + budgetVariance + " 元，主要由差旅、市场活动和招待费共同驱动。");
        vo.setDrivers(Arrays.asList(
                new VarianceDiagnosisVO.DriverItem("华东客户拜访差旅增加", new BigDecimal("18600.00"), new BigDecimal("40.43"), "销售一部", "本月新增重点客户现场拜访 12 次，机票和住宿费用集中入账。"),
                new VarianceDiagnosisVO.DriverItem("渠道市场活动提前执行", new BigDecimal("15400.00"), new BigDecimal("33.48"), "市场部", "原计划 6 月执行的渠道活动提前到 5 月，导致预算跨期。"),
                new VarianceDiagnosisVO.DriverItem("客户招待费超标准", new BigDecimal("8200.00"), new BigDecimal("17.83"), "销售二部", "3 笔招待费接近或超过单次标准，需复核审批依据。"),
                new VarianceDiagnosisVO.DriverItem("零星费用尾差", new BigDecimal("3800.00"), new BigDecimal("8.26"), "销售支持", "办公、快递等零星费用合计影响较小。")
        ));
        vo.setEvidence(Arrays.asList(
                "预算系统显示销售部本月预算 240000.00 元，实际入账 286000.00 元。",
                "报销审批记录中差旅类单据环比增加 18%，其中 5 笔为大额差旅。",
                "市场活动合同 CT-202605-002 在 5 月提前验收，导致费用提前确认。",
                "凭证审核未发现借贷不平，但提示 3 笔招待费需复核费用标准。"
        ));
        if (!Boolean.FALSE.equals(dto.getIncludeActionPlan())) {
            vo.setActionPlan(Arrays.asList(
                    "将提前执行的市场活动费用在管理分析中单独列示，避免误判持续性超支。",
                    "对销售一部差旅设置剩余预算预警线，超过 90% 时触发财务BP复核。",
                    "抽查客户招待费附件和审批依据，超标准部分进入费用整改清单。",
                    "下月预算滚动预测调增重点客户拜访费用，同时压降非刚性招待支出。"
            ));
        }
        vo.setFollowUpQuestions(Arrays.asList(
                "查看差旅费明细和员工排行",
                "对比销售费用和收入增长是否匹配",
                "生成销售部下月费用预算预警"
        ));
        return Result.success(vo);
    }

    private FinancialRatioVO ratio(String period, String name, String category, String value, String unit,
                                   String formula, String referenceRange, String evaluation, String healthLevel,
                                   String description) {
        return new FinancialRatioVO(period, name, category, new BigDecimal(value), unit, formula, referenceRange,
                evaluation, healthLevel, description);
    }

    private AnomalyVO anomaly(String period, String suffix, String anomalyType, String metric, String severity,
                              String actualValue, String expectedValue, String deviationRate, String description,
                              String suggestion, String... evidence) {
        AnomalyVO vo = new AnomalyVO();
        vo.setAnomalyNo("ANO-" + period.replace("-", "") + "-" + suffix);
        vo.setPeriod(period);
        vo.setAnomalyType(anomalyType);
        vo.setMetric(metric);
        vo.setSeverity(severity);
        vo.setActualValue(new BigDecimal(actualValue));
        vo.setExpectedValue(new BigDecimal(expectedValue));
        vo.setDeviationRate(new BigDecimal(deviationRate));
        vo.setDescription(description);
        vo.setSuggestion(suggestion);
        vo.setEvidence(Arrays.asList(evidence));
        return vo;
    }

    private BigDecimal trendBase(String metric) {
        if (metric.contains("销售费用") || metric.contains("费用")) {
            return new BigDecimal("218000.00");
        }
        if (metric.contains("回款率") || metric.contains("率")) {
            return new BigDecimal("58.20");
        }
        return new BigDecimal("1980000.00");
    }

    private BigDecimal trendIncrement(String metric) {
        if (metric.contains("销售费用") || metric.contains("费用")) {
            return new BigDecimal("17000.00");
        }
        if (metric.contains("回款率") || metric.contains("率")) {
            return new BigDecimal("1.90");
        }
        return new BigDecimal("117500.00");
    }

    private String trendUnit(String metric) {
        return metric.contains("率") ? "%" : "元";
    }

    private String trendInsight(String metric) {
        if (metric.contains("费用")) {
            return "费用增速需要与收入增速联合判断，建议继续执行差异归因。";
        }
        if (metric.contains("回款")) {
            return "回款效率持续改善，但仍需关注高龄应收。";
        }
        return "指标保持增长，建议结合预算和现金流评估增长质量。";
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(new BigDecimal("100")).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultAmount(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
