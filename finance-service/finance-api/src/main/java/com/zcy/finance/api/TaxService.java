package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.TaxCalculateDTO;
import com.zcy.finance.api.dto.TaxReturnGenerateDTO;
import com.zcy.finance.api.vo.TaxCalculationVO;
import com.zcy.finance.api.vo.TaxReturnVO;

public interface TaxService {

    Result<TaxCalculationVO> calculateTax(TaxCalculateDTO dto);

    Result<TaxReturnVO> generateTaxReturn(TaxReturnGenerateDTO dto);
}
