package com.zcy.finance.core.arap;

import com.zcy.finance.api.ArApService;
import com.zcy.finance.api.common.PageResult;
import com.zcy.finance.api.common.Result;
import com.zcy.finance.api.dto.AgingQueryDTO;
import com.zcy.finance.api.vo.AgingVO;
import com.zcy.finance.api.vo.CollectionAdviceVO;
import com.zcy.finance.api.vo.ReconciliationVO;
import com.zcy.finance.infra.arap.ArApRepository;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DubboService(interfaceClass = ArApService.class)
public class ArApServiceImpl implements ArApService {

    private final ArApRepository repository;

    public ArApServiceImpl(ArApRepository repository) {
        this.repository = repository;
    }

    @Override
    public Result<PageResult<AgingVO>> queryAging(AgingQueryDTO dto) {
        if (dto == null || isBlank(dto.getBaseDate())) {
            return Result.failure("账龄基准日期不能为空");
        }
        String type = defaultText(dto.getType(), "AR").toUpperCase();
        if (!"AR".equals(type) && !"AP".equals(type)) {
            return Result.failure("账龄类型必须为 AR 或 AP");
        }

        List<AgingVO> rows = repository.queryAging(type, dto.getBaseDate());
        List<AgingVO> filtered = new ArrayList<AgingVO>();
        for (AgingVO row : rows) {
            if (!isBlank(dto.getPartnerId()) && !row.getPartnerId().equalsIgnoreCase(dto.getPartnerId())) {
                continue;
            }
            if (!isBlank(dto.getPartnerName()) && !row.getPartnerName().contains(dto.getPartnerName())) {
                continue;
            }
            filtered.add(row);
        }
        int pageNo = dto.getPageNo() == null || dto.getPageNo() < 1 ? 1 : dto.getPageNo();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 20 : dto.getPageSize();
        return Result.success(PageResult.of(filtered, filtered.size(), pageNo, pageSize));
    }

    @Override
    public Result<List<CollectionAdviceVO>> suggestCollection(String customerId) {
        if (isBlank(customerId)) {
            return Result.failure("客户ID不能为空");
        }
        String customerName = customerName(customerId);
        return Result.success(Arrays.asList(
                advice("COL-ADV-001", customerId, customerName, "128900.00", 96, "HIGH", "电话+正式催收函", "应收会计", "2026-06-05",
                        "贵司逾期款项已超过90天，请于3个工作日内确认付款计划；如有差异请同步对账明细。"),
                advice("COL-ADV-002", customerId, customerName, "56000.00", 48, "MEDIUM", "邮件+销售协同", "销售负责人", "2026-06-10",
                        "请协助确认本期到期款项安排，并回复预计付款日期，我们将据此更新回款计划。"),
                advice("COL-ADV-003", customerId, customerName, "34000.00", 12, "LOW", "企业微信提醒", "应收会计", "2026-06-15",
                        "温馨提醒：贵司近期有款项到期，请核对账单并按合同约定安排付款。")
        ));
    }

    @Override
    public Result<ReconciliationVO> reconcile(String partnerId, String period) {
        if (isBlank(partnerId) || isBlank(period)) {
            return Result.failure("往来单位ID和期间不能为空");
        }
        boolean supplier = partnerId.toUpperCase().startsWith("S");
        ReconciliationVO vo = new ReconciliationVO();
        vo.setReconciliationNo("REC-" + period.replace("-", "") + "-" + partnerId.toUpperCase());
        vo.setPartnerId(partnerId.toUpperCase());
        vo.setPartnerName(supplier ? "上海数科服务有限公司" : customerName(partnerId));
        vo.setPartnerType(supplier ? "SUPPLIER" : "CUSTOMER");
        vo.setPeriod(period);
        vo.setInternalBalance(supplier ? new BigDecimal("126000.00") : new BigDecimal("218900.00"));
        vo.setPartnerBalance(supplier ? new BigDecimal("117400.00") : new BigDecimal("204300.00"));
        vo.setDifferenceAmount(vo.getInternalBalance().subtract(vo.getPartnerBalance()).abs());
        vo.setStatus("DIFFERENCE");
        vo.setConclusion("存在未达收付款和发票时间差，完成差异处理前不建议确认本期往来余额。");
        vo.setDifferenceItems(Arrays.asList(
                new ReconciliationVO.DifferenceItem("BT-" + period.replace("-", "") + "-003", "未达收付款", new BigDecimal("8600.00"), "我方已入账，对方尚未确认。", "核对银行回单并发送对方财务确认。"),
                new ReconciliationVO.DifferenceItem("INV-" + period.replace("-", "") + "-003", "发票时间差", new BigDecimal("6000.00"), "发票已开具但对方尚未入账。", "补发发票和对账明细，等待对方回函。")
        ));
        return Result.success(vo);
    }

    private List<AgingVO> receivableAging(String baseDate) {
        return Arrays.asList(
                aging("C1001", "浙江智造设备有限公司", "AR", baseDate, "386500.00", "168000.00", "79400.00", "10200.00", "128900.00", 96, "HIGH", "优先催收90天以上款项并暂停新增授信。"),
                aging("C1002", "杭州云启零售有限公司", "AR", baseDate, "268000.00", "178000.00", "56000.00", "34000.00", "0.00", 72, "MEDIUM", "由销售负责人协同确认付款计划。"),
                aging("C1003", "宁波远洋贸易有限公司", "AR", baseDate, "196000.00", "162000.00", "34000.00", "0.00", "0.00", 42, "LOW", "发送到期提醒并持续跟踪。")
        );
    }

    private List<AgingVO> payableAging(String baseDate) {
        return Arrays.asList(
                aging("S2001", "上海数科服务有限公司", "AP", baseDate, "186000.00", "98000.00", "42000.00", "46000.00", "0.00", 76, "MEDIUM", "结合现金头寸安排账期内付款。"),
                aging("S2002", "深圳蓝海咨询有限公司", "AP", baseDate, "128000.00", "86000.00", "42000.00", "0.00", "0.00", 51, "LOW", "校验发票验真状态后按期付款。"),
                aging("S2003", "杭州差旅服务有限公司", "AP", baseDate, "76000.00", "76000.00", "0.00", "0.00", "0.00", 22, "LOW", "可合并同供应商付款以减少手续费。")
        );
    }

    private AgingVO aging(String partnerId, String partnerName, String type, String baseDate, String total,
                          String within30, String days31To60, String days61To90, String over90,
                          int oldestDays, String riskLevel, String suggestion) {
        AgingVO vo = new AgingVO();
        vo.setPartnerId(partnerId);
        vo.setPartnerName(partnerName);
        vo.setType(type);
        vo.setBaseDate(baseDate);
        vo.setTotalAmount(new BigDecimal(total));
        vo.setWithin30Days(new BigDecimal(within30));
        vo.setDays31To60(new BigDecimal(days31To60));
        vo.setDays61To90(new BigDecimal(days61To90));
        vo.setOver90Days(new BigDecimal(over90));
        vo.setOverdueAmount(new BigDecimal(days31To60).add(new BigDecimal(days61To90)).add(new BigDecimal(over90)));
        vo.setOldestOverdueDays(oldestDays);
        vo.setRiskLevel(riskLevel);
        vo.setSuggestion(suggestion);
        return vo;
    }

    private CollectionAdviceVO advice(String adviceNo, String customerId, String customerName, String amount,
                                      int overdueDays, String priority, String channel, String owner, String dueDate,
                                      String wording) {
        CollectionAdviceVO vo = new CollectionAdviceVO();
        vo.setAdviceNo(adviceNo);
        vo.setCustomerId(customerId.toUpperCase());
        vo.setCustomerName(customerName);
        vo.setAmount(new BigDecimal(amount));
        vo.setOverdueDays(overdueDays);
        vo.setPriority(priority);
        vo.setChannel(channel);
        vo.setOwner(owner);
        vo.setDueDate(dueDate);
        vo.setWording(wording);
        return vo;
    }

    private String customerName(String customerId) {
        if ("C1002".equalsIgnoreCase(customerId)) {
            return "杭州云启零售有限公司";
        }
        if ("C1003".equalsIgnoreCase(customerId)) {
            return "宁波远洋贸易有限公司";
        }
        return "浙江智造设备有限公司";
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
