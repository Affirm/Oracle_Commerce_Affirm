package com.affirm.commerce.payment;

import atg.commerce.order.Order;

/**
 * Affirm paymentInfo class
 * 
 * @author dev
 *
 */
public class AffirmPaymentInfo {

	
	private double amount;
	private Order order;
	private String checkoutToken;
	private String chargeId;
	private String authTxnId;
	
	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	public Order getOrder() {
		return order;
	}
	public void setOrder(Order order) {
		this.order = order;
	}
	
	public String getCheckoutToken() {
		return checkoutToken;
	}
	public void setCheckoutToken(String checkoutToken) {
		this.checkoutToken = checkoutToken;
	}
	
	public String getChargeId() {
		return chargeId;
	}
	public void setChargeId(String chargeId) {
		this.chargeId = chargeId;
	}
	public String getAuthTxnId() {
		return authTxnId;
	}
	public void setAuthTxnId(String authTxnId) {
		this.authTxnId = authTxnId;
	}
	

	
}
