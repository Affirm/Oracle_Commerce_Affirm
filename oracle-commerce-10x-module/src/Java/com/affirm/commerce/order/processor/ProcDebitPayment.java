package com.affirm.commerce.order.processor;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import atg.commerce.order.CreditCard;
import atg.commerce.order.GiftCertificate;
import atg.commerce.order.InvalidParameterException;
import atg.commerce.order.Order;
import atg.commerce.order.PaymentGroup;
import atg.commerce.payment.PaymentManager;
import atg.nucleus.logging.ApplicationLoggingImpl;
import atg.service.pipeline.PipelineProcessor;
import atg.service.pipeline.PipelineResult;

import com.affirm.commerce.payment.AffirmPaymentConfiguration;
import com.affirm.commerce.payment.AffirmUtil;

public class ProcDebitPayment extends ApplicationLoggingImpl implements PipelineProcessor {

	private final int SUCCESS = 1;
	private PaymentManager paymentManager;
	private AffirmPaymentConfiguration configuration;
	private AffirmUtil affirmUtil;

	String mLoggingIdentifier = "ProcDebitPayment";

	public int[] getRetCodes() {
		int[] ret = new int[] { 1 };
		return ret;
	}

	public void setLoggingIdentifier(String pLoggingIdentifier) {
		this.mLoggingIdentifier = pLoggingIdentifier;
	}

	public String getLoggingIdentifier() {
		return this.mLoggingIdentifier;
	}

	@SuppressWarnings("rawtypes")
	public int runProcess(Object pParam, PipelineResult pResult) throws Exception {
		
		if(!this.getConfiguration().isCapturePaymentOnOrderSubmit()){
			if(isLoggingDebug()){
				logDebug("No Capture on order submit");
			}
			return SUCCESS;
		}
		
		HashMap map = (HashMap) pParam;
		List failedPaymentGroups = null;
		Order order = (Order) map.get("Order");
		if (order == null) {
			throw new InvalidParameterException("Invalid Order Parameter");
		} 
		else 
		{
			//check to see that the order has only affirm pg
			if(!getAffirmUtil().isAffirmCheckout(order))
			{
				if(isLoggingDebug()){
					logDebug("The order passed " + order.getId() + " is not an affirm order. Returning Success");
				}
				return SUCCESS;
			}
			
			if (this.isLoggingDebug()) 
			{
				this.logDebug("Capturing PaymentGroups: " + order.getPaymentGroups());
			}
				
			failedPaymentGroups = this.getPaymentManager().debit(order, order.getPaymentGroups());
			if (failedPaymentGroups.size() > 0) {
				if (this.isLoggingDebug()) {
					this.logDebug("The following payment groups failed debit: " + failedPaymentGroups);
				}
				
				return -1;

			}
			
			return SUCCESS;
		}
		
	}

	protected void addErrorToPipelineResult(PaymentGroup pFailedPaymentGroup, String pStatusMessage,
			PipelineResult pResult, ResourceBundle pBundle) {
		if (pFailedPaymentGroup instanceof GiftCertificate) {
			this.addGiftCertificateError(pFailedPaymentGroup, pStatusMessage, pResult, pBundle);
		} else if (pFailedPaymentGroup instanceof CreditCard) {
			this.addCreditCardError(pFailedPaymentGroup, pStatusMessage, pResult, pBundle);
		} else {
			this.addPaymentGroupError(pFailedPaymentGroup, pStatusMessage, pResult, pBundle);
		}

	}

	protected void addGiftCertificateError(PaymentGroup pFailedPaymentGroup, String pStatusMessage,
			PipelineResult pResult, ResourceBundle pBundle) {
		String errorMessage = MessageFormat.format(pBundle.getString("FailedGiftCertificateAuthorization"),
				new Object[] { ((GiftCertificate) pFailedPaymentGroup).getGiftCertificateNumber(), pStatusMessage });
		String errorKey = "FailedGiftCertAuth:" + pFailedPaymentGroup.getId();
		pResult.addError(errorKey, errorMessage);
	}

	protected void addCreditCardError(PaymentGroup pFailedPaymentGroup, String pStatusMessage, PipelineResult pResult,
			ResourceBundle pBundle) {
		String errorMessage = MessageFormat.format(pBundle.getString("FailedCreditCardAuthorization"),
				new Object[] { ((CreditCard) pFailedPaymentGroup).getCreditCardType(),
						((CreditCard) pFailedPaymentGroup).getCreditCardNumber(), pStatusMessage });
		String errorKey = "FailedCreditCardAuth:" + pFailedPaymentGroup.getId();
		pResult.addError(errorKey, errorMessage);
	}

	protected void addPaymentGroupError(PaymentGroup pFailedPaymentGroup, String pStatusMessage, PipelineResult pResult,
			ResourceBundle pBundle) {
		String errorMessage = MessageFormat.format(pBundle.getString("FailedPaymentGroupAuthorization"),
				new Object[] { pFailedPaymentGroup.getId(), pStatusMessage });
		String errorKey = "FailedPaymentGroupAuth:" + pFailedPaymentGroup.getId();
		pResult.addError(errorKey, errorMessage);
	}
	
	public AffirmPaymentConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(AffirmPaymentConfiguration configuration) {
		this.configuration = configuration;
	}

	public PaymentManager getPaymentManager() {
		return paymentManager;
	}

	public void setPaymentManager(PaymentManager paymentManager) {
		this.paymentManager = paymentManager;
	}

	public AffirmUtil getAffirmUtil() {
		return affirmUtil;
	}

	public void setAffirmUtil(AffirmUtil affirmUtil) {
		this.affirmUtil = affirmUtil;
	}
	
	
}  