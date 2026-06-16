package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.ContractAssetQueryDTO;
import com.zcy.finance.api.vo.ContractAssetVO;

public interface ContractAssetService {

    Result<ContractAssetVO> queryOverview(ContractAssetQueryDTO dto);
}
