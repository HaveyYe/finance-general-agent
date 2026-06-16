package com.zcy.finance.api;

import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.AccountMappingRecommendDTO;
import com.zcy.finance.api.vo.AccountMappingRecommendationVO;

public interface AccountMappingService {

    Result<AccountMappingRecommendationVO> recommend(AccountMappingRecommendDTO dto);
}
