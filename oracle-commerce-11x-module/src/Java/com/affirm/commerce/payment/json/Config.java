package com.affirm.commerce.payment.json;

public class Config {
	
	private String public_api_key;
	private String financial_product_key;
	
	public String getPublic_api_key() {
		return public_api_key;
	}
	public void setPublic_api_key(String public_api_key) {
		this.public_api_key = public_api_key;
	}
	
	public String getFinancial_product_key() {
		return financial_product_key;
	}
	public void setFinancial_product_key(String financial_product_key) {
		this.financial_product_key = financial_product_key;
	}

}
