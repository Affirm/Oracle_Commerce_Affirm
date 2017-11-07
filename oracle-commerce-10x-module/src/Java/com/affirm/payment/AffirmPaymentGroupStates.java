package com.affirm.payment;

import atg.commerce.states.PaymentGroupStates;

public class AffirmPaymentGroupStates extends PaymentGroupStates {
	
	public static final String VOID = "void";
	public static final String REFUNDED = "refunded";
	public static final String PARTIALLY_REFUNDED = "partially_refunded";
	
	private String resourcefileName;

	public String getResourcefileName() {
		return this.resourcefileName == null ? super.getResourceFileName() : this.getResourcefileName();
	}

	public void setResourcefileName(String resourcefileName) {
		this.resourcefileName = resourcefileName;
	}

}
