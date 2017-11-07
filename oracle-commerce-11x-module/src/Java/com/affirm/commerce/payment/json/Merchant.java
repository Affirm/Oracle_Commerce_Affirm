package com.affirm.commerce.payment.json;

public class Merchant {

	private String public_api_key;
	private String user_cancel_url;
	private String user_confirmation_url;
	
	public String getPublic_api_key() {
		return public_api_key;
	}
	public void setPublic_api_key(String public_api_key) {
		this.public_api_key = public_api_key;
	}
	
	public String getUser_cancel_url() {
		return user_cancel_url;
	}
	public void setUser_cancel_url(String user_cancel_url) {
		this.user_cancel_url = user_cancel_url;
	}
	
	public String getUser_confirmation_url() {
		return user_confirmation_url;
	}
	public void setUser_confirmation_url(String user_confirmation_url) {
		this.user_confirmation_url = user_confirmation_url;
	}
	
}
