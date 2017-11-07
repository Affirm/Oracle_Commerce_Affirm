package com.affirm.commerce.payment.json;

public class AffirmMetaData {
	
	private String platform_type;
	private String platform_version;
	private String order_id;
	private String platform_affirm;
	private String order_key;
	
	public String getPlatform_type() {
		return platform_type;
	}
	public void setPlatform_type(String platform_type) {
		this.platform_type = platform_type;
	}
	public String getPlatform_version() {
		return platform_version;
	}
	public void setPlatform_version(String platform_version) {
		this.platform_version = platform_version;
	}
	public String getOrder_id() {
		return order_id;
	}
	public void setOrder_id(String order_id) {
		this.order_id = order_id;
	}
	public String getPlatform_affirm() {
		return platform_affirm;
	}
	public void setPlatform_affirm(String platform_affirm) {
		this.platform_affirm = platform_affirm;
	}
	public String getOrder_key() {
		return order_key;
	}
	public void setOrder_key(String order_key) {
		this.order_key = order_key;
	}

}
