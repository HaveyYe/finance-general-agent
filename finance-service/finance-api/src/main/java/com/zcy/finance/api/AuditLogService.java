package com.zcy.finance.api;

import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.AuditLogDTO;
import com.zcy.finance.api.dto.LogQueryDTO;
import com.zcy.finance.api.vo.AuditLogVO;

public interface AuditLogService {

    Result<Void> logInvocation(AuditLogDTO dto);

    Result<PageResult<AuditLogVO>> queryLogs(LogQueryDTO dto);
}
