-- 创建case表
CREATE TABLE IF NOT EXISTS "case" (
    "id" BIGINT PRIMARY KEY AUTO_INCREMENT,
    "created_at" TIMESTAMP NOT NULL,
    "updated_at" TIMESTAMP NOT NULL,
    "deleted" BOOLEAN NOT NULL,
    "acceptance_date" DATE,
    "actual_received" DECIMAL(15,2),
    "amount" DECIMAL(15,2),
    "archive_date" DATE,
    "archive_location" VARCHAR(255),
    "asset_batch_no" VARCHAR(100),
    "attorney_fee" DECIMAL(15,2),
    "business_type" VARCHAR(50),
    "case_name" VARCHAR(255) NOT NULL,
    "case_number" VARCHAR(50) UNIQUE,
    "case_reason" VARCHAR(100),
    "case_type" VARCHAR(20) NOT NULL,
    "client_id" BIGINT,
    "close_date" DATE,
    "close_status" VARCHAR(20),
    "co_departments" CLOB,
    "collateral_status" VARCHAR(50),
    "commission_date" DATE,
    "conflict_check_status" VARCHAR(20),
    "conflict_waiver_approval_id" BIGINT,
    "contract_end_date" DATE,
    "contract_start_date" DATE,
    "court" VARCHAR(100),
    "court_case_number" VARCHAR(100),
    "criminal_suspect" VARCHAR(100),
    "current_stage" VARCHAR(50),
    "deadline_date" DATE,
    "department_percentage" DECIMAL(5,2),
    "disputed_amount" DECIMAL(15,2),
    "entrusting_bank_name" VARCHAR(100),
    "execution_recovery_amount" DECIMAL(15,2),
    "fee_method" VARCHAR(20),
    "fee_remark" VARCHAR(1000),
    "filing_date" DATE,
    "firm_percentage" DECIMAL(5,2),
    "fixed_fee" DECIMAL(15,2),
    "guarantee_type" VARCHAR(50),
    "hearing_date" DATE,
    "host_department" CLOB,
    "interest_balance" DECIMAL(15,2),
    "is_legal_aid" BOOLEAN,
    "level" VARCHAR(20),
    "loan_contract_no" VARCHAR(100),
    "method" VARCHAR(20),
    "npa_subtype" VARCHAR(50),
    "other_clients" CLOB,
    "owner_id" BIGINT NOT NULL,
    "preservation_status" VARCHAR(50),
    "principal_balance" DECIMAL(15,2),
    "procedure" VARCHAR(20),
    "procedure_levels" CLOB,
    "remark" VARCHAR(1000),
    "representation_type" VARCHAR(20),
    "risk_fee" DECIMAL(15,2),
    "risk_ratio" DECIMAL(5,2),
    "source_person" CLOB,
    "source_person_percentage" DECIMAL(5,2),
    "status" VARCHAR(20),
    "strategies" CLOB,
    "summary" VARCHAR(255),
    "tags" VARCHAR(500),
    "termination_status" VARCHAR(50),
    "transfer_agreement_no" VARCHAR(100),
    "won_amount" DECIMAL(15,2)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS "idx_case_name" ON "case" ("case_name");
CREATE INDEX IF NOT EXISTS "idx_case_status" ON "case" ("status");
CREATE INDEX IF NOT EXISTS "idx_case_owner" ON "case" ("owner_id");
CREATE INDEX IF NOT EXISTS "idx_case_created" ON "case" ("created_at");
CREATE INDEX IF NOT EXISTS "idx_case_deleted" ON "case" ("deleted");

-- 创建audit_log表
CREATE TABLE IF NOT EXISTS "audit_log" (
    "id" BIGINT PRIMARY KEY AUTO_INCREMENT,
    "created_at" TIMESTAMP NOT NULL,
    "error_msg" VARCHAR(255),
    "execution_time" INTEGER,
    "ip" VARCHAR(50),
    "method" VARCHAR(100),
    "module" VARCHAR(50) NOT NULL,
    "operation" VARCHAR(50) NOT NULL,
    "params" CLOB,
    "status" INTEGER,
    "user_id" BIGINT NOT NULL
);

-- 注意：user表等核心表会由Hibernate自动创建，这里只创建case表和audit_log表
-- 测试用户数据由DataInitializer负责插入
