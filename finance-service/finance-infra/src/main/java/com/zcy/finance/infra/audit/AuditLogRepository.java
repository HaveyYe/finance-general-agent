package com.zcy.finance.infra.audit;

import com.zcy.finance.api.vo.AuditLogVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(AuditLogVO log) {
        jdbcTemplate.update(
                "INSERT INTO t_audit_log (log_id, trace_id, session_id, channel, corp_id, user_id, user_name, "
                        + "request_text, tool_name, service_name, request_args, response_summary, status, error_message, "
                        + "duration_ms, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                log.getLogId(), log.getTraceId(), log.getSessionId(), log.getChannel(), log.getCorpId(),
                log.getUserId(), log.getUserName(), log.getRequestText(), log.getToolName(), log.getServiceName(),
                log.getRequestArgs(), log.getResponseSummary(), log.getStatus(), log.getErrorMessage(),
                log.getDurationMs(), Timestamp.valueOf(log.getOccurredAt())
        );
    }

    public List<AuditLogVO> findAll() {
        return jdbcTemplate.query("SELECT * FROM t_audit_log ORDER BY occurred_at DESC", new AuditLogRowMapper());
    }

    private static class AuditLogRowMapper implements RowMapper<AuditLogVO> {
        @Override
        public AuditLogVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            AuditLogVO vo = new AuditLogVO();
            vo.setLogId(rs.getString("log_id"));
            vo.setTraceId(rs.getString("trace_id"));
            vo.setSessionId(rs.getString("session_id"));
            vo.setChannel(rs.getString("channel"));
            vo.setCorpId(rs.getString("corp_id"));
            vo.setUserId(rs.getString("user_id"));
            vo.setUserName(rs.getString("user_name"));
            vo.setRequestText(rs.getString("request_text"));
            vo.setToolName(rs.getString("tool_name"));
            vo.setServiceName(rs.getString("service_name"));
            vo.setRequestArgs(rs.getString("request_args"));
            vo.setResponseSummary(rs.getString("response_summary"));
            vo.setStatus(rs.getString("status"));
            vo.setErrorMessage(rs.getString("error_message"));
            vo.setDurationMs(Long.valueOf(rs.getLong("duration_ms")));
            Timestamp occurredAt = rs.getTimestamp("occurred_at");
            vo.setOccurredAt(occurredAt == null ? null : occurredAt.toString().substring(0, 19));
            return vo;
        }
    }
}
