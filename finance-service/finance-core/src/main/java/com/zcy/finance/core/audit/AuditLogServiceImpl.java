package com.zcy.finance.core.audit;

import com.zcy.finance.api.AuditLogService;
import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.AuditLogDTO;
import com.zcy.finance.api.dto.LogQueryDTO;
import com.zcy.finance.api.vo.AuditLogVO;
import com.zcy.finance.infra.audit.AuditLogRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@DubboService(interfaceClass = AuditLogService.class)
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogServiceImpl(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<Void> logInvocation(AuditLogDTO dto) {
        AuditLogVO vo = new AuditLogVO();
        vo.setLogId("AUDIT-" + compactNow() + "-" + shortUuid());
        vo.setTraceId(hasText(dto == null ? null : dto.getTraceId()) ? dto.getTraceId() : vo.getLogId());
        vo.setSessionId(value(dto == null ? null : dto.getSessionId(), "unknown-session"));
        vo.setChannel(value(dto == null ? null : dto.getChannel(), "web"));
        vo.setCorpId(dto == null ? null : dto.getCorpId());
        vo.setUserId(dto == null ? null : dto.getUserId());
        vo.setUserName(dto == null ? null : dto.getUserName());
        vo.setRequestText(dto == null ? null : dto.getRequestText());
        vo.setToolName(value(dto == null ? null : dto.getToolName(), "unknown_tool"));
        vo.setServiceName(value(dto == null ? null : dto.getServiceName(), "finance"));
        vo.setRequestArgs(dto == null ? null : dto.getRequestArgs());
        vo.setResponseSummary(dto == null ? null : dto.getResponseSummary());
        vo.setStatus(value(dto == null ? null : dto.getStatus(), "success").toLowerCase());
        vo.setErrorMessage(dto == null ? null : dto.getErrorMessage());
        vo.setDurationMs(dto == null || dto.getDurationMs() == null ? Long.valueOf(0L) : dto.getDurationMs());
        vo.setOccurredAt(value(dto == null ? null : dto.getOccurredAt(), now()));
        repository.insert(vo);
        return Result.success((Void) null);
    }

    @Override
    public Result<PageResult<AuditLogVO>> queryLogs(LogQueryDTO dto) {
        int pageNo = dto != null && dto.getPageNo() != null && dto.getPageNo().intValue() > 0 ? dto.getPageNo().intValue() : 1;
        int pageSize = dto != null && dto.getPageSize() != null && dto.getPageSize().intValue() > 0 ? Math.min(dto.getPageSize().intValue(), 50) : 10;
        List<AuditLogVO> filtered = new ArrayList<AuditLogVO>();
        for (AuditLogVO log : repository.findAll()) {
            if (!matches(log.getSessionId(), dto == null ? null : dto.getSessionId())) {
                continue;
            }
            if (!matches(log.getToolName(), dto == null ? null : dto.getToolName())) {
                continue;
            }
            if (!matches(log.getStatus(), dto == null ? null : dto.getStatus())) {
                continue;
            }
            if (!matches(log.getChannel(), dto == null ? null : dto.getChannel())) {
                continue;
            }
            if (!matches(log.getUserId(), dto == null ? null : dto.getUserId())) {
                continue;
            }
            filtered.add(log);
        }
        int from = Math.min((pageNo - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        return Result.success(PageResult.of(new ArrayList<AuditLogVO>(filtered.subList(from, to)), filtered.size(), pageNo, pageSize));
    }

    private static boolean matches(String actual, String expected) {
        return !hasText(expected) || "all".equalsIgnoreCase(expected) || (actual != null && actual.equalsIgnoreCase(expected));
    }

    private static String value(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private static String compactNow() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
