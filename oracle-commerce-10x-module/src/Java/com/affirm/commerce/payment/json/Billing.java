package com.affirm.commerce.payment.json;

public class Billing {

	private Name name;
	private AffirmAddress address;
	private String email;
	private String phone;

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public AffirmAddress getAddress() {
		return address;
	}

	public void setAddress(AffirmAddress address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

}