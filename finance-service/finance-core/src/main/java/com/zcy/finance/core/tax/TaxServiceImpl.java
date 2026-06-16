package com.zcy.finance.core.tax;

import com.zcy.finance.api.TaxService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.TaxCalculateDTO;
import com.zcy.finance.api.dto.TaxReturnGenerateDTO;
import com.zcy.finance.api.vo.TaxCalculationVO;
import com.zcy.finance.api.vo.TaxReturnVO;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

@DubboService(interfaceClass = TaxService.class)
public class TaxServiceImpl implements TaxService {

    private static final BigDecimal VAT_DEFAULT_RATE = new BigDecimal("0.13");
    private static final BigDecimal CIT_DEFAULT_RATE = new BigDecimal("0.25");

    @Override
    public Result<TaxCalculationVO> calculateTax(TaxCalculateDTO dto) {
        if (dto == null || dto.getTaxableAmount() == null) {
            return Result.failure("计税金额不能为空");
        }

        String taxType = normalizeTaxType(dto.getTaxType());
        BigDecimal rate = dto.getTaxRate() == null ? defaultRate(taxType) : normalizeRate(dto.getTaxRate());
        BigDecimal deductible = dto.getDeductibleAmount() == null ? BigDecimal.ZERO : dto.getDeductibleAmount();
        BigDecimal taxPayable = dto.getTaxableAmount().multiply(rate).subtract(deductible).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        TaxCalculationVO vo = new TaxCalculationVO();
        vo.setTaxType(taxType);
        vo.setPeriod(defaultPeriod(dto.getPeriod()));
        vo.setTaxableAmount(dto.getTaxableAmount());
        vo.setTaxRate(rate);
        vo.setDeductibleAmount(deductible);
        vo.setTaxPayable(taxPayable);
        vo.setCalculationSteps(Arrays.asList(
                "计税基础：" + dto.getTaxableAmount(),
                "适用税率：" + rate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%",
                "可抵扣/减免：" + deductible,
                "应纳税额 = 计税基础 × 税率 - 可抵扣/减免"
        ));
        vo.setRiskHints(Arrays.asList(
                "Demo 结果仅用于流程验证，正式申报需复核税率、计税方法和政策有效期。",
                "如存在简易计税、免税、留抵退税或税收优惠，应单独拆分测算。"
        ));
        return Result.success(vo);
    }

    @Override
    public Result<TaxReturnVO> generateTaxReturn(TaxReturnGenerateDTO dto) {
        String taxType = normalizeTaxType(dto == null ? null : dto.getTaxType());
        String period = defaultPeriod(dto == null ? null : dto.getPeriod());

        TaxReturnVO vo = new TaxReturnVO();
        vo.setReturnNo("TAX-" + period.replace("-", "") + "-0001");
        vo.setTaxType(taxType);
        vo.setPeriod(period);
        vo.setSalesAmount(new BigDecimal("1286500.00"));
        vo.setOutputTax(new BigDecimal("167245.00"));
        vo.setInputTax(new BigDecimal("102430.00"));
        vo.setTaxPayable(vo.getOutputTax().subtract(vo.getInputTax()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        vo.setStatus("DRAFT");
        vo.setRows(Arrays.asList(
                new TaxReturnVO.Row("销售额", vo.getSalesAmount(), "按 Demo 销项发票汇总"),
                new TaxReturnVO.Row("销项税额", vo.getOutputTax(), "按 13% 默认税率估算"),
                new TaxReturnVO.Row("进项税额", vo.getInputTax(), "按已验真发票可抵扣税额汇总"),
                new TaxReturnVO.Row("本期应纳税额", vo.getTaxPayable(), "销项税额 - 进项税额")
        ));
        vo.setRiskHints(Arrays.asList(
                "申报表为草稿状态，提交前需复核发票勾选、红冲作废和留抵余额。",
                "Demo 未接电子税务局，正式环境应增加申报前校验和回执归档。"
        ));
        return Result.success(vo);
    }

    private String normalizeTaxType(String taxType) {
        if (taxType == null || taxType.trim().length() == 0) {
            return "VAT";
        }
        String normalized = taxType.trim().toUpperCase();
        if ("增值税".equals(taxType) || "VAT".equals(normalized)) {
            return "VAT";
        }
        if ("企业所得税".equals(taxType) || "CIT".equals(normalized)) {
            return "CIT";
        }
        return normalized;
    }

    private BigDecimal defaultRate(String taxType) {
        return "CIT".equals(taxType) ? CIT_DEFAULT_RATE : VAT_DEFAULT_RATE;
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
        }
        return rate;
    }

    private String defaultPeriod(String period) {
        return period == null || period.trim().length() == 0 ? "2026-05" : period;
    }
}
