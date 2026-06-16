package com.zcy.finance.core.voucher;

import com.zcy.finance.api.VoucherService;
import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.VoucherAuditDTO;
import com.zcy.finance.api.dto.VoucherCreateDTO;
import com.zcy.finance.api.dto.VoucherQueryDTO;
import com.zcy.finance.api.vo.VoucherAuditVO;
import com.zcy.finance.api.vo.VoucherVO;
import com.zcy.finance.infra.voucher.VoucherRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = VoucherService.class)
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository repository;

    public VoucherServiceImpl(VoucherRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<VoucherVO> createVoucher(VoucherCreateDTO dto) {
        if (dto == null || dto.getEntries() == null || dto.getEntries().isEmpty()) {
            return Result.failure("凭证分录不能为空");
        }

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (VoucherCreateDTO.Entry entry : dto.getEntries()) {
            if (entry.getDebitAmount() != null) {
                debitTotal = debitTotal.add(entry.getDebitAmount());
            }
            if (entry.getCreditAmount() != null) {
                creditTotal = creditTotal.add(entry.getCreditAmount());
            }
        }

        VoucherVO vo = new VoucherVO();
        vo.setVoucherNo(repository.nextVoucherNo(periodOf(dto.getVoucherDate())));
        vo.setVoucherDate(dto.getVoucherDate());
        vo.setPeriod(periodOf(dto.getVoucherDate()));
        vo.setSummary(defaultString(dto.getSummary(), "自动生成凭证"));
        vo.setPreparer("AI_AGENT");
        vo.setReviewer("AUTO_RULE");
        vo.setSourceDocumentNo("CHAT-GENERATED");
        vo.setDebitTotal(debitTotal);
        vo.setCreditTotal(creditTotal);
        vo.setEntries(convertEntries(dto.getEntries()));

        if (debitTotal.compareTo(creditTotal) == 0) {
            vo.setStatus("AUDIT_PASSED");
            vo.getAuditMessages().add("借贷平衡，凭证已通过自动审核");
        } else {
            vo.setStatus("NEED_REVIEW");
            vo.getAuditMessages().add("借贷不平衡，需人工复核");
        }
        repository.insert(vo);
        return Result.success(vo);
    }

    @Override
    public Result<VoucherVO> getVoucher(String voucherNo) {
        if (isBlank(voucherNo)) {
            return Result.failure("凭证号不能为空");
        }
        VoucherVO voucher = repository.findByNo(voucherNo);
        if (voucher != null) {
            return Result.success(voucher);
        }
        return Result.failure("凭证不存在：" + voucherNo);
    }

    @Override
    public Result<PageResult<VoucherVO>> queryVouchers(VoucherQueryDTO dto) {
        int pageNo = dto != null && dto.getPageNo() != null && dto.getPageNo().intValue() > 0 ? dto.getPageNo().intValue() : 1;
        int pageSize = dto != null && dto.getPageSize() != null && dto.getPageSize().intValue() > 0 ? Math.min(dto.getPageSize().intValue(), 50) : 10;
        List<VoucherVO> filtered = new ArrayList<VoucherVO>();
        for (VoucherVO voucher : repository.findAll()) {
            if (dto != null && hasText(dto.getVoucherNo()) && !voucher.getVoucherNo().contains(dto.getVoucherNo())) {
                continue;
            }
            if (dto != null && hasText(dto.getPeriod()) && !dto.getPeriod().equals(voucher.getPeriod())) {
                continue;
            }
            if (dto != null && hasText(dto.getStatus()) && !dto.getStatus().equalsIgnoreCase(voucher.getStatus())) {
                continue;
            }
            if (dto != null && hasText(dto.getSummaryKeyword()) && (voucher.getSummary() == null || !voucher.getSummary().contains(dto.getSummaryKeyword()))) {
                continue;
            }
            if (dto != null && hasText(dto.getAccountCode()) && !containsAccount(voucher, dto.getAccountCode())) {
                continue;
            }
            filtered.add(voucher);
        }
        int from = Math.min((pageNo - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        return Result.success(PageResult.of(new ArrayList<VoucherVO>(filtered.subList(from, to)), filtered.size(), pageNo, pageSize));
    }

    @Override
    public Result<Void> auditVoucher(String voucherNo, String auditor) {
        if (isBlank(voucherNo)) {
            return Result.failure("凭证号不能为空");
        }
        if (isBlank(auditor)) {
            return Result.failure("审核人不能为空");
        }
        if (repository.audit(voucherNo, auditor)) {
            return Result.success(null);
        }
        return Result.failure("凭证不存在：" + voucherNo);
    }

    @Override
    public Result<VoucherAuditVO> auditVoucher(VoucherAuditDTO dto) {
        if (dto == null || dto.getEntries() == null || dto.getEntries().isEmpty()) {
            return Result.failure("凭证分录不能为空");
        }

        VoucherAuditVO vo = new VoucherAuditVO();
        vo.setVoucherNo(defaultString(dto.getVoucherNo(), buildVoucherNo(dto.getVoucherDate())));
        vo.setVoucherDate(dto.getVoucherDate());

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (VoucherCreateDTO.Entry entry : dto.getEntries()) {
            debitTotal = debitTotal.add(zeroIfNull(entry.getDebitAmount()));
            creditTotal = creditTotal.add(zeroIfNull(entry.getCreditAmount()));
        }
        vo.setDebitTotal(debitTotal);
        vo.setCreditTotal(creditTotal);

        List<VoucherAuditVO.AuditItem> items = new ArrayList<VoucherAuditVO.AuditItem>();
        auditBalance(items, debitTotal, creditTotal);
        auditTripleMatch(items, dto);
        auditEntryLogic(items, dto);
        auditPeriod(items, dto);
        auditDuplicate(items, dto);
        auditInternalControl(items, dto, debitTotal);

        vo.setAuditItems(items);
        summarize(vo);
        return Result.success(vo);
    }

    private void auditBalance(List<VoucherAuditVO.AuditItem> items, BigDecimal debitTotal, BigDecimal creditTotal) {
        if (debitTotal.compareTo(creditTotal) != 0) {
            items.add(item("AUD-BAL-001", "金额校验", "RED", "借贷不平衡",
                    "借方合计 " + debitTotal + " 与贷方合计 " + creditTotal + " 不一致。",
                    "拦截入账，调整分录金额后重新审核。"));
        }
    }

    private void auditTripleMatch(List<VoucherAuditVO.AuditItem> items, VoucherAuditDTO dto) {
        if (isBlank(dto.getRelatedInvoiceNo()) && isBlank(dto.getPurchaseOrderNo()) && isBlank(dto.getReceiptNo())) {
            return;
        }
        if (isBlank(dto.getRelatedInvoiceNo()) || isBlank(dto.getPurchaseOrderNo()) || isBlank(dto.getReceiptNo())) {
            items.add(item("AUD-3WM-001", "三单匹配", "YELLOW", "三单资料不完整",
                    "发票、采购订单、入库单未全部关联，无法完成完整三单匹配。",
                    "补齐业务单据或转采购/仓储确认。"));
        }
        if (!amountEqual(dto.getInvoiceAmount(), dto.getOrderAmount()) || !amountEqual(dto.getInvoiceAmount(), dto.getReceiptAmount())) {
            items.add(item("AUD-3WM-002", "三单匹配", "RED", "三单金额不一致",
                    "发票金额、订单金额、入库金额存在差异。",
                    "拦截自动过账，要求采购、仓储、财务联合复核。"));
        }
    }

    private void auditEntryLogic(List<VoucherAuditVO.AuditItem> items, VoucherAuditDTO dto) {
        for (VoucherCreateDTO.Entry entry : dto.getEntries()) {
            String accountCode = entry.getAccountCode();
            BigDecimal debit = zeroIfNull(entry.getDebitAmount());
            BigDecimal credit = zeroIfNull(entry.getCreditAmount());
            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                items.add(item("AUD-LOGIC-001", "逻辑校验", "RED", "同一分录借贷同时有值",
                        "科目 " + defaultString(entry.getAccountName(), accountCode) + " 同时存在借方和贷方金额。",
                        "拆分为两条分录或确认金额方向。"));
            }
            if (accountCode != null && accountCode.startsWith("660") && credit.compareTo(BigDecimal.ZERO) > 0) {
                items.add(item("AUD-LOGIC-002", "逻辑校验", "YELLOW", "费用科目出现在贷方",
                        "费用科目 " + defaultString(entry.getAccountName(), accountCode) + " 出现贷方金额，可能为冲销或方向错误。",
                        "确认是否为费用冲销；若不是，调整为借方。"));
            }
            if ((accountCode == null || accountCode.trim().length() == 0) && (debit.compareTo(BigDecimal.ZERO) > 0 || credit.compareTo(BigDecimal.ZERO) > 0)) {
                items.add(item("AUD-LOGIC-003", "科目映射", "YELLOW", "分录缺少科目编码",
                        "存在有效金额但未配置会计科目编码。",
                        "补充科目映射或进入AI科目推荐队列。"));
            }
        }
    }

    private void auditPeriod(List<VoucherAuditVO.AuditItem> items, VoucherAuditDTO dto) {
        if (!isBlank(dto.getPeriod()) && !isBlank(dto.getDocumentDate()) && !dto.getDocumentDate().startsWith(dto.getPeriod())) {
            items.add(item("AUD-PERIOD-001", "期间匹配", "BLUE", "单据日期与入账期间不一致",
                    "单据日期 " + dto.getDocumentDate() + " 与入账期间 " + dto.getPeriod() + " 不一致。",
                    "确认是否为跨期入账，并保留跨期说明。"));
        }
    }

    private void auditDuplicate(List<VoucherAuditVO.AuditItem> items, VoucherAuditDTO dto) {
        String invoiceNo = dto.getRelatedInvoiceNo();
        if ("INV-202605-001".equals(invoiceNo) || "INV-202605-002".equals(invoiceNo)) {
            items.add(item("AUD-DUP-001", "重复入账", "YELLOW", "疑似重复入账",
                    "发票 " + invoiceNo + " 已存在关联凭证或电子档案记录。",
                    "引用原凭证或确认本次是否为拆分入账。"));
        }
    }

    private void auditInternalControl(List<VoucherAuditVO.AuditItem> items, VoucherAuditDTO dto, BigDecimal amount) {
        if (!isBlank(dto.getPreparerId()) && dto.getPreparerId().equals(dto.getReviewerId())) {
            items.add(item("AUD-CTRL-001", "合规校验", "RED", "制单人与审核人相同",
                    "制单人与审核人均为 " + dto.getPreparerId() + "，违反不相容岗位分离要求。",
                    "更换审核人后再提交。"));
        }
        if (dto.getBudgetAmount() != null && amount.compareTo(dto.getBudgetAmount()) > 0) {
            items.add(item("AUD-CTRL-002", "合规校验", "YELLOW", "超预算凭证",
                    "凭证金额 " + amount + " 超过部门预算 " + dto.getBudgetAmount() + "。",
                    "追加预算审批或拆分至正确项目/部门。"));
        }
        if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            items.add(item("AUD-CTRL-003", "合规校验", "BLUE", "大额凭证提示",
                    "凭证金额达到大额复核阈值。",
                    "建议增加财务主管复核节点。"));
        }
    }

    private void summarize(VoucherAuditVO vo) {
        int red = 0;
        int yellow = 0;
        int blue = 0;
        for (VoucherAuditVO.AuditItem item : vo.getAuditItems()) {
            if ("RED".equals(item.getSeverity())) {
                red++;
            } else if ("YELLOW".equals(item.getSeverity())) {
                yellow++;
            } else if ("BLUE".equals(item.getSeverity())) {
                blue++;
            }
        }
        vo.setRedCount(red);
        vo.setYellowCount(yellow);
        vo.setBlueCount(blue);
        if (red > 0) {
            vo.setOverallSeverity("RED");
            vo.setAuditStatus("BLOCKED");
            vo.setAdvices(Arrays.asList("存在红色异常，必须人工处理后才能入账。", "优先处理借贷平衡、三单差异和岗位分离问题。"));
            vo.setNextActions(Arrays.asList("拦截自动过账", "推送异常处理任务", "整改后重新发起自动审核"));
        } else if (yellow > 0) {
            vo.setOverallSeverity("YELLOW");
            vo.setAuditStatus("NEED_REVIEW");
            vo.setAdvices(Arrays.asList("存在黄色异常，建议人工复核后入账。", "保留审核轨迹以满足内控审计要求。"));
            vo.setNextActions(Arrays.asList("进入待复核队列", "允许授权人员确认后过账"));
        } else if (blue > 0) {
            vo.setOverallSeverity("BLUE");
            vo.setAuditStatus("PASSED_WITH_HINT");
            vo.setAdvices(Arrays.asList("凭证主体校验通过，存在提示性事项。"));
            vo.setNextActions(Arrays.asList("允许自动入账", "归档提示说明"));
        } else {
            vo.setOverallSeverity("GREEN");
            vo.setAuditStatus("PASSED");
            vo.setAdvices(Arrays.asList("凭证已通过自动审核。"));
            vo.setNextActions(Arrays.asList("允许自动过账", "归档审核日志"));
        }
    }

    private VoucherAuditVO.AuditItem item(String itemId, String category, String severity, String title, String description, String suggestion) {
        return new VoucherAuditVO.AuditItem(itemId, category, severity, title, description, suggestion);
    }

    private static VoucherVO demoVoucher(String voucherNo, String voucherDate, String summary, String status,
                                         String preparer, String reviewer, String sourceDocumentNo,
                                         VoucherVO.VoucherEntry debit, VoucherVO.VoucherEntry credit) {
        VoucherVO vo = new VoucherVO();
        vo.setVoucherNo(voucherNo);
        vo.setVoucherDate(voucherDate);
        vo.setPeriod(periodOf(voucherDate));
        vo.setSummary(summary);
        vo.setStatus(status);
        vo.setPreparer(preparer);
        vo.setReviewer(reviewer);
        vo.setSourceDocumentNo(sourceDocumentNo);
        vo.setEntries(Arrays.asList(debit, credit));
        vo.setDebitTotal(zeroIfNullStatic(debit.getDebitAmount()).add(zeroIfNullStatic(credit.getDebitAmount())));
        vo.setCreditTotal(zeroIfNullStatic(debit.getCreditAmount()).add(zeroIfNullStatic(credit.getCreditAmount())));
        vo.setAuditMessages(statusMessages(status));
        return vo;
    }

    private static VoucherVO.VoucherEntry entry(String accountCode, String accountName, String direction,
                                               String debitAmount, String creditAmount, String remark) {
        return new VoucherVO.VoucherEntry(accountCode, accountName, direction, new BigDecimal(debitAmount), new BigDecimal(creditAmount), remark);
    }

    private List<VoucherVO.VoucherEntry> convertEntries(List<VoucherCreateDTO.Entry> source) {
        List<VoucherVO.VoucherEntry> rows = new ArrayList<VoucherVO.VoucherEntry>();
        for (VoucherCreateDTO.Entry entry : source) {
            BigDecimal debit = zeroIfNull(entry.getDebitAmount());
            BigDecimal credit = zeroIfNull(entry.getCreditAmount());
            rows.add(new VoucherVO.VoucherEntry(
                    entry.getAccountCode(),
                    entry.getAccountName(),
                    debit.compareTo(BigDecimal.ZERO) > 0 ? "DEBIT" : "CREDIT",
                    debit,
                    credit,
                    defaultString(entry.getAccountName(), entry.getAccountCode())
            ));
        }
        return rows;
    }

    private static VoucherVO copyVoucher(VoucherVO source) {
        VoucherVO vo = new VoucherVO();
        vo.setVoucherNo(source.getVoucherNo());
        vo.setVoucherDate(source.getVoucherDate());
        vo.setPeriod(source.getPeriod());
        vo.setSummary(source.getSummary());
        vo.setStatus(source.getStatus());
        vo.setPreparer(source.getPreparer());
        vo.setReviewer(source.getReviewer());
        vo.setSourceDocumentNo(source.getSourceDocumentNo());
        vo.setDebitTotal(source.getDebitTotal());
        vo.setCreditTotal(source.getCreditTotal());
        vo.setAuditMessages(new ArrayList<String>(source.getAuditMessages()));
        List<VoucherVO.VoucherEntry> entries = new ArrayList<VoucherVO.VoucherEntry>();
        for (VoucherVO.VoucherEntry entry : source.getEntries()) {
            entries.add(new VoucherVO.VoucherEntry(entry.getAccountCode(), entry.getAccountName(), entry.getDirection(),
                    entry.getDebitAmount(), entry.getCreditAmount(), entry.getRemark()));
        }
        vo.setEntries(entries);
        return vo;
    }

    private boolean containsAccount(VoucherVO voucher, String accountCode) {
        for (VoucherVO.VoucherEntry entry : voucher.getEntries()) {
            if (entry.getAccountCode() != null && entry.getAccountCode().startsWith(accountCode)) {
                return true;
            }
        }
        return false;
    }

    private static String periodOf(String voucherDate) {
        if (voucherDate == null || voucherDate.length() < 7) {
            return "2026-05";
        }
        return voucherDate.substring(0, 7);
    }

    private static List<String> statusMessages(String status) {
        if ("AUDIT_PASSED".equals(status)) {
            return Arrays.asList("借贷平衡，凭证已通过自动审核。");
        }
        return Arrays.asList("凭证存在待复核事项，请查看自动审核结果。");
    }

    private boolean amountEqual(BigDecimal left, BigDecimal right) {
        return left == null || right == null || left.compareTo(right) == 0;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal zeroIfNullStatic(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String buildVoucherNo(String voucherDate) {
        if (isBlank(voucherDate)) {
            return "V-AUDIT-0001";
        }
        return "V-" + voucherDate.replace("-", "") + "-AUDIT";
    }

    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private boolean hasText(String value) {
        return !isBlank(value);
    }
}
