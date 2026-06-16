package com.zcy.finance.core.integration;

import com.zcy.finance.api.DataIntegrationService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.DataIntegrationQueryDTO;
import com.zcy.finance.api.vo.DataIntegrationVO;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = DataIntegrationService.class)
public class DataIntegrationServiceImpl implements DataIntegrationService {

    @Override
    public Result<DataIntegrationVO> queryStatus(DataIntegrationQueryDTO dto) {
        String period = dto != null && hasText(dto.getPeriod()) ? dto.getPeriod() : "2026-06";
        String systemType = dto != null && hasText(dto.getSystemType()) ? dto.getSystemType() : "all";
        String syncMode = dto != null && hasText(dto.getSyncMode()) ? dto.getSyncMode() : "all";
        boolean includeQuality = dto == null || dto.getIncludeQualityDetails() == null || dto.getIncludeQualityDetails().booleanValue();

        List<DataIntegrationVO.ConnectorStatus> connectors = filterConnectors(connectors(), systemType, syncMode);
        List<DataIntegrationVO.EtlJobStatus> etlJobs = etlJobs(systemType);
        List<DataIntegrationVO.QualityMetric> qualityMetrics = includeQuality ? qualityMetrics() : new ArrayList<DataIntegrationVO.QualityMetric>();
        List<DataIntegrationVO.MasterDataMapping> mappings = masterDataMappings();
        List<DataIntegrationVO.RetryTask> retryTasks = retryTasks(connectors);

        DataIntegrationVO vo = new DataIntegrationVO();
        vo.setPeriod(period);
        vo.setSystemType(systemType);
        vo.setConnectors(connectors);
        vo.setEtlJobs(etlJobs);
        vo.setQualityMetrics(qualityMetrics);
        vo.setMasterDataMappings(mappings);
        vo.setRetryTasks(retryTasks);
        fillSummary(vo, connectors, qualityMetrics);
        vo.setAlerts(alerts(vo));
        vo.setAdvices(advices(vo));
        return Result.success(vo);
    }

    private List<DataIntegrationVO.ConnectorStatus> connectors() {
        return Arrays.asList(
                new DataIntegrationVO.ConnectorStatus("ERP-SAP-GL", "erp", "SAP S/4HANA", "RFC/BAPI", "INCREMENTAL", "2026-06-03 08:30:00", "HEALTHY", 12860, 0, "420ms", "总账凭证、科目余额同步正常。"),
                new DataIntegrationVO.ConnectorStatus("ERP-YONYOU-APAR", "erp", "用友 YonBIP", "OpenAPI", "INCREMENTAL", "2026-06-03 08:28:00", "WARNING", 3860, 18, "980ms", "供应商主数据存在税号冲突，已进入映射复核。"),
                new DataIntegrationVO.ConnectorStatus("BANK-CMB-DIRECT", "bank", "招商银行银企直连", "CERT_API", "REALTIME", "2026-06-03 08:31:10", "HEALTHY", 582, 0, "260ms", "账户余额、交易明细和电子回单获取正常。"),
                new DataIntegrationVO.ConnectorStatus("BANK-ICBC-PAY", "bank", "工商银行付款指令", "CERT_API", "EVENT", "2026-06-03 08:18:44", "WARNING", 26, 1, "610ms", "1 笔批量付款指令等待银行受理回执。"),
                new DataIntegrationVO.ConnectorStatus("TAX-DIGITAL", "tax", "电子税务局数字账户", "REST", "SCHEDULED", "2026-06-03 07:50:00", "HEALTHY", 1240, 0, "730ms", "进销项发票与申报预填数据同步正常。"),
                new DataIntegrationVO.ConnectorStatus("OA-DINGTALK", "business", "钉钉 OA 审批", "REST", "EVENT", "2026-06-03 08:32:00", "HEALTHY", 318, 0, "180ms", "费用申请、合同审批单同步正常。"),
                new DataIntegrationVO.ConnectorStatus("CRM-SALES", "business", "CRM 客户合同", "REST", "INCREMENTAL", "2026-06-03 08:10:00", "HEALTHY", 965, 0, "350ms", "客户、合同签约和回款计划同步正常。"),
                new DataIntegrationVO.ConnectorStatus("HR-HCM", "business", "HR 人力成本", "DB", "SCHEDULED", "2026-06-03 06:00:00", "FAILED", 450, 36, "timeout", "薪资社保明细缺少成本中心，暂停下游人力成本分摊。"),
                new DataIntegrationVO.ConnectorStatus("PURCHASE-SRM", "business", "采购 SRM", "REST", "INCREMENTAL", "2026-06-03 08:20:00", "HEALTHY", 1288, 0, "410ms", "采购订单、入库单、验收单同步正常。")
        );
    }

    private List<DataIntegrationVO.ConnectorStatus> filterConnectors(List<DataIntegrationVO.ConnectorStatus> rows, String systemType, String syncMode) {
        List<DataIntegrationVO.ConnectorStatus> filtered = new ArrayList<DataIntegrationVO.ConnectorStatus>();
        for (DataIntegrationVO.ConnectorStatus row : rows) {
            if (hasText(systemType) && !"all".equalsIgnoreCase(systemType) && !systemType.equalsIgnoreCase(row.getSystemType())) {
                continue;
            }
            if (hasText(syncMode) && !"all".equalsIgnoreCase(syncMode) && !syncMode.equalsIgnoreCase(row.getSyncMode())) {
                continue;
            }
            filtered.add(row);
        }
        return filtered;
    }

    private List<DataIntegrationVO.EtlJobStatus> etlJobs(String systemType) {
        List<DataIntegrationVO.EtlJobStatus> rows = Arrays.asList(
                new DataIntegrationVO.EtlJobStatus("ETL-GL-DAILY", "总账凭证增量入湖", "SCHEDULED", "LOAD", "RUNNING", new BigDecimal("72.5"), "voucher:202606030830", "2026-06-03 09:00:00", "支持断点续传，当前加载 2026-06-03 08:30 后凭证。"),
                new DataIntegrationVO.EtlJobStatus("ETL-MDM-CLEAN", "客户供应商主数据清洗", "EVENT", "STANDARDIZE", "WARNING", new BigDecimal("88.0"), "party:taxNoConflict:18", "事件触发", "18 条供应商税号冲突等待人工确认。"),
                new DataIntegrationVO.EtlJobStatus("ETL-HR-COST", "人力成本归集", "SCHEDULED", "PAUSED", "BLOCKED", new BigDecimal("64.0"), "hrCost:costCenterMissing", "待修复后恢复", "数据质量低于阈值，已暂停下游分摊。")
        );
        if (!hasText(systemType) || "all".equalsIgnoreCase(systemType)) {
            return rows;
        }
        if ("business".equalsIgnoreCase(systemType)) {
            return rows;
        }
        if ("erp".equalsIgnoreCase(systemType)) {
            return Arrays.asList(rows.get(0), rows.get(1));
        }
        return new ArrayList<DataIntegrationVO.EtlJobStatus>();
    }

    private List<DataIntegrationVO.QualityMetric> qualityMetrics() {
        return Arrays.asList(
                new DataIntegrationVO.QualityMetric("完整性", new BigDecimal("92.4"), new BigDecimal("90.0"), "PASS", "HR 薪资明细缺少成本中心 36 条。", "补齐成本中心后恢复人力成本分摊。"),
                new DataIntegrationVO.QualityMetric("准确性", new BigDecimal("96.8"), new BigDecimal("95.0"), "PASS", "银行回单金额与流水金额一致。", "继续保持银企直连自动校验。"),
                new DataIntegrationVO.QualityMetric("一致性", new BigDecimal("89.5"), new BigDecimal("92.0"), "WARNING", "供应商税号与 ERP/OA 主数据存在 18 条冲突。", "按税号优先、名称模糊匹配二次确认。"),
                new DataIntegrationVO.QualityMetric("及时性", new BigDecimal("94.2"), new BigDecimal("90.0"), "PASS", "大部分连接器 15 分钟内完成同步。", "HR-HCM 失败需单独跟进。")
        );
    }

    private List<DataIntegrationVO.MasterDataMapping> masterDataMappings() {
        return Arrays.asList(
                new DataIntegrationVO.MasterDataMapping("科目", "SAP S/4HANA", 426, 426, 0, new BigDecimal("100.00"), "按统一科目表精确映射。"),
                new DataIntegrationVO.MasterDataMapping("客户", "CRM 客户合同", 965, 952, 13, new BigDecimal("98.65"), "税号精确匹配 + 名称相似度。"),
                new DataIntegrationVO.MasterDataMapping("供应商", "用友 YonBIP/OA", 688, 670, 18, new BigDecimal("97.38"), "税号冲突进入人工复核队列。"),
                new DataIntegrationVO.MasterDataMapping("成本中心", "HR HCM", 450, 414, 36, new BigDecimal("92.00"), "组织编码与财务成本中心对照。")
        );
    }

    private List<DataIntegrationVO.RetryTask> retryTasks(List<DataIntegrationVO.ConnectorStatus> connectors) {
        List<DataIntegrationVO.RetryTask> rows = new ArrayList<DataIntegrationVO.RetryTask>();
        for (DataIntegrationVO.ConnectorStatus connector : connectors) {
            if (connector.getFailedRecords() <= 0) {
                continue;
            }
            rows.add(new DataIntegrationVO.RetryTask(
                    "RETRY-" + connector.getConnectorId(),
                    connector.getConnectorId(),
                    "EXTRACT".equals(connector.getSyncMode()) ? "EXTRACT" : "STANDARDIZE",
                    connector.getMessage(),
                    "FAILED".equals(connector.getStatus()) ? 3 : 1,
                    "FAILED".equals(connector.getStatus()) ? "人工修复后重试" : "2026-06-03 08:45:00",
                    "数据集成运维"
            ));
        }
        return rows;
    }

    private void fillSummary(DataIntegrationVO vo, List<DataIntegrationVO.ConnectorStatus> connectors, List<DataIntegrationVO.QualityMetric> qualityMetrics) {
        int healthy = 0;
        int warning = 0;
        int failed = 0;
        long total = 0;
        long failedRecords = 0;
        for (DataIntegrationVO.ConnectorStatus connector : connectors) {
            if ("HEALTHY".equals(connector.getStatus())) {
                healthy++;
            } else if ("FAILED".equals(connector.getStatus())) {
                failed++;
            } else {
                warning++;
            }
            total += connector.getRecords();
            failedRecords += connector.getFailedRecords();
        }
        vo.setConnectorCount(connectors.size());
        vo.setHealthyConnectorCount(healthy);
        vo.setWarningConnectorCount(warning);
        vo.setFailedConnectorCount(failed);
        vo.setTotalRecords(total);
        vo.setFailedRecords(failedRecords);
        vo.setSuccessRecords(total - failedRecords);
        vo.setOverallQualityScore(overallQualityScore(qualityMetrics));
    }

    private BigDecimal overallQualityScore(List<DataIntegrationVO.QualityMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return new BigDecimal("0.00");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (DataIntegrationVO.QualityMetric metric : metrics) {
            total = total.add(metric.getScore());
        }
        return total.divide(new BigDecimal(metrics.size()), 2, RoundingMode.HALF_UP);
    }

    private List<String> alerts(DataIntegrationVO vo) {
        List<String> rows = new ArrayList<String>();
        if (vo.getFailedConnectorCount() > 0) {
            rows.add("存在 " + vo.getFailedConnectorCount() + " 个失败连接器，已暂停相关下游处理。 ");
        }
        if (vo.getOverallQualityScore().compareTo(new BigDecimal("92.00")) < 0) {
            rows.add("数据质量评分低于 92，需先完成主数据冲突处理。 ");
        }
        if (vo.getFailedRecords() > 0) {
            rows.add("存在 " + vo.getFailedRecords() + " 条失败记录，已生成断点续传和重试任务。 ");
        }
        return rows;
    }

    private List<String> advices(DataIntegrationVO vo) {
        List<String> rows = new ArrayList<String>();
        rows.add("优先修复 HR-HCM 成本中心缺失，恢复人力成本归集和费用分摊。 ");
        rows.add("供应商主数据采用税号精确匹配优先，18 条冲突需财务共享中心确认。 ");
        rows.add("银企直连付款指令建议按银行回执事件触发重试，避免重复支付。 ");
        rows.add("质量评分低于阈值时保持暂停下游报表和分析任务，防止脏数据扩散。 ");
        return rows;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
