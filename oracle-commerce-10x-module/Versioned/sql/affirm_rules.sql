-- Table to hold rules
-- DROP TABLE affirm_rule;

CREATE TABLE affirm_rule (
	rule_id VARCHAR2(40) NOT NULL,
	display_name VARCHAR2(200),
	program_id VARCHAR2(100),
	data_promo_id VARCHAR2(100),
	rule_type INTEGER,
	exclusive_flag NUMBER(1,0),
	sku_id VARCHAR2(40),
	category_id VARCHAR2(40),
	amount NUMBER(19,7),
	start_date TIMESTAMP,
	end_date TIMESTAMP,
	priority INTEGER,
	asset_version INT NOT NULL,
	workspace_id VARCHAR(40) NOT NULL,
	branch_id VARCHAR(40) NOT NULL,
	is_head NUMERIC(1) NOT NULL,
	version_deleted NUMERIC(1) NOT NULL,
	version_editable NUMERIC(1) NOT NULL,
	pred_version INT NULL,
	checkin_date TIMESTAMP NULL,
	CONSTRAINT affirm_rule_pk PRIMARY KEY (rule_id, asset_version)
);

CREATE INDEX affirm_rule_wsx ON affirm_rule (workspace_id);
CREATE INDEX affirm_rule_cix ON affirm_rule (checkin_date);


CREATE TABLE affirm_rule_products (
	rule_id VARCHAR2(40) NOT NULL,
	sequence_num INTEGER NOT NULL,
	product_id VARCHAR2(40) NOT NULL,
	asset_version NUMBER(19) NOT NULL,
	CONSTRAINT affirm_rule_prd_pk PRIMARY KEY (rule_id, sequence_num, asset_version)
);

CREATE INDEX affirm_rule_rul_idx on affirm_rule_products (rule_id);
CREATE INDEX affirm_rule_prd_idx on affirm_rule_products (product_id);


