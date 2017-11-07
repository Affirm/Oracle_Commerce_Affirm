package com.affirm.commerce.payment;

import atg.commerce.CommerceException;
import atg.commerce.payment.PaymentManagerPipelineArgs;
import atg.commerce.payment.processor.ProcProcessPaymentGroup;
import atg.payment.PaymentStatus;

/**
 * pipeline processor for processing payments, this processor runs after
 * ProcCreateAffirmPaymentInfo paymentInfo object is passed to the correct
 * processor to authorize/debit etc..
 * 
 * @author dev
 * 
 */
public class ProcProcessAffirmPayment extends ProcProcessPaymentGroup {

	/**
	 * reference to processor which will actually processes the transaction
	 */
	private AffirmPaymentProcessor paymentProcessor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * atg.commerce.payment.processor.ProcProcessPaymentGroup#authorizePaymentGroup
	 * (atg.commerce.payment.PaymentManagerPipelineArgs)
	 */
	@Override
	public PaymentStatus authorizePaymentGroup(PaymentManagerPipelineArgs pParams) throws CommerceException {
		AffirmPaymentInfo paymentInfo = (AffirmPaymentInfo) pParams.getPaymentInfo();
		return getPaymentProcessor().authorize(paymentInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * atg.commerce.payment.processor.ProcProcessPaymentGroup#creditPaymentGroup
	 * (atg.commerce.payment.PaymentManagerPipelineArgs)
	 */
	@Override
	public PaymentStatus creditPaymentGroup(PaymentManagerPipelineArgs pParams) throws CommerceException {
		AffirmPaymentInfo paymentInfo = (AffirmPaymentInfo) pParams.getPaymentInfo();
		return getPaymentProcessor().credit(paymentInfo,pParams.getAmount());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * atg.commerce.payment.processor.ProcProcessPaymentGroup#debitPaymentGroup
	 * (atg.commerce.payment.PaymentManagerPipelineArgs)
	 */
	@Override
	public PaymentStatus debitPaymentGroup(PaymentManagerPipelineArgs pParams) throws CommerceException {
		AffirmPaymentInfo paymentInfo = (AffirmPaymentInfo) pParams.getPaymentInfo();
		AffirmPaymentStatus authStatus = (AffirmPaymentStatus) pParams.getPaymentStatus();
		return getPaymentProcessor().debit(paymentInfo, authStatus);
	}
	
	
	

	public PaymentStatus decreaseAuthorizationForPaymentGroup(PaymentManagerPipelineArgs pParams) throws CommerceException {
		AffirmPaymentInfo paymentInfo = (AffirmPaymentInfo) pParams.getPaymentInfo();
		return getPaymentProcessor().decreaseAuthorizationForPaymentGroup(paymentInfo);
	}
	

	public AffirmPaymentProcessor getPaymentProcessor() {
		return paymentProcessor;
	}

	public void setPaymentProcessor(AffirmPaymentProcessor paymentProcessor) {
		this.paymentProcessor = paymentProcessor;
	}




}
