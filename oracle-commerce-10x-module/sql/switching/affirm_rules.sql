-- Table to hold rules
drop table affirm_rule;

create table affirm_rule (
	rule_id VARCHAR2(40) NOT NULL,
	display_name VARCHAR2(200),
	program_id VARCHAR2(100),
	data_promo_id VARCHAR2(100),
	rule_type INTEGER,
	exclusive_flag NUMBER(1,0),
	product_id VARCHAR2(40),
	sku_id VARCHAR2(40),
	category_id VARCHAR2(40),
	amount NUMBER(19,7),
	start_date TIMESTAMP,
	end_date TIMESTAMP,
	priority INTEGER,
	CONSTRAINT affirm_rule_pk PRIMARY KEY (rule_id)
);

create table affirm_rule_products  (
	rule_id VARCHAR2(40) NOT NULL,
	sequence_num INTEGER NOT NULL,
	product_id VARCHAR2(40) NOT NULL,
	CONSTRAINT affirm_rule_products_pk PRIMARY KEY (rule_id, sequence_num),
	CONSTRAINT affirm_rule_prd_prd_fk FOREIGN KEY (product_id) references dcs_product (product_id),
	CONSTRAINT affirm_rule_prd_rul_fk FOREIGN KEY (rule_id) references affirm_rule (rule_id)
);

