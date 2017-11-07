package com.affirm.commerce.payment;

import java.util.Date;

import atg.payment.PaymentStatusImpl;

/**
 * Status class for Affirm checkout, extends base PaymentStatusImpl customs
 * properties will be defined here
 * 
 * @author dev
 * 
 */
public class AffirmPaymentStatus extends PaymentStatusImpl {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AffirmPaymentStatus() {
		super();
	}

	public AffirmPaymentStatus(String pTransactionId, double pAmount, boolean pTransactionSuccess, String pErrorMessage,
			Date pTransactionTimestamp) {
		super(pTransactionId, pAmount, pTransactionSuccess, pErrorMessage, pTransactionTimestamp);
	}

}