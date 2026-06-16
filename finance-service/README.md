# finance-service

本模块提供财务数智人 Demo Dubbo 服务，按系统设计文档拆为：

- `finance-api`: Dubbo 接口、DTO、VO、统一返回包装
- `finance-infra`: Demo 数据
- `finance-core`: 应收看板、标准应收应付账龄、催收建议、应收催收计划、应付付款计划、往来余额对账、科目映射、凭证创建、凭证台账查询、凭证自动审核、发票录入、标准发票查验、发票台账、发票独立查重、增强发票验真查验、资产负债表、利润表、现金流量表、综合分析、财务比率、趋势分析、异常检测、差异归因、预算管控、月末结账、报销、报销智能审批、税务、银行对账、合规风控、资金管理、合同与资产、数据集成、调用审计服务实现
- `finance-starter`: Dubbo provider 启动模块

`finance-infra/data/demo-data.sql` 提供设计文档要求的核心表与演示数据。`finance-starter` 默认使用 H2 MySQL 兼容模式在启动时自动执行该脚本；接入 MySQL 时可覆盖 `spring.datasource.*` 并继续复用同一脚本。

凭证、发票、费用报销、预算、银行流水、应收应付和审计调用链通过 JDBC 持久化或查询；资产负债表、利润表、现金流量表通过 SQL 聚合生成，不再依赖进程内静态列表保存核心业务写入或报表结果。报表接口启用 Spring Cache，本地 Demo 默认使用内存缓存；生产环境可启用 MySQL + Redis：

```bash
SPRING_PROFILES_ACTIVE=production \
FINANCE_MYSQL_URL='jdbc:mysql://127.0.0.1:3306/finance_demo?serverTimezone=Asia/Shanghai' \
FINANCE_MYSQL_USERNAME=finance \
FINANCE_MYSQL_PASSWORD=finance \
FINANCE_REDIS_HOST=127.0.0.1 \
java -jar finance-starter/target/finance-starter-0.0.1-SNAPSHOT.jar
```

## Run

从仓库根目录启动：

```bash
mvn -q -DskipTests package
java -jar finance-service/finance-starter/target/finance-starter-0.0.1-SNAPSHOT.jar
```

默认导出 Dubbo 服务到：

```text
dubbo://127.0.0.1:20880
```

启动后可配合 `mcp/finance-mcp` 的 `query_ar_dashboard`、`query_ar_ap_aging`、`suggest_collection_advice`、`reconcile_ar_ap`、`generate_collection_plan`、`optimize_payment_plan`、`reconcile_counterparty_balance`、`recommend_account_mapping`、`create_voucher`、`get_voucher`、`query_vouchers`、`audit_voucher`、`audit_voucher_by_no`、`query_invoice`、`check_invoice`、`input_invoice`、`check_invoice_duplicate`、`verify_invoice`、`generate_report`、`get_balance_sheet`、`get_income_statement`、`get_cash_flow_statement`、`analyze_financial`、`calculate_financial_ratios`、`analyze_financial_trend`、`detect_financial_anomalies`、`diagnose_financial_variance`、`evaluate_budget_control`、`run_month_end_close`、`create_expense`、`query_expense`、`approve_expense`、`query_budget_remaining`、`calculate_tax`、`generate_tax_return`、`query_bank_transactions`、`reconcile_bank_statement`、`assess_compliance_risk`、`forecast_cash_flow`、`query_contract_assets`、`query_data_integration`、`record_audit_log`、`query_audit_logs` 工具联调。
