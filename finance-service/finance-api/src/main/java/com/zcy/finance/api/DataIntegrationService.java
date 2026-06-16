package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.DataIntegrationQueryDTO;
import com.zcy.finance.api.vo.DataIntegrationVO;

public interface DataIntegrationService {

    Result<DataIntegrationVO> queryStatus(DataIntegrationQueryDTO dto);
}
