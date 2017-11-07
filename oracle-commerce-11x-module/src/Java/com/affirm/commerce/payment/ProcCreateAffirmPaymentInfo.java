package com.affirm.commerce.payment;

import atg.commerce.order.Order;
import atg.commerce.payment.PaymentManagerPipelineArgs;
import atg.nucleus.GenericService;
import atg.service.pipeline.PipelineProcessor;
import atg.service.pipeline.PipelineResult;

/**
 * pipeline processor responsible for creating AffirmPaymentInfo object
 * 
 * @author dev
 * 
 */
public class ProcCreateAffirmPaymentInfo extends GenericService implements PipelineProcessor {
	/**
	 * info class to be created
	 */
	private String affirmPaymentInfoClass;
	/** The possible return value for this processor. **/
	private static final int SUCCESS = 1;
	private static final int retCodes[] = { SUCCESS };

	@Override
	public int[] getRetCodes() {
		return retCodes;
	}

	/**
	 * implemented runProcess method grabs the payment group creates a new
	 * paymentInfo class and populates it, sets it in pipeline for the next
	 * processor to pick it up
	 * 
	 */
	@Override
	public int runProcess(Object param, PipelineResult result) throws Exception {
		PaymentManagerPipelineArgs params = (PaymentManagerPipelineArgs) param;
		Order order = params.getOrder();
		AffirmPayment affirmPayment = (AffirmPayment) params.getPaymentGroup();
		double amount = params.getAmount();


		// create and populate store points info class
		AffirmPaymentInfo api = getAffirmPaymentInfo();
		addDataToAffirmPaymentInfo(order, affirmPayment, amount, params, api);
		if (isLoggingDebug())
			logDebug("Putting AffirmPaymentInfo object into pipeline: " + api.toString());
		params.setPaymentInfo(api);
		return SUCCESS;
	}


	/**
	 * creates a new AffirmPaymentInfo object
	 * 
	 * @return
	 * @throws Exception
	 */
	public AffirmPaymentInfo getAffirmPaymentInfo() throws Exception {
		if (isLoggingDebug())
			logDebug("Making a new instance of type: " + getAffirmPaymentInfoClass());
		AffirmPaymentInfo api = (AffirmPaymentInfo) Class.forName(getAffirmPaymentInfoClass()).newInstance();
		return api;
	}

	/**
	 * populate AffirmPaymentInfo object
	 * 
	 * @param pOrder
	 * @param paymentGroup
	 * @param amount
	 * @param params
	 * @param affirmPaymentInfo
	 */
	protected void addDataToAffirmPaymentInfo(Order pOrder, AffirmPayment paymentGroup, double amount,
		PaymentManagerPipelineArgs params, AffirmPaymentInfo affirmPaymentInfo) {
		
		affirmPaymentInfo.setAmount(amount);
		affirmPaymentInfo.setOrder(pOrder);
		affirmPaymentInfo.setChargeId(paymentGroup.getChargeId());
		affirmPaymentInfo.setAuthTxnId(paymentGroup.getAuthTxnId());
		affirmPaymentInfo.setCheckoutToken(paymentGroup.getCheckoutToken());
	}

	public String getAffirmPaymentInfoClass() {
		return affirmPaymentInfoClass;
	}

	public void setAffirmPaymentInfoClass(String affirmPaymentInfoClass) {
		this.affirmPaymentInfoClass = affirmPaymentInfoClass;
	}

	
}
