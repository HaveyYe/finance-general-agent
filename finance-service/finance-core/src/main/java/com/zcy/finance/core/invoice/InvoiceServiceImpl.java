package com.zcy.finance.core.invoice;

import com.zcy.finance.api.InvoiceService;
import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.InvoiceInputDTO;
import com.zcy.finance.api.dto.InvoiceQueryDTO;
import com.zcy.finance.api.dto.InvoiceVerifyDTO;
import com.zcy.finance.api.vo.InvoiceCheckVO;
import com.zcy.finance.api.vo.InvoiceDupVO;
import com.zcy.finance.api.vo.InvoiceDuplicateVO;
import com.zcy.finance.api.vo.InvoiceVerificationVO;
import com.zcy.finance.api.vo.InvoiceVO;
import com.zcy.finance.infra.invoice.InvoiceRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = InvoiceService.class)
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository repository;

    public InvoiceServiceImpl(InvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<InvoiceVO> inputInvoice(InvoiceInputDTO dto) {
        if (dto == null || isBlank(dto.getInvoiceNo())) {
            return Result.failure("invoiceNo is required");
        }
        if (dto.getAmount() == null) {
            return Result.failure("amount is required");
        }
        List<InvoiceDuplicateVO> duplicates = duplicateRows(dto);
        InvoiceVO vo = new InvoiceVO();
        vo.setInvoiceNo(dto.getInvoiceNo());
        vo.setInvoiceCode(defaultString(dto.getInvoiceCode(), "CODE-" + dto.getInvoiceNo()));
        vo.setInvoiceDate(defaultString(dto.getInvoiceDate(), "2026-05-31"));
        vo.setInvoiceType(defaultString(dto.getInvoiceType(), "增值税专用发票"));
        vo.setBuyerName(defaultString(dto.getBuyerName(), "杭州云启科技有限公司"));
        vo.setSellerName(defaultString(dto.getSellerName(), "待识别供应商"));
        vo.setAmount(dto.getAmount());
        vo.setTaxAmount(dto.getTaxAmount() == null ? BigDecimal.ZERO : dto.getTaxAmount());
        vo.setTotalAmount(vo.getAmount().add(vo.getTaxAmount()));
        vo.setSource(defaultString(dto.getSource(), "MANUAL"));
        vo.setFileHash(dto.getFileHash());
        vo.setDuplicate(!duplicates.isEmpty());
        vo.setRiskLevel(duplicates.isEmpty() ? "LOW" : "HIGH");
        vo.setInputStatus(duplicates.isEmpty() ? "RECORDED" : "DUPLICATE_BLOCKED");
        vo.setVerifyStatus(dto.isAutoVerify() && duplicates.isEmpty() ? "待自动验真" : "待复核");
        if (duplicates.isEmpty()) {
            repository.insert(vo);
        }
        return Result.success(vo);
    }

    @Override
    public Result<PageResult<InvoiceVO>> query(InvoiceQueryDTO dto) {
        List<InvoiceVO> rows = repository.findAll();
        if (dto != null && dto.getInvoiceNo() != null && dto.getInvoiceNo().trim().length() > 0) {
            List<InvoiceVO> filtered = new ArrayList<InvoiceVO>();
            for (InvoiceVO row : rows) {
                if (row.getInvoiceNo().contains(dto.getInvoiceNo())) {
                    filtered.add(row);
                }
            }
            rows = filtered;
        }
        if (dto != null && dto.getDateRange() != null && dto.getDateRange().size() >= 2) {
            List<InvoiceVO> filtered = new ArrayList<InvoiceVO>();
            String start = dto.getDateRange().get(0);
            String end = dto.getDateRange().get(1);
            for (InvoiceVO row : rows) {
                if (between(row.getInvoiceDate(), start, end)) {
                    filtered.add(row);
                }
            }
            rows = filtered;
        }
        return Result.success(PageResult.of(rows, rows.size(), 1, rows.size()));
    }

    @Override
    public Result<PageResult<InvoiceVO>> queryLedger(InvoiceQueryDTO dto) {
        return query(dto);
    }

    @Override
    public Result<InvoiceCheckVO> checkInvoice(String invoiceCode, String invoiceNo) {
        if (isBlank(invoiceCode) || isBlank(invoiceNo)) {
            return Result.failure("发票代码和发票号码不能为空");
        }
        InvoiceVO invoice = findInvoice(invoiceNo);
        InvoiceCheckVO vo = new InvoiceCheckVO();
        vo.setInvoiceCode(invoiceCode);
        vo.setInvoiceNo(invoiceNo);
        vo.setCheckedAt("2026-06-04 10:00:00");
        if (invoice == null) {
            vo.setAuthentic(false);
            vo.setAmountMatched(false);
            vo.setCheckStatus("NOT_FOUND");
            vo.setRiskLevel("HIGH");
            vo.setHints(Arrays.asList("税务查验未找到匹配发票。", "请核对发票代码和号码后重试。"));
            return Result.success(vo);
        }
        boolean codeMatched = invoiceCode.equals(invoice.getInvoiceCode());
        vo.setAuthentic(codeMatched);
        vo.setAmountMatched(codeMatched);
        vo.setAmount(invoice.getAmount());
        vo.setTaxAmount(invoice.getTaxAmount());
        vo.setSellerName(invoice.getSellerName());
        vo.setCheckStatus(codeMatched ? "MATCHED" : "CODE_MISMATCH");
        vo.setRiskLevel(codeMatched ? "LOW" : "HIGH");
        vo.setHints(codeMatched
                ? Arrays.asList("发票代码、号码和金额信息查验一致。")
                : Arrays.asList("发票号码存在，但发票代码不一致。", "已拦截自动入账。"));
        return Result.success(vo);
    }

    @Override
    public Result<List<InvoiceDuplicateVO>> checkDuplicate(InvoiceInputDTO dto) {
        if (dto == null || isBlank(dto.getInvoiceNo())) {
            return Result.failure("invoiceNo is required");
        }
        return Result.success(duplicateRows(dto));
    }

    @Override
    public Result<List<InvoiceDupVO>> checkDuplicate(String invoiceCode, String invoiceNo) {
        if (isBlank(invoiceCode) || isBlank(invoiceNo)) {
            return Result.failure("发票代码和发票号码不能为空");
        }
        InvoiceInputDTO dto = new InvoiceInputDTO();
        dto.setInvoiceCode(invoiceCode);
        dto.setInvoiceNo(invoiceNo);
        List<InvoiceDupVO> rows = new ArrayList<InvoiceDupVO>();
        for (InvoiceDuplicateVO source : duplicateRows(dto)) {
            InvoiceDupVO row = new InvoiceDupVO();
            row.setInvoiceCode(invoiceCode);
            row.setInvoiceNo(invoiceNo);
            row.setMatchedInvoiceNo(source.getMatchedInvoiceNo());
            row.setDuplicateType(source.getDuplicateType());
            row.setSimilarity(source.getSimilarity());
            row.setSource(source.getMatchedSource());
            row.setRiskLevel(source.getRiskLevel());
            row.setSuggestion(source.getSuggestions().isEmpty() ? "转人工复核。" : source.getSuggestions().get(0));
            rows.add(row);
        }
        return Result.success(rows);
    }

    @Override
    public Result<InvoiceVerificationVO> verify(InvoiceVerifyDTO dto) {
        if (dto == null || isBlank(dto.getInvoiceNo())) {
            return Result.failure("invoiceNo is required");
        }

        InvoiceVO ledgerInvoice = findInvoice(dto.getInvoiceNo());
        InvoiceVerificationVO vo = new InvoiceVerificationVO();
        vo.setInvoiceNo(dto.getInvoiceNo());
        vo.setInvoiceType(defaultString(dto.getInvoiceType(), "增值税专用发票"));
        vo.setArchiveStatus(dto.isIncludeArchiveCheck() ? "ARCHIVED_WITH_OCR_AND_VERIFY_LOG" : "SKIPPED");

        if (ledgerInvoice == null) {
            vo.setVerifyStatus("查无此票");
            vo.setTaxAuthorityStatus("NOT_FOUND");
            vo.setTaxAuthorityMatched(false);
            vo.setRiskLevel("HIGH");
            vo.setRiskHints(Arrays.asList(
                    "税务查验平台未返回匹配记录，可能为未同步、号码错误或异常发票。",
                    "已拦截自动入账，建议24小时后重试查验或转人工复核。"
            ));
            vo.setNextActions(Arrays.asList("创建待复查任务", "暂缓生成凭证", "通知提交人补充原件"));
            return Result.success(vo);
        }

        List<String> riskHints = new ArrayList<String>();
        List<String> nextActions = new ArrayList<String>();
        boolean amountMatched = amountMatched(dto.getAmount(), ledgerInvoice.getAmount());
        boolean taxMatched = amountMatched(dto.getTaxAmount(), ledgerInvoice.getTaxAmount());
        boolean sellerMatched = isBlank(dto.getSellerName()) || ledgerInvoice.getSellerName().contains(dto.getSellerName()) || dto.getSellerName().contains(ledgerInvoice.getSellerName());

        vo.setDuplicate(true);
        vo.setDuplicateRefs(Arrays.asList("INVOICE_LEDGER:" + ledgerInvoice.getInvoiceNo(), "ARCHIVE:PDF-" + ledgerInvoice.getInvoiceNo()));

        if (!amountMatched || !taxMatched || !sellerMatched) {
            vo.setVerifyStatus("查验不一致");
            vo.setTaxAuthorityStatus("MISMATCH");
            vo.setTaxAuthorityMatched(false);
            vo.setRiskLevel("HIGH");
            if (!amountMatched) {
                riskHints.add("发票金额与税务查验记录不一致。");
            }
            if (!taxMatched) {
                riskHints.add("税额与税务查验记录不一致。");
            }
            if (!sellerMatched) {
                riskHints.add("销方名称与税务查验记录不一致。");
            }
            riskHints.add("检测到台账中已存在同号发票，需确认是否重复报销或重复入账。");
            nextActions.add("拦截入账并推送高风险预警");
            nextActions.add("要求提交人重新上传原票");
        } else if ("待复核".equals(ledgerInvoice.getVerifyStatus())) {
            vo.setVerifyStatus("待复查");
            vo.setTaxAuthorityStatus("PENDING_RETRY");
            vo.setTaxAuthorityMatched(false);
            vo.setRiskLevel("MEDIUM");
            riskHints.add("税务端查验存在时间差，当前发票进入待复查队列。");
            riskHints.add("供应商短期内存在连续号码发票，建议结合合同和验收单复核业务真实性。");
            nextActions.add("24小时后自动重试税务查验");
            nextActions.add("凭证生成前要求人工确认");
        } else {
            vo.setVerifyStatus("查验一致");
            vo.setTaxAuthorityStatus("MATCHED");
            vo.setTaxAuthorityMatched(true);
            vo.setRiskLevel("LOW");
            riskHints.add("发票号码、金额、税额和销方信息均与税务端记录一致。");
            riskHints.add("检测到台账中已存在同号发票，重复提交时应提示用户引用原档案。");
            nextActions.add("允许进入抵扣判断和凭证生成流程");
            nextActions.add("自动关联电子档案和查验日志");
        }

        vo.setSerialRisk(dto.getInvoiceNo().endsWith("002") || dto.getInvoiceNo().endsWith("003"));
        if (vo.isSerialRisk() && !"HIGH".equals(vo.getRiskLevel())) {
            riskHints.add("同一供应商短期内出现连续号码发票，已加入连号监控。");
        }

        if (dto.isIncludeDeductionCheck()) {
            boolean deductible = vo.isTaxAuthorityMatched() && isDeductibleType(vo.getInvoiceType()) && ledgerInvoice.getTaxAmount().compareTo(BigDecimal.ZERO) > 0;
            vo.setDeductible(deductible);
            vo.setDeductibleTaxAmount(deductible ? ledgerInvoice.getTaxAmount() : BigDecimal.ZERO);
            if (!deductible) {
                riskHints.add("当前发票未满足自动抵扣条件，需人工确认用途或票种。");
            }
        }
        vo.setRiskHints(riskHints);
        vo.setNextActions(nextActions);
        return Result.success(vo);
    }

    private InvoiceVO findInvoice(String invoiceNo) {
        return repository.findByNo(invoiceNo);
    }

    private boolean amountMatched(BigDecimal input, BigDecimal expected) {
        return input == null || expected == null || input.compareTo(expected) == 0;
    }

    private boolean between(String value, String start, String end) {
        if (value == null) {
            return false;
        }
        boolean afterStart = isBlank(start) || value.compareTo(start) >= 0;
        boolean beforeEnd = isBlank(end) || value.compareTo(end) <= 0;
        return afterStart && beforeEnd;
    }

    private boolean isDeductibleType(String invoiceType) {
        return invoiceType != null && (invoiceType.contains("专用") || invoiceType.contains("全电") || invoiceType.contains("机动车"));
    }

    private List<InvoiceDuplicateVO> duplicateRows(InvoiceInputDTO dto) {
        List<InvoiceDuplicateVO> rows = new ArrayList<InvoiceDuplicateVO>();
        for (InvoiceVO existing : repository.findAll()) {
            InvoiceDuplicateVO duplicate = matchDuplicate(dto, existing);
            if (duplicate != null) {
                rows.add(duplicate);
            }
        }
        return rows;
    }

    private InvoiceDuplicateVO matchDuplicate(InvoiceInputDTO dto, InvoiceVO existing) {
        boolean sameNo = dto.getInvoiceNo() != null && dto.getInvoiceNo().equals(existing.getInvoiceNo());
        boolean sameCode = !isBlank(dto.getInvoiceCode()) && dto.getInvoiceCode().equals(existing.getInvoiceCode());
        boolean sameHash = !isBlank(dto.getFileHash()) && dto.getFileHash().equals(existing.getFileHash());
        boolean sameSellerAmount = !isBlank(dto.getSellerName())
                && existing.getSellerName() != null
                && existing.getSellerName().contains(dto.getSellerName())
                && amountMatched(dto.getAmount(), existing.getAmount());
        if (!sameNo && !sameCode && !sameHash && !sameSellerAmount) {
            return null;
        }

        InvoiceDuplicateVO vo = new InvoiceDuplicateVO();
        vo.setDuplicateNo("DUP-" + dto.getInvoiceNo() + "-" + existing.getInvoiceNo());
        vo.setInvoiceCode(dto.getInvoiceCode());
        vo.setInvoiceNo(dto.getInvoiceNo());
        vo.setMatchedInvoiceNo(existing.getInvoiceNo());
        vo.setMatchedSource(defaultString(existing.getSource(), "INVOICE_LEDGER"));
        vo.setStatus("BLOCKED");
        if (sameNo && sameCode) {
            vo.setDuplicateType("SAME_CODE_AND_NO");
            vo.setRiskLevel("HIGH");
            vo.setSimilarity(new BigDecimal("100.00"));
            vo.setEvidence(Arrays.asList("发票代码和发票号码完全一致。", "台账中已存在同票记录：" + existing.getInvoiceNo()));
        } else if (sameNo) {
            vo.setDuplicateType("SAME_NO");
            vo.setRiskLevel("HIGH");
            vo.setSimilarity(new BigDecimal("96.00"));
            vo.setEvidence(Arrays.asList("发票号码完全一致。", "需确认是否为重复上传或重复报销。"));
        } else if (sameHash) {
            vo.setDuplicateType("SAME_FILE_HASH");
            vo.setRiskLevel("HIGH");
            vo.setSimilarity(new BigDecimal("98.00"));
            vo.setEvidence(Arrays.asList("电子发票文件哈希一致。", "疑似同一PDF重复提交。"));
        } else {
            vo.setDuplicateType("SAME_SELLER_AMOUNT");
            vo.setRiskLevel("MEDIUM");
            vo.setSimilarity(new BigDecimal("88.00"));
            vo.setEvidence(Arrays.asList("销方名称和不含税金额高度一致。", "建议结合开票日期、合同和验收单复核。"));
        }
        vo.setSuggestions(Arrays.asList("拦截自动入账，提示用户引用原发票档案。", "如确认为拆分业务，需上传说明并转人工复核。"));
        return vo;
    }

    private static InvoiceVO enrichSeedInvoice(InvoiceVO source) {
        InvoiceVO vo = copyInvoice(source);
        vo.setInvoiceCode("CODE-" + source.getInvoiceNo());
        vo.setInvoiceType("增值税专用发票");
        vo.setTotalAmount(zeroIfNull(source.getAmount()).add(zeroIfNull(source.getTaxAmount())));
        vo.setInputStatus("RECORDED");
        vo.setSource("DEMO_LEDGER");
        vo.setFileHash("HASH-" + source.getInvoiceNo());
        vo.setDuplicate(false);
        vo.setRiskLevel("待复核".equals(source.getVerifyStatus()) ? "MEDIUM" : "LOW");
        return vo;
    }

    private static List<InvoiceVO> copyInvoices(List<InvoiceVO> source) {
        List<InvoiceVO> rows = new ArrayList<InvoiceVO>();
        for (InvoiceVO invoice : source) {
            rows.add(copyInvoice(invoice));
        }
        return rows;
    }

    private static InvoiceVO copyInvoice(InvoiceVO source) {
        InvoiceVO vo = new InvoiceVO();
        vo.setInvoiceNo(source.getInvoiceNo());
        vo.setInvoiceCode(source.getInvoiceCode());
        vo.setInvoiceDate(source.getInvoiceDate());
        vo.setInvoiceType(source.getInvoiceType());
        vo.setBuyerName(source.getBuyerName());
        vo.setSellerName(source.getSellerName());
        vo.setAmount(source.getAmount());
        vo.setTaxAmount(source.getTaxAmount());
        vo.setTotalAmount(source.getTotalAmount());
        vo.setVerifyStatus(source.getVerifyStatus());
        vo.setInputStatus(source.getInputStatus());
        vo.setSource(source.getSource());
        vo.setFileHash(source.getFileHash());
        vo.setDuplicate(source.isDuplicate());
        vo.setRiskLevel(source.getRiskLevel());
        return vo;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultString(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
