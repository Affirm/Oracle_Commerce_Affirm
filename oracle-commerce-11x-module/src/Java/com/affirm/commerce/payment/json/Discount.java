package com.affirm.commerce.payment.json;

public class Discount {

	private String discount_display_name;
	private Integer discount_amount;
	
	public String getDiscount_display_name() {
		return discount_display_name;
	}
	public void setDiscount_display_name(String discount_display_name) {
		this.discount_display_name = discount_display_name;
	}
	
	public Integer getDiscount_amount() {
		return discount_amount;
	}
	public void setDiscount_amount(Integer discount_amount) {
		this.discount_amount = discount_amount;
	}
	
}
