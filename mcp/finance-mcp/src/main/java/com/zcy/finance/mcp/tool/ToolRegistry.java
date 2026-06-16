package com.zcy.finance.mcp.tool;

import com.zcy.finance.mcp.config.FinanceDubboProperties;
import com.zcy.finance.mcp.config.FinanceMcpProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final FinanceDubboProperties properties;
    private final FinanceMcpProperties mcpProperties;
    private final Map<String, ToolDefinition> tools = new LinkedHashMap<String, ToolDefinition>();

    public ToolRegistry(FinanceDubboProperties properties, FinanceMcpProperties mcpProperties) {
        this.properties = properties;
        this.mcpProperties = mcpProperties;
    }

    @PostConstruct
    public void init() {
        if (mcpProperties.isIncludeDefaultTools()) {
            registerContractTools();
            registerFinanceDemoTools();
        }
        registerConfiguredTools();
    }

    private void registerContractTools() {
        FinanceDubboProperties.Interfaces interfaces = properties.getInterfaces();

        register(poTool(
                "contract.loginByDingTalk",
                "钉钉免登 authCode 换取合同系统用户 token 或用户标识",
                interfaces.getFinanceContractMobile(),
                "login",
                "cn.gov.zcy.finance.contract.po.FinanceLoginPO",
                fields(field("authCode", "string", "钉钉免登 authCode")),
                required("authCode"),
                "none",
                false
        ));

        register(tokenPoTool(
                "contract.currentUser",
                "查询当前登录用户信息，token 从请求头注入",
                interfaces.getFinanceContractMobile(),
                "getCurrentUserInfo",
                "cn.gov.zcy.finance.contract.po.FinanceUserPO",
                fields(),
                Collections.<String>emptyList()
        ));

        register(tokenPoTool(
                "contract.page",
                "查询当前用户有权限访问的合同分页列表",
                interfaces.getFinanceContractMobile(),
                "queryPagedContract",
                "cn.gov.zcy.finance.contract.po.QueryContractPO",
                fields(
                        field("contractNo", "string", "合同编号，可选"),
                        field("contractName", "string", "合同名称关键词，可选"),
                        field("customerName", "string", "客户名称关键词，可选"),
                        field("status", "string", "合同状态，可选"),
                        field("pageNo", "integer", "页码，默认 1"),
                        field("pageSize", "integer", "每页条数，建议不超过 50")
                ),
                Collections.<String>emptyList()
        ));

        register(tokenPoTool(
                "contract.detail",
                "查询当前用户有权限访问的合同详情",
                interfaces.getFinanceContractMobile(),
                "getContractDetail",
                "cn.gov.zcy.finance.contract.po.FinanceContractPO",
                fields(field("contractNo", "string", "合同编号")),
                required("contractNo")
        ));

        register(tokenPoTool(
                "contract.statusList",
                "查询合同状态字典",
                interfaces.getFinanceContractMobile(),
                "getContractStatusList",
                "cn.gov.zcy.finance.contract.po.FinanceContractPO",
                fields(field("contractNo", "string", "合同编号，可选")),
                Collections.<String>emptyList()
        ));

        register(tokenPoTool(
                "contract.workflowKey",
                "查询合同当前工作流 key",
                interfaces.getFinanceContractMobile(),
                "getCurrentWorkflowKey",
                "cn.gov.zcy.finance.contract.po.FinanceContractPO",
                fields(field("contractNo", "string", "合同编号")),
                required("contractNo")
        ));

        register(tokenPoTool(
                "contract.attachments",
                "查询合同附件、预览地址和下载地址",
                interfaces.getFinanceContractMobile(),
                "listAttachments",
                "cn.gov.zcy.finance.contract.po.QueryAttachmentPO",
                fields(
                        field("contractNo", "string", "合同编号"),
                        field("type", "integer", "附件类型"),
                        field("pageNo", "integer", "页码，默认 1"),
                        field("pageSize", "integer", "每页条数，建议不超过 50")
                ),
                required("contractNo", "type")
        ));

        register(tokenPoTool(
                "contract.receiptAttachments",
                "查询单据附件和下载地址",
                interfaces.getFinanceContractMobile(),
                "receiptListAttachments",
                "cn.gov.zcy.finance.contract.po.QueryReceiptAttachmentPO",
                fields(
                        field("receiptNo", "string", "单据编号"),
                        field("pageNo", "integer", "页码，默认 1"),
                        field("pageSize", "integer", "每页条数，建议不超过 50")
                ),
                required("receiptNo")
        ));

        register(contractPrecheckStringTool(
                "contract.invoices",
                "查询合同开票记录，调用前会先用合同详情校验当前用户权限",
                interfaces.getFinanceContract(),
                "getContractInvoices"
        ));

        register(contractPrecheckStringTool(
                "contract.payment",
                "查询合同回款信息，调用前会先用合同详情校验当前用户权限",
                interfaces.getFinanceContract(),
                "getContractPayment"
        ));

        register(new ToolDefinition(
                "receipt.page",
                "查询单据分页并返回详情 URL，DigitUser 由 MCP 根据请求头构造",
                objectSchema(fields(
                        field("receiptNo", "string", "单据编号，可选"),
                        field("contractNo", "string", "合同编号，可选"),
                        field("customerName", "string", "客户名称关键词，可选"),
                        field("pageNo", "integer", "页码，默认 1"),
                        field("pageSize", "integer", "每页条数，建议不超过 50")
                ), Collections.<String>emptyList()),
                new DubboMapping(
                        interfaces.getReceipt(),
                        "queryPageReceipt",
                        Collections.singletonList("cn.gov.zcy.finance.receipt.po.ApiQueryReceiptPO"),
                        new ConfigurableArgumentMapper("object", null, null)
                ),
                "read",
                "operator",
                false,
                false,
                null,
                "contractNo",
                false,
                true
        ));

        register(stringTool(
                "common.dictionary",
                "查询合同系统数据字典",
                interfaces.getContractCommon(),
                "getDictionaryList",
                "typeCode",
                "字典类型编码"
        ));

        register(noArgTool(
                "common.departmentTree",
                "查询部门树",
                interfaces.getFinanceDepartment(),
                "getDepartmentTree"
        ));

        register(noArgTool(
                "common.bizProductInfo",
                "查询业务产品线全量信息",
                interfaces.getContractProdLine(),
                "getBizProductInfo"
        ));

        register(poTool(
                "product.page",
                "查询已上架产品分页列表",
                interfaces.getFinanceProduct(),
                "getProductList",
                "cn.gov.zcy.finance.contract.po.FinanceProductListPO",
                fields(
                        field("name", "string", "产品名称关键词，可选"),
                        field("code", "string", "产品编码，可选"),
                        field("pageNo", "integer", "页码，默认 1"),
                        field("pageSize", "integer", "每页条数，最大 500")
                ),
                Collections.<String>emptyList(),
                "none",
                false
        ));

        register(stringTool(
                "product.detail",
                "按产品编码查询产品详情",
                interfaces.getFinanceProduct(),
                "getProductDetailById",
                "code",
                "产品编码"
        ));

        register(poTool(
                "project.page",
                "查询项目库分页",
                interfaces.getProjectLibrary(),
                "paging",
                "cn.gov.zcy.finance.contract.po.QueryCentralProjectPO",
                fields(
                        field("projectCode", "string", "项目编号，可选"),
                        field("projectName", "string", "项目名称关键词，可选"),
                        field("pageNo", "integer", "页码，默认 1"),
                        field("pageSize", "integer", "每页条数，建议不超过 50")
                ),
                Collections.<String>emptyList(),
                "none",
                false
        ));

        register(poTool(
                "project.digitPage",
                "查询数字化项目库分页，至少提供项目、客户或合同相关条件之一",
                interfaces.getProjectLibrary(),
                "pagingDigitProject",
                "cn.gov.zcy.finance.contract.po.QueryDigitProjectPO",
                fields(
                        field("projectCode", "string", "项目编号，可选"),
                        field("projectName", "string", "项目名称关键词，可选"),
                        field("customerId", "integer", "客户 ID，可选"),
                        field("customerName", "string", "客户名称关键词，可选"),
                        field("contractNo", "string", "合同编号，可选"),
                        field("contractName", "string", "合同名称关键词，可选"),
                        field("pageNo", "integer", "页码，默认 1"),
                        field("pageSize", "integer", "每页条数，建议不超过 50")
                ),
                Collections.<String>emptyList(),
                "none",
                false
        ));

        register(new ToolDefinition(
                "bankTransaction.preCheck",
                "预核销银行流水。高风险写工具，默认要求写开关、管理员白名单和 confirmToken。",
                objectSchema(fields(
                        field("skipAmountCheck", "boolean", "是否跳过金额校验"),
                        field("amount", "integer", "收款流水金额，单位分"),
                        arrayField("bankTransactions", "银行流水数组"),
                        field("confirmToken", "string", "写操作二次确认口令"),
                        field("idempotencyKey", "string", "幂等键，建议传入")
                ), required("skipAmountCheck", "amount", "bankTransactions", "confirmToken")),
                new DubboMapping(
                        interfaces.getBankTransaction(),
                        "preCheck",
                        Collections.singletonList("cn.gov.zcy.finance.contract.po.BankTransactionPreCheckPO"),
                        new ConfigurableArgumentMapper("object", null, null)
                ),
                "write",
                "admin",
                true,
                true,
                null,
                "contractNo",
                false,
                false
        ));
    }

    private void registerFinanceDemoTools() {
        FinanceDubboProperties.Interfaces interfaces = properties.getInterfaces();

        register(financeDtoTool(
                "create_voucher",
                "创建记账凭证，校验借贷平衡并返回凭证号和审核提示",
                interfaces.getVoucher(),
                "createVoucher",
                "com.zcy.finance.api.dto.VoucherCreateDTO",
                fields(
                        field("voucherDate", "string", "凭证日期，格式 YYYY-MM-DD"),
                        field("summary", "string", "凭证摘要"),
                        arrayField("entries", "借贷分录数组")
                ),
                required("voucherDate", "summary", "entries"),
                "write"
        ));

        register(financeFieldTool(
                "get_voucher",
                "按凭证号查询凭证详情",
                interfaces.getVoucher(),
                "getVoucher",
                "voucherNo",
                "凭证号"
        ));

        register(financeDtoTool(
                "query_vouchers",
                "查询凭证台账",
                interfaces.getVoucher(),
                "queryVouchers",
                "com.zcy.finance.api.dto.VoucherQueryDTO",
                fields(
                        field("voucherNo", "string", "凭证号，可选"),
                        field("period", "string", "期间，格式 YYYY-MM，可选"),
                        field("status", "string", "状态，可选"),
                        field("accountCode", "string", "科目编码，可选"),
                        field("summaryKeyword", "string", "摘要关键字，可选"),
                        field("pageNo", "integer", "页码"),
                        field("pageSize", "integer", "每页条数")
                ),
                Collections.<String>emptyList(),
                "read"
        ));

        register(financeDtoTool(
                "query_invoice",
                "查询发票台账",
                interfaces.getInvoice(),
                "query",
                "com.zcy.finance.api.dto.InvoiceQueryDTO",
                fields(
                        field("invoiceNo", "string", "发票号码，可选"),
                        arrayStringField("dateRange", "开票日期范围，可选")
                ),
                Collections.<String>emptyList(),
                "read"
        ));

        register(financeDtoTool(
                "generate_report",
                "生成财务报表",
                interfaces.getReport(),
                "generate",
                "com.zcy.finance.api.dto.ReportGenerateDTO",
                fields(
                        field("reportType", "string", "报表类型：balance_sheet、income_statement、cash_flow"),
                        field("period", "string", "期间，格式 YYYY-MM")
                ),
                required("reportType", "period"),
                "read"
        ));

        register(financeFieldTool(
                "get_balance_sheet",
                "按期间生成资产负债表",
                interfaces.getReport(),
                "getBalanceSheet",
                "period",
                "期间，格式 YYYY-MM"
        ));

        register(financeFieldTool(
                "get_income_statement",
                "按期间生成利润表",
                interfaces.getReport(),
                "getIncomeStatement",
                "period",
                "期间，格式 YYYY-MM"
        ));

        register(financeFieldTool(
                "get_cash_flow_statement",
                "按期间生成现金流量表",
                interfaces.getReport(),
                "getCashFlowStatement",
                "period",
                "期间，格式 YYYY-MM"
        ));

        register(financeDtoTool(
                "create_expense",
                "创建费用报销单",
                interfaces.getExpense(),
                "createExpense",
                "com.zcy.finance.api.dto.ExpenseCreateDTO",
                fields(
                        field("employeeId", "string", "报销人员工号"),
                        field("employeeName", "string", "报销人姓名"),
                        field("department", "string", "部门"),
                        field("projectCode", "string", "项目编码"),
                        field("expenseType", "string", "费用类型"),
                        field("expenseDate", "string", "费用日期"),
                        field("description", "string", "报销说明"),
                        field("amount", "number", "报销金额"),
                        arrayStringField("invoiceNos", "发票号码数组"),
                        arrayStringField("attachments", "附件数组"),
                        field("submitForApproval", "boolean", "是否提交审批")
                ),
                required("employeeId", "amount"),
                "write"
        ));

        register(financeDtoTool(
                "query_expense",
                "查询费用报销单",
                interfaces.getExpense(),
                "query",
                "com.zcy.finance.api.dto.ExpenseQueryDTO",
                fields(
                        field("employeeId", "string", "员工号，可选"),
                        field("status", "string", "审批状态，可选"),
                        arrayStringField("dateRange", "费用日期范围，可选")
                ),
                Collections.<String>emptyList(),
                "read"
        ));

        register(financeDtoTool(
                "approve_expense",
                "智能审批费用报销单，输出风险项、审批意见和自动通过结果",
                interfaces.getExpense(),
                "approveExpense",
                "com.zcy.finance.api.dto.ExpenseApproveDTO",
                fields(
                        field("expenseNo", "string", "报销单号"),
                        field("employeeId", "string", "员工号"),
                        field("employeeName", "string", "报销人姓名"),
                        field("employeeLevel", "string", "员工级别"),
                        field("department", "string", "部门"),
                        field("projectCode", "string", "项目编码"),
                        field("expenseType", "string", "费用类型"),
                        field("cityTier", "string", "城市等级"),
                        field("submitDate", "string", "提交日期"),
                        field("invoiceNo", "string", "发票号码"),
                        field("purchaseOrderNo", "string", "采购订单号"),
                        field("receiptNo", "string", "入库单号"),
                        field("description", "string", "报销说明"),
                        field("amount", "number", "报销金额"),
                        field("invoiceAmount", "number", "发票金额"),
                        field("orderAmount", "number", "采购订单金额"),
                        field("receiptAmount", "number", "入库金额"),
                        field("availableBudget", "number", "可用预算"),
                        field("invoiceVerified", "boolean", "发票是否验真通过"),
                        field("duplicateInvoice", "boolean", "是否疑似重复发票"),
                        field("autoApproveEnabled", "boolean", "低风险自动通过开关"),
                        arrayField("ruleCitations", "RAG 命中的制度引用")
                ),
                required("expenseNo", "amount"),
                "write"
        ));

        register(financeFieldsTool(
                "query_budget_remaining",
                "查询部门预算余额",
                interfaces.getExpense(),
                "queryBudgetRemaining",
                fields(
                        field("department", "string", "部门"),
                        field("period", "string", "期间，格式 YYYY-MM")
                ),
                required("department", "period"),
                Arrays.asList("department", "period")
        ));
    }

    private ToolDefinition tokenPoTool(String name,
                                       String description,
                                       String interfaceName,
                                       String methodName,
                                       String parameterType,
                                       Map<String, Object> properties,
                                       List<String> required) {
        return poTool(name, description, interfaceName, methodName, parameterType, properties, required, "token", true);
    }

    private ToolDefinition poTool(String name,
                                  String description,
                                  String interfaceName,
                                  String methodName,
                                  String parameterType,
                                  Map<String, Object> properties,
                                  List<String> required,
                                  String authMode,
                                  boolean injectToken) {
        return new ToolDefinition(
                name,
                description,
                objectSchema(properties, required),
                new DubboMapping(interfaceName, methodName, Collections.singletonList(parameterType), new ConfigurableArgumentMapper("object", null, null)),
                "read",
                authMode,
                false,
                false,
                null,
                "contractNo",
                injectToken,
                false
        );
    }

    private ToolDefinition contractPrecheckStringTool(String name, String description, String interfaceName, String methodName) {
        return new ToolDefinition(
                name,
                description,
                objectSchema(fields(field("contractNo", "string", "合同编号")), required("contractNo")),
                new DubboMapping(interfaceName, methodName, Collections.singletonList("java.lang.String"), new ConfigurableArgumentMapper("field", "contractNo", null)),
                "read",
                "preCheckContractPermission",
                false,
                false,
                "contract.detail",
                "contractNo",
                false,
                false
        );
    }

    private ToolDefinition stringTool(String name,
                                      String description,
                                      String interfaceName,
                                      String methodName,
                                      String fieldName,
                                      String fieldDescription) {
        return new ToolDefinition(
                name,
                description,
                objectSchema(fields(field(fieldName, "string", fieldDescription)), required(fieldName)),
                new DubboMapping(interfaceName, methodName, Collections.singletonList("java.lang.String"), new ConfigurableArgumentMapper("field", fieldName, null))
        );
    }

    private ToolDefinition noArgTool(String name, String description, String interfaceName, String methodName) {
        return new ToolDefinition(
                name,
                description,
                objectSchema(fields(), Collections.<String>emptyList()),
                new DubboMapping(interfaceName, methodName, Collections.<String>emptyList(), new ConfigurableArgumentMapper("fields", null, Collections.<String>emptyList()))
        );
    }

    private ToolDefinition financeDtoTool(String name,
                                          String description,
                                          String interfaceName,
                                          String methodName,
                                          String parameterType,
                                          Map<String, Object> properties,
                                          List<String> required,
                                          String operation) {
        return new ToolDefinition(
                name,
                description,
                objectSchema(properties, required),
                new DubboMapping(interfaceName, methodName, Collections.singletonList(parameterType), new ConfigurableArgumentMapper("object", null, null)),
                operation,
                "none",
                false,
                false,
                null,
                "expenseNo",
                false,
                false
        );
    }

    private ToolDefinition financeFieldTool(String name,
                                            String description,
                                            String interfaceName,
                                            String methodName,
                                            String fieldName,
                                            String fieldDescription) {
        return new ToolDefinition(
                name,
                description,
                objectSchema(fields(field(fieldName, "string", fieldDescription)), required(fieldName)),
                new DubboMapping(interfaceName, methodName, Collections.singletonList("java.lang.String"), new ConfigurableArgumentMapper("field", fieldName, null))
        );
    }

    private ToolDefinition financeFieldsTool(String name,
                                             String description,
                                             String interfaceName,
                                             String methodName,
                                             Map<String, Object> properties,
                                             List<String> required,
                                             List<String> argumentFields) {
        List<String> parameterTypes = new ArrayList<String>();
        for (int i = 0; i < argumentFields.size(); i++) {
            parameterTypes.add("java.lang.String");
        }
        return new ToolDefinition(
                name,
                description,
                objectSchema(properties, required),
                new DubboMapping(interfaceName, methodName, parameterTypes, new ConfigurableArgumentMapper("fields", null, argumentFields))
        );
    }

    private void registerConfiguredTools() {
        for (FinanceMcpProperties.Tool tool : mcpProperties.getTools()) {
            validateConfiguredTool(tool);
            FinanceMcpProperties.Dubbo dubbo = tool.getDubbo();
            register(new ToolDefinition(
                    tool.getName(),
                    tool.getDescription(),
                    tool.getInputSchema(),
                    new DubboMapping(
                            dubbo.getInterfaceName(),
                            dubbo.getMethodName(),
                            dubbo.getParameterTypes(),
                            new ConfigurableArgumentMapper(dubbo.getArgumentMode(), dubbo.getArgumentField(), dubbo.getArgumentFields())
                    ),
                    tool.getOperation(),
                    tool.getAuthMode(),
                    tool.isConfirmRequired(),
                    tool.isAdminOnly(),
                    tool.getPermissionPrecheckTool(),
                    tool.getPermissionArgumentField(),
                    tool.isInjectToken(),
                    tool.isInjectDigitUser()
            ));
        }
    }

    public ToolDefinition get(String name) {
        return tools.get(name);
    }

    public List<Map<String, Object>> listMcpTools() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ToolDefinition tool : tools.values()) {
            result.add(tool.toMcpTool());
        }
        return result;
    }

    private void register(ToolDefinition toolDefinition) {
        tools.put(toolDefinition.getName(), toolDefinition);
    }

    private void validateConfiguredTool(FinanceMcpProperties.Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("finance.mcp.tools contains null item");
        }
        if (!StringUtils.hasText(tool.getName())) {
            throw new IllegalArgumentException("finance.mcp.tools.name is required");
        }
        if (!StringUtils.hasText(tool.getDescription())) {
            throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "].description is required");
        }
        if (tool.getInputSchema() == null || tool.getInputSchema().isEmpty()) {
            throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "].input-schema is required");
        }
        FinanceMcpProperties.Dubbo dubbo = tool.getDubbo();
        if (dubbo == null) {
            throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "].dubbo is required");
        }
        if (!StringUtils.hasText(dubbo.getInterfaceName())) {
            throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "].dubbo.interface-name is required");
        }
        if (!StringUtils.hasText(dubbo.getMethodName())) {
            throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "].dubbo.method-name is required");
        }
        if (dubbo.getParameterTypes() == null) {
            throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "].dubbo.parameter-types is required");
        }
        validateArgumentShape(tool, dubbo);
    }

    private void validateArgumentShape(FinanceMcpProperties.Tool tool, FinanceMcpProperties.Dubbo dubbo) {
        String mode = dubbo.getArgumentMode() == null ? "object" : dubbo.getArgumentMode();
        if ("object".equals(mode)) {
            if (dubbo.getParameterTypes().size() != 1) {
                throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "] object mode requires exactly one parameter type");
            }
            return;
        }
        if ("field".equals(mode)) {
            if (dubbo.getParameterTypes().size() != 1) {
                throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "] field mode requires exactly one parameter type");
            }
            if (!StringUtils.hasText(dubbo.getArgumentField())) {
                throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "].dubbo.argument-field is required");
            }
            return;
        }
        if ("fields".equals(mode)) {
            if (dubbo.getParameterTypes().size() != dubbo.getArgumentFields().size()) {
                throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "] parameter-types must match argument-fields size");
            }
            return;
        }
        throw new IllegalArgumentException("finance.mcp.tools[" + tool.getName() + "] unsupported argument-mode: " + mode);
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private static Map<String, Object> fields(Map<String, Object>... fields) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        for (Map<String, Object> field : fields) {
            properties.put((String) field.get("name"), field.get("schema"));
        }
        return properties;
    }

    private static Map<String, Object> field(String name, String type, String description) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", type);
        schema.put("description", description);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("name", name);
        result.put("schema", schema);
        return result;
    }

    private static Map<String, Object> arrayField(String name, String description) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "array");
        schema.put("description", description);
        schema.put("items", Collections.singletonMap("type", "object"));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("name", name);
        result.put("schema", schema);
        return result;
    }

    private static Map<String, Object> arrayStringField(String name, String description) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "array");
        schema.put("description", description);
        schema.put("items", Collections.singletonMap("type", "string"));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("name", name);
        result.put("schema", schema);
        return result;
    }

    private static List<String> required(String... names) {
        return Arrays.asList(names);
    }
}
