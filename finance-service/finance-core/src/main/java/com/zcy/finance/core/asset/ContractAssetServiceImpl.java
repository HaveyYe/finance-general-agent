package com.zcy.finance.core.asset;

import com.zcy.finance.api.ContractAssetService;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.ContractAssetQueryDTO;
import com.zcy.finance.api.vo.ContractAssetVO;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = ContractAssetService.class)
public class ContractAssetServiceImpl implements ContractAssetService {

    @Override
    public Result<ContractAssetVO> queryOverview(ContractAssetQueryDTO dto) {
        String period = dto != null && hasText(dto.getPeriod()) ? dto.getPeriod() : "2026-06";
        String scope = dto != null && hasText(dto.getScope()) ? dto.getScope() : "all";
        String department = dto != null && hasText(dto.getDepartment()) ? dto.getDepartment() : "all";
        int reminderDays = dto != null && dto.getReminderDays() != null ? dto.getReminderDays().intValue() : 30;

        List<ContractAssetVO.ContractSummary> contracts = contracts();
        List<ContractAssetVO.PaymentMilestone> milestones = milestones();
        List<ContractAssetVO.DepreciationRow> depreciationRows = filterByDepartment(depreciationRows(), department);
        List<ContractAssetVO.InventoryException> inventoryExceptions = inventoryExceptions();
        List<ContractAssetVO.IntangibleReminder> intangibleReminders = intangibleReminders();

        ContractAssetVO vo = new ContractAssetVO();
        vo.setPeriod(period);
        vo.setScope(scope);
        vo.setContracts(include(scope, "contract") ? contracts : new ArrayList<ContractAssetVO.ContractSummary>());
        vo.setMilestones(include(scope, "contract") ? milestones : new ArrayList<ContractAssetVO.PaymentMilestone>());
        vo.setDepreciationRows(include(scope, "asset") ? depreciationRows : new ArrayList<ContractAssetVO.DepreciationRow>());
        vo.setInventoryExceptions(include(scope, "asset") ? inventoryExceptions : new ArrayList<ContractAssetVO.InventoryException>());
        vo.setIntangibleReminders(include(scope, "asset") ? intangibleReminders : new ArrayList<ContractAssetVO.IntangibleReminder>());
        vo.setActiveContractCount(contracts.size());
        vo.setDueMilestoneCount(countDue(milestones));
        vo.setOverdueMilestoneCount(countOverdue(milestones));
        vo.setAssetCount(depreciationRows.size() + intangibleReminders.size());
        vo.setContractAmount(sumContracts(contracts));
        vo.setMonthlyDepreciation(sumDepreciation(depreciationRows));
        vo.setIntangibleAmortization(sumAmortization(intangibleReminders));
        vo.setAdvices(advices(vo, reminderDays));
        return Result.success(vo);
    }

    private List<ContractAssetVO.ContractSummary> contracts() {
        return Arrays.asList(
                new ContractAssetVO.ContractSummary("CT-202605-001", "浙江智造设备有限公司", "销售合同", new BigDecimal("580000.00"), "2026-05-01", "2026-11-30", "ACTIVE", "96%", "第二期回款节点临近，需提前确认验收资料。"),
                new ContractAssetVO.ContractSummary("CT-202605-002", "上海数科服务有限公司", "软件服务合同", new BigDecimal("42000.00"), "2026-05-15", "2027-05-14", "ACTIVE", "91%", "自动续约条款置信度偏低，建议法务复核。"),
                new ContractAssetVO.ContractSummary("CT-202604-006", "深圳蓝海咨询有限公司", "咨询采购合同", new BigDecimal("128000.00"), "2026-04-01", "2026-07-31", "REVIEW", "84%", "付款条件与发票复核状态存在联动风险。")
        );
    }

    private List<ContractAssetVO.PaymentMilestone> milestones() {
        return Arrays.asList(
                new ContractAssetVO.PaymentMilestone("MS-202606-001", "CT-202605-001", "应收", "2026-06-10", new BigDecimal("174000.00"), "应收会计", "DUE_SOON", "YELLOW", "发送验收资料确认提醒，同步销售负责人。"),
                new ContractAssetVO.PaymentMilestone("MS-202606-002", "CT-202605-002", "应付", "2026-06-15", new BigDecimal("42000.00"), "应付会计", "PENDING_APPROVAL", "YELLOW", "付款前确认发票已验真并完成凭证复核。"),
                new ContractAssetVO.PaymentMilestone("MS-202605-009", "CT-202604-006", "应付", "2026-05-25", new BigDecimal("51200.00"), "采购负责人", "OVERDUE", "RED", "节点已逾期，升级采购主管并冻结自动付款。")
        );
    }

    private List<ContractAssetVO.DepreciationRow> depreciationRows() {
        return Arrays.asList(
                new ContractAssetVO.DepreciationRow("FA-202401-001", "生产检测设备", "机器设备", "制造部", "年限平均法", new BigDecimal("360000.00"), new BigDecimal("78000.00"), new BigDecimal("4750.00"), new BigDecimal("282000.00"), "借：制造费用 4750，贷：累计折旧 4750"),
                new ContractAssetVO.DepreciationRow("FA-202503-008", "研发服务器集群", "电子设备", "研发部", "双倍余额递减法", new BigDecimal("240000.00"), new BigDecimal("96000.00"), new BigDecimal("8000.00"), new BigDecimal("144000.00"), "借：研发费用 8000，贷：累计折旧 8000"),
                new ContractAssetVO.DepreciationRow("FA-202506-002", "办公家具批次", "办公设备", "行政部", "年限平均法", new BigDecimal("86000.00"), new BigDecimal("0.00"), new BigDecimal("1361.67"), new BigDecimal("86000.00"), "新购资产下月起折旧，本期仅生成卡片。")
        );
    }

    private List<ContractAssetVO.InventoryException> inventoryExceptions() {
        return Arrays.asList(
                new ContractAssetVO.InventoryException("INV-TASK-202606", "FA-202503-008", "研发服务器集群", "研发机房A区", "在用", "在用-位置变更", "LOCATION_MISMATCH", "资产管理员", "更新资产地图并补充调拨单。"),
                new ContractAssetVO.InventoryException("INV-TASK-202606", "FA-202402-015", "移动扫码终端", "仓库", "在用", "未盘到", "LOSS_RISK", "行政主管", "发起盘亏核查流程，3日内提交说明。")
        );
    }

    private List<ContractAssetVO.IntangibleReminder> intangibleReminders() {
        return Arrays.asList(
                new ContractAssetVO.IntangibleReminder("IA-202401-003", "CRM软件许可", "软件许可", new BigDecimal("120000.00"), new BigDecimal("3333.33"), "2026-06-30", "30天内到期，需确认续费或停用。"),
                new ContractAssetVO.IntangibleReminder("IA-202503-001", "数据治理平台实施费", "长期待摊费用", new BigDecimal("180000.00"), new BigDecimal("5000.00"), "2028-02-28", "按直线法继续摊销，本期生成摊销凭证。")
        );
    }

    private List<ContractAssetVO.DepreciationRow> filterByDepartment(List<ContractAssetVO.DepreciationRow> rows, String department) {
        if (!hasText(department) || "all".equalsIgnoreCase(department)) {
            return rows;
        }
        List<ContractAssetVO.DepreciationRow> filtered = new ArrayList<ContractAssetVO.DepreciationRow>();
        for (ContractAssetVO.DepreciationRow row : rows) {
            if (department.equals(row.getDepartment())) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    private boolean include(String scope, String area) {
        return !hasText(scope) || "all".equalsIgnoreCase(scope) || area.equalsIgnoreCase(scope);
    }

    private int countDue(List<ContractAssetVO.PaymentMilestone> rows) {
        int count = 0;
        for (ContractAssetVO.PaymentMilestone row : rows) {
            if ("DUE_SOON".equals(row.getStatus()) || "PENDING_APPROVAL".equals(row.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countOverdue(List<ContractAssetVO.PaymentMilestone> rows) {
        int count = 0;
        for (ContractAssetVO.PaymentMilestone row : rows) {
            if ("OVERDUE".equals(row.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal sumContracts(List<ContractAssetVO.ContractSummary> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (ContractAssetVO.ContractSummary row : rows) {
            total = total.add(row.getAmount());
        }
        return total;
    }

    private BigDecimal sumDepreciation(List<ContractAssetVO.DepreciationRow> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (ContractAssetVO.DepreciationRow row : rows) {
            total = total.add(row.getMonthlyDepreciation());
        }
        return total;
    }

    private BigDecimal sumAmortization(List<ContractAssetVO.IntangibleReminder> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (ContractAssetVO.IntangibleReminder row : rows) {
            total = total.add(row.getMonthlyAmortization());
        }
        return total;
    }

    private List<String> advices(ContractAssetVO vo, int reminderDays) {
        List<String> advices = new ArrayList<String>();
        advices.add("未来 " + reminderDays + " 天内存在 " + vo.getDueMilestoneCount() + " 个合同收付款节点，建议同步钉钉待办给责任人。 ");
        if (vo.getOverdueMilestoneCount() > 0) {
            advices.add("存在 " + vo.getOverdueMilestoneCount() + " 个逾期节点，应升级业务负责人并暂停自动付款。 ");
        }
        advices.add("本期固定资产折旧额 " + vo.getMonthlyDepreciation() + "，可推送凭证模块生成折旧凭证草稿。 ");
        advices.add("盘点异常 " + vo.getInventoryExceptions().size() + " 项，建议资产管理员完成调拨或盘亏核查。 ");
        advices.add("无形资产本期摊销 " + vo.getIntangibleAmortization() + "，到期许可需提前确认续费。 ");
        return advices;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
