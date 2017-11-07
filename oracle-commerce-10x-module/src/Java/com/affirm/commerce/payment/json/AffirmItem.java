package com.affirm.commerce.payment.json;

public class AffirmItem {

	private String display_name;
	private String sku;
	private Integer unit_price;
	private Integer qty;
	private String item_image_url;
	private String item_url;
	
	public String getDisplay_name() {
		return display_name;
	}
	public void setDisplay_name(String display_name) {
		this.display_name = display_name;
	}
	
	public String getSku() {
		return sku;
	}
	public void setSku(String sku) {
		this.sku = sku;
	}
	
	public Integer getUnit_price() {
		return unit_price;
	}
	public void setUnit_price(Integer unit_price) {
		this.unit_price = unit_price;
	}
	
	public Integer getQty() {
		return qty;
	}
	public void setQty(Integer qty) {
		this.qty = qty;
	}
	
	public String getItem_image_url() {
		return item_image_url;
	}
	public void setItem_image_url(String item_image_url) {
		this.item_image_url = item_image_url;
	}
	
	public String getItem_url() {
		return item_url;
	}
	public void setItem_url(String item_url) {
		this.item_url = item_url;
	}
	
}
