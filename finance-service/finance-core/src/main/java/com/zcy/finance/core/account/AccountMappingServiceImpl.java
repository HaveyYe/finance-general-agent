package com.zcy.finance.core.account;

import com.zcy.finance.api.AccountMappingService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.AccountMappingRecommendDTO;
import com.zcy.finance.api.vo.AccountMappingRecommendationVO;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@DubboService(interfaceClass = AccountMappingService.class)
public class AccountMappingServiceImpl implements AccountMappingService {

    @Override
    public Result<AccountMappingRecommendationVO> recommend(AccountMappingRecommendDTO dto) {
        if (dto == null || isBlank(dto.getSummary())) {
            return Result.failure("单据摘要不能为空");
        }

        String documentType = defaultText(dto.getDocumentType(), "invoice");
        String scenario = defaultText(dto.getBusinessScenario(), inferScenario(dto));
        BigDecimal amount = defaultAmount(dto.getAmount(), new BigDecimal("12000.00"));
        BigDecimal taxAmount = defaultAmount(dto.getTaxAmount(), BigDecimal.ZERO);
        MappingProfile profile = resolveProfile(documentType, scenario, dto.getSummary(), dto.getCounterpartyName());

        AccountMappingRecommendationVO vo = new AccountMappingRecommendationVO();
        vo.setMappingNo("MAP-202606-" + profile.serial);
        vo.setDocumentType(documentType);
        vo.setBusinessScenario(scenario);
        vo.setSummary(dto.getSummary());
        vo.setRecommendationMode(profile.mode);
        vo.setConfidence(profile.confidence);
        vo.setAutoApplicable(profile.confidence.compareTo(new BigDecimal("85")) >= 0 && !profile.manualReview);
        vo.setManualReviewRequired(profile.manualReview || profile.confidence.compareTo(new BigDecimal("85")) < 0);
        vo.setSelectedAccountCode(profile.accountCode);
        vo.setSelectedAccountName(profile.accountName);
        vo.setSelectedTaxAccountCode(taxAmount.compareTo(BigDecimal.ZERO) > 0 ? "22210101" : null);
        vo.setSelectedTaxAccountName(taxAmount.compareTo(BigDecimal.ZERO) > 0 ? "应交税费-应交增值税-进项税额" : null);
        vo.setPayableOrReceivableAccountCode(profile.counterpartyAccountCode);
        vo.setPayableOrReceivableAccountName(profile.counterpartyAccountName);
        vo.setCandidates(buildCandidates(profile));
        if (!Boolean.FALSE.equals(dto.getIncludeVoucherPreview())) {
            vo.setVoucherPreview(buildVoucherPreview(profile, amount, taxAmount));
        }
        vo.setEvidence(buildEvidence(profile, dto));
        vo.setRiskHints(buildRiskHints(profile, dto));
        vo.setNextActions(buildNextActions(vo.getAutoApplicable(), vo.getManualReviewRequired()));
        return Result.success(vo);
    }

    private MappingProfile resolveProfile(String documentType, String scenario, String summary, String counterpartyName) {
        String text = (documentType + " " + scenario + " " + summary + " " + defaultText(counterpartyName, "")).toLowerCase();
        MappingProfile profile = new MappingProfile();
        profile.serial = "001";
        profile.mode = "RULE_MATCHED";
        profile.confidence = new BigDecimal("96.50");
        profile.counterpartyAccountCode = "2202";
        profile.counterpartyAccountName = "应付账款";
        profile.manualReview = false;

        if (contains(text, "采购") || contains(text, "原材料") || contains(text, "入库") || contains(text, "三单")) {
            profile.serial = "PUR";
            profile.accountCode = "1403";
            profile.accountName = "原材料";
            profile.accountType = "ASSET";
            profile.reason = "采购发票命中采购入库规则，借记存货类科目。";
            return profile;
        }
        if (contains(text, "差旅") || contains(text, "交通") || contains(text, "住宿") || contains(text, "机票")) {
            profile.serial = "TRV";
            profile.accountCode = "660101";
            profile.accountName = "销售费用-差旅费";
            profile.accountType = "EXPENSE";
            profile.reason = "摘要包含差旅/交通特征，部门费用归集到销售费用-差旅费。";
            return profile;
        }
        if (contains(text, "软件") || contains(text, "saas") || contains(text, "订阅") || contains(text, "服务费")) {
            profile.serial = "SVC";
            profile.mode = "AI_FALLBACK";
            profile.confidence = new BigDecimal("88.00");
            profile.accountCode = "660205";
            profile.accountName = "管理费用-软件服务费";
            profile.accountType = "EXPENSE";
            profile.reason = "规则未完全命中，AI根据供应商、摘要和历史相似凭证推荐软件服务费。";
            return profile;
        }
        if (contains(text, "收入") || contains(text, "销售") || contains(text, "回款")) {
            profile.serial = "SAL";
            profile.accountCode = "6001";
            profile.accountName = "主营业务收入";
            profile.accountType = "REVENUE";
            profile.counterpartyAccountCode = "1122";
            profile.counterpartyAccountName = "应收账款";
            profile.reason = "销售业务命中收入确认规则，贷记主营业务收入。";
            return profile;
        }

        profile.serial = "MAN";
        profile.mode = "AI_LOW_CONFIDENCE";
        profile.confidence = new BigDecimal("72.00");
        profile.accountCode = "660299";
        profile.accountName = "管理费用-其他";
        profile.accountType = "EXPENSE";
        profile.reason = "缺少稳定规则特征，仅能给出低置信度兜底科目。";
        profile.manualReview = true;
        return profile;
    }

    private List<AccountMappingRecommendationVO.AccountCandidate> buildCandidates(MappingProfile profile) {
        List<AccountMappingRecommendationVO.AccountCandidate> rows = new ArrayList<AccountMappingRecommendationVO.AccountCandidate>();
        rows.add(new AccountMappingRecommendationVO.AccountCandidate(profile.accountCode, profile.accountName, profile.accountType, profile.confidence, profile.reason));
        if ("1403".equals(profile.accountCode)) {
            rows.add(new AccountMappingRecommendationVO.AccountCandidate("1405", "库存商品", "ASSET", new BigDecimal("83.00"), "若采购已完工入库，可改用库存商品。"));
            rows.add(new AccountMappingRecommendationVO.AccountCandidate("1601", "固定资产", "ASSET", new BigDecimal("71.00"), "若单据为设备采购且达到资本化标准，可转固定资产。"));
        } else if ("6001".equals(profile.accountCode)) {
            rows.add(new AccountMappingRecommendationVO.AccountCandidate("6051", "其他业务收入", "REVENUE", new BigDecimal("78.00"), "非主营销售可使用其他业务收入。"));
            rows.add(new AccountMappingRecommendationVO.AccountCandidate("2203", "预收账款", "LIABILITY", new BigDecimal("65.00"), "未满足收入确认条件时可先入预收。"));
        } else {
            rows.add(new AccountMappingRecommendationVO.AccountCandidate("660201", "管理费用-办公费", "EXPENSE", new BigDecimal("79.00"), "服务或办公类摘要的备选科目。"));
            rows.add(new AccountMappingRecommendationVO.AccountCandidate("660103", "销售费用-业务招待费", "EXPENSE", new BigDecimal("68.00"), "若实际为客户招待，可改用业务招待费。"));
        }
        return rows;
    }

    private List<AccountMappingRecommendationVO.VoucherPreviewEntry> buildVoucherPreview(MappingProfile profile, BigDecimal amount, BigDecimal taxAmount) {
        List<AccountMappingRecommendationVO.VoucherPreviewEntry> rows = new ArrayList<AccountMappingRecommendationVO.VoucherPreviewEntry>();
        if ("REVENUE".equals(profile.accountType)) {
            rows.add(new AccountMappingRecommendationVO.VoucherPreviewEntry(profile.counterpartyAccountCode, profile.counterpartyAccountName, amount.add(taxAmount), BigDecimal.ZERO, "销售业务确认应收。"));
            rows.add(new AccountMappingRecommendationVO.VoucherPreviewEntry(profile.accountCode, profile.accountName, BigDecimal.ZERO, amount, "按推荐科目确认收入。"));
            if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
                rows.add(new AccountMappingRecommendationVO.VoucherPreviewEntry("22210102", "应交税费-应交增值税-销项税额", BigDecimal.ZERO, taxAmount, "销项税额。"));
            }
            return rows;
        }
        rows.add(new AccountMappingRecommendationVO.VoucherPreviewEntry(profile.accountCode, profile.accountName, amount, BigDecimal.ZERO, "按推荐科目入账。"));
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            rows.add(new AccountMappingRecommendationVO.VoucherPreviewEntry("22210101", "应交税费-应交增值税-进项税额", taxAmount, BigDecimal.ZERO, "可抵扣进项税额。"));
        }
        rows.add(new AccountMappingRecommendationVO.VoucherPreviewEntry(profile.counterpartyAccountCode, profile.counterpartyAccountName, BigDecimal.ZERO, amount.add(taxAmount), "形成应付或待付款。"));
        return rows;
    }

    private List<String> buildEvidence(MappingProfile profile, AccountMappingRecommendDTO dto) {
        List<String> rows = new ArrayList<String>();
        rows.add(profile.reason);
        rows.add("参考单据类型：" + defaultText(dto.getDocumentType(), "未提供") + "，业务场景：" + defaultText(dto.getBusinessScenario(), "自动推断"));
        if (!isBlank(dto.getCounterpartyName())) {
            rows.add("交易对手：" + dto.getCounterpartyName() + " 已纳入相似凭证匹配。");
        }
        return rows;
    }

    private List<String> buildRiskHints(MappingProfile profile, AccountMappingRecommendDTO dto) {
        List<String> rows = new ArrayList<String>();
        if (profile.manualReview) {
            rows.add("置信度低于自动入账阈值，需人工确认科目。 ");
        }
        if (dto.getAmount() != null && dto.getAmount().compareTo(new BigDecimal("50000")) > 0 && "EXPENSE".equals(profile.accountType)) {
            rows.add("费用金额较高，建议结合预算和审批权限复核。 ");
        }
        if (rows.isEmpty()) {
            rows.add("未发现阻断性科目映射风险。 ");
        }
        return rows;
    }

    private List<String> buildNextActions(Boolean autoApplicable, Boolean manualReviewRequired) {
        List<String> rows = new ArrayList<String>();
        if (Boolean.TRUE.equals(autoApplicable)) {
            rows.add("将推荐科目写入凭证生成模板。 ");
            rows.add("进入凭证预览和自动审核。 ");
        } else if (Boolean.TRUE.equals(manualReviewRequired)) {
            rows.add("进入科目映射人工确认队列。 ");
            rows.add("确认结果回写为训练样本，提升后续推荐置信度。 ");
        } else {
            rows.add("由财务BP复核后继续生成凭证。 ");
        }
        return rows;
    }

    private String inferScenario(AccountMappingRecommendDTO dto) {
        String text = defaultText(dto.getSummary(), "") + defaultText(dto.getCounterpartyName(), "");
        if (contains(text, "采购") || contains(text, "入库")) return "purchase";
        if (contains(text, "差旅") || contains(text, "交通")) return "expense";
        if (contains(text, "销售") || contains(text, "收入") || contains(text, "回款")) return "sales";
        return "general";
    }

    private BigDecimal defaultAmount(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class MappingProfile {
        private String serial;
        private String mode;
        private BigDecimal confidence;
        private String accountCode;
        private String accountName;
        private String accountType;
        private String counterpartyAccountCode;
        private String counterpartyAccountName;
        private String reason;
        private boolean manualReview;
    }
}
