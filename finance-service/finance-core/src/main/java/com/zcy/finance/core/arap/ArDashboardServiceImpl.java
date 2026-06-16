package com.zcy.finance.core.arap;

import com.zcy.finance.api.ArDashboardService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.CollectionPlanDTO;
import com.zcy.finance.api.dto.CounterpartyReconcileDTO;
import com.zcy.finance.api.dto.PaymentPlanOptimizeDTO;
import com.zcy.finance.api.vo.ArDashboardVO;
import com.zcy.finance.api.vo.CollectionPlanVO;
import com.zcy.finance.api.vo.CounterpartyReconcileVO;
import com.zcy.finance.api.vo.PaymentPlanVO;
import com.zcy.finance.infra.arap.ArApRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.Arrays;

@DubboService(interfaceClass = ArDashboardService.class)
public class ArDashboardServiceImpl implements ArDashboardService {

    private final ArApRepository repository;

    public ArDashboardServiceImpl(ArApRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<ArDashboardVO> queryOverview(String month) {
        if (month == null || month.trim().length() == 0) {
            return Result.failure("月份不能为空");
        }
        return Result.success(repository.dashboard(month));
    }

    @Override
    public Result<CollectionPlanVO> generateCollectionPlan(CollectionPlanDTO dto) {
        if (dto == null) {
            return Result.failure("催收计划参数不能为空");
        }
        CollectionPlanVO vo = new CollectionPlanVO();
        vo.setPlanNo("COL-" + defaultString(dto.getPeriod(), "2026-05").replace("-", "") + "-001");
        vo.setPeriod(defaultString(dto.getPeriod(), "2026-05"));
        vo.setCustomerName(defaultString(dto.getCustomerName(), "杭州示例客户有限公司"));
        vo.setCreditLevel(defaultString(dto.getCreditLevel(), "B"));
        vo.setOverdueAmount(zeroIfNull(dto.getOverdueAmount()));
        vo.setOverdueDays(dto.getOverdueDays());

        String owner = defaultString(dto.getOwner(), "应收会计");
        if ("C".equalsIgnoreCase(vo.getCreditLevel()) || dto.getOverdueDays() >= 30) {
            vo.setCollectionStrategy("强催收");
            vo.setEscalationLevel("SALES_DIRECTOR_AND_FINANCE_MANAGER");
            vo.setFreezeCreditSuggestion("建议冻结后续授信额度，回款前暂停新增订单放行。");
            vo.getActionItems().add(new CollectionPlanVO.ActionItem("2026-06-03", "电话催收", owner, "当天电话确认付款计划并记录承诺付款日。", "PENDING"));
            vo.getActionItems().add(new CollectionPlanVO.ActionItem("2026-06-04", "升级销售负责人", "销售负责人", "同步客户逾期明细，要求销售协助回款。", "PENDING"));
            vo.getActionItems().add(new CollectionPlanVO.ActionItem("2026-06-06", "法务预警", "法务", "若仍未确认付款计划，准备律师函模板。", "WAITING"));
        } else if ("B".equalsIgnoreCase(vo.getCreditLevel()) || dto.getOverdueDays() >= 7) {
            vo.setCollectionStrategy("标准催收");
            vo.setEscalationLevel("SALES_OWNER");
            vo.setFreezeCreditSuggestion("暂不冻结授信，逾期超过15天自动升级。");
            vo.getActionItems().add(new CollectionPlanVO.ActionItem("2026-06-03", "邮件提醒", owner, "发送对账单和付款提醒邮件。", "PENDING"));
            vo.getActionItems().add(new CollectionPlanVO.ActionItem("2026-06-05", "电话跟进", owner, "确认付款时间并更新催收台账。", "PENDING"));
            vo.getActionItems().add(new CollectionPlanVO.ActionItem("2026-06-10", "升级销售", "销售负责人", "逾期仍未回款时转销售协同。", "WAITING"));
        } else {
            vo.setCollectionStrategy("温和提醒");
            vo.setEscalationLevel("AR_OWNER");
            vo.setFreezeCreditSuggestion("不冻结授信，仅发送到期提醒。");
            vo.getActionItems().add(new CollectionPlanVO.ActionItem("2026-06-03", "到期提醒", owner, "发送温和付款提醒。", "PENDING"));
            vo.getActionItems().add(new CollectionPlanVO.ActionItem("2026-06-10", "自动复查", "系统", "检查是否到账，未到账再升级。", "WAITING"));
        }
        if (dto.isIncludeLetter()) {
            vo.setCollectionLetterDraft("尊敬的" + vo.getCustomerName() + "：贵司当前逾期金额为" + vo.getOverdueAmount() + "元，逾期" + vo.getOverdueDays() + "天。请于约定日期前完成付款或反馈差异。");
        }
        vo.setAdvices(Arrays.asList("催收动作已按信用等级和逾期天数生成。", "每次跟进结果需写入催收台账，便于审计追溯。"));
        return Result.success(vo);
    }

    @Override
    public Result<PaymentPlanVO> optimizePaymentPlan(PaymentPlanOptimizeDTO dto) {
        if (dto == null || dto.getPayableAmount() == null) {
            return Result.failure("应付金额不能为空");
        }
        PaymentPlanVO vo = new PaymentPlanVO();
        vo.setPlanNo("PAY-" + defaultString(dto.getPeriod(), "2026-06").replace("-", "") + "-001");
        vo.setPeriod(defaultString(dto.getPeriod(), "2026-06"));
        vo.setSupplierName(defaultString(dto.getSupplierName(), "示例供应商"));
        vo.setPayableAmount(dto.getPayableAmount());
        vo.setDiscountBenefit(resolveDiscountBenefit(dto));
        vo.setCashAfterPayment(zeroIfNull(dto.getCashBalance()).subtract(dto.getPayableAmount()));
        vo.setMergePaymentRecommended(dto.isAllowMergePayment() && dto.getPayableAmount().compareTo(new BigDecimal("30000")) >= 0);

        BigDecimal safety = zeroIfNull(dto.getSafetyCashLevel());
        if (vo.getCashAfterPayment().compareTo(safety) < 0) {
            vo.setPaymentStrategy("延期至账期末付款");
            vo.setRecommendedPayDate(defaultString(dto.getDueDate(), "2026-06-30"));
            vo.setLiquidityImpact("HIGH");
            vo.getRiskHints().add("付款后现金余额低于安全水位，建议延后或拆分付款。");
            vo.getNextActions().add("提交付款延期审批");
        } else if (vo.getDiscountBenefit().compareTo(new BigDecimal("1000")) >= 0) {
            vo.setPaymentStrategy("提前付款获取折扣");
            vo.setRecommendedPayDate("2026-06-10");
            vo.setLiquidityImpact("LOW");
            vo.getNextActions().add("发起提前付款申请并锁定折扣收益。");
        } else {
            vo.setPaymentStrategy("账期末付款");
            vo.setRecommendedPayDate(defaultString(dto.getDueDate(), "2026-06-30"));
            vo.setLiquidityImpact("MEDIUM");
            vo.getNextActions().add("按账期末付款，最大化资金使用效率。");
        }
        if (vo.isMergePaymentRecommended()) {
            vo.getScheduleItems().add("建议合并同供应商多笔应付，减少付款频次和手续费。");
        }
        vo.getScheduleItems().add("付款前校验发票验真、凭证审核和审批流状态。");
        vo.getScheduleItems().add("付款完成后自动回写银行流水和应付核销状态。");
        if (vo.getRiskHints().isEmpty()) {
            vo.getRiskHints().add("未发现重大流动性风险。");
        }
        return Result.success(vo);
    }

    @Override
    public Result<CounterpartyReconcileVO> reconcileCounterparty(CounterpartyReconcileDTO dto) {
        if (dto == null || dto.getPeriod() == null || dto.getPeriod().trim().length() == 0) {
            return Result.failure("对账期间不能为空");
        }
        CounterpartyReconcileVO vo = new CounterpartyReconcileVO();
        vo.setReconcileNo("ARAP-REC-" + dto.getPeriod().replace("-", "") + "-001");
        vo.setPeriod(dto.getPeriod());
        vo.setCounterpartyName(defaultString(dto.getCounterpartyName(), "浙江智造设备有限公司"));
        vo.setCounterpartyType(defaultString(dto.getCounterpartyType(), "CUSTOMER"));
        vo.setInternalBalance(defaultBalance(dto.getInternalBalance(), vo.getCounterpartyType(), true));
        vo.setCounterpartyBalance(defaultBalance(dto.getCounterpartyBalance(), vo.getCounterpartyType(), false));
        vo.setDifferenceAmount(vo.getInternalBalance().subtract(vo.getCounterpartyBalance()).abs());

        if (vo.getDifferenceAmount().compareTo(BigDecimal.ZERO) == 0) {
            vo.setReconcileStatus("MATCHED");
            vo.setRiskLevel("LOW");
            vo.setConfirmationStatus("CONFIRMED");
            vo.getAdvices().add("双方余额一致，可归档本期往来对账单。");
            vo.getNextActions().add("锁定本期往来余额并同步月结工作底稿。");
        } else if (vo.getDifferenceAmount().compareTo(new BigDecimal("10000")) >= 0) {
            vo.setReconcileStatus("DIFFERENCE_BLOCKED");
            vo.setRiskLevel("HIGH");
            vo.setConfirmationStatus("PENDING_COUNTERPARTY_CONFIRM");
            vo.getDifferenceItems().add(new CounterpartyReconcileVO.DifferenceItem("DIF-001", "未达收付款", new BigDecimal("8600.00"), "我方已确认收款/付款，对方尚未入账。", "出纳", "核对银行流水和回单，必要时补发付款/收款证明。"));
            vo.getDifferenceItems().add(new CounterpartyReconcileVO.DifferenceItem("DIF-002", "发票或折扣差异", vo.getDifferenceAmount().subtract(new BigDecimal("8600.00")).max(BigDecimal.ZERO), "对方账面可能未扣除折扣或未收到发票。", "应收会计", "补发发票清单、折扣协议和对账明细。"));
            vo.getAdvices().add("差异金额超过阈值，暂不建议关账确认。");
            vo.getNextActions().add("生成余额确认函并发送对方财务确认。");
            vo.getNextActions().add("差异处理完成后重新运行往来对账。");
        } else {
            vo.setReconcileStatus("MINOR_DIFFERENCE");
            vo.setRiskLevel("MEDIUM");
            vo.setConfirmationStatus("WAITING_INTERNAL_REVIEW");
            vo.getDifferenceItems().add(new CounterpartyReconcileVO.DifferenceItem("DIF-001", "尾差/时间差", vo.getDifferenceAmount(), "存在小额尾差或跨期入账时间差。", "应收会计", "由财务复核后确认是否调整或列入下期跟踪。"));
            vo.getAdvices().add("差异金额较小，可在复核说明后进入月结底稿。");
            vo.getNextActions().add("补充差异说明并等待主管复核。");
        }
        if (Boolean.TRUE.equals(dto.getIncludeConfirmationLetter())) {
            vo.setConfirmationLetterDraft("尊敬的" + vo.getCounterpartyName() + "：请确认截至" + vo.getPeriod() + "我方账面往来余额为" + vo.getInternalBalance() + "元，贵方回函余额为" + vo.getCounterpartyBalance() + "元，差异为" + vo.getDifferenceAmount() + "元。请于3个工作日内反馈差异原因或盖章确认。");
        }
        return Result.success(vo);
    }

    private BigDecimal resolveDiscountBenefit(PaymentPlanOptimizeDTO dto) {
        String term = dto.getDiscountTerm();
        if (term == null || term.trim().length() == 0 || dto.getPayableAmount() == null) {
            return BigDecimal.ZERO;
        }
        if (term.contains("2/10")) {
            return dto.getPayableAmount().multiply(new BigDecimal("0.02"));
        }
        if (term.contains("1/10")) {
            return dto.getPayableAmount().multiply(new BigDecimal("0.01"));
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal defaultBalance(BigDecimal value, String counterpartyType, boolean internal) {
        if (value != null) {
            return value;
        }
        if ("SUPPLIER".equalsIgnoreCase(counterpartyType)) {
            return internal ? new BigDecimal("126000.00") : new BigDecimal("117400.00");
        }
        return internal ? new BigDecimal("218900.00") : new BigDecimal("204300.00");
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }
}
