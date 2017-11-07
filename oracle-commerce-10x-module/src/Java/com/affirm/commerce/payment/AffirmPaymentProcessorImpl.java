package com.affirm.commerce.payment;

import java.util.Date;

import atg.commerce.CommerceException;
import atg.commerce.order.OrderManager;
import atg.core.util.StringUtils;
import atg.nucleus.GenericService;

import com.affirm.commerce.payment.response.AffirmResponse;
import com.affirm.payment.AffirmPaymentConstants;

public class AffirmPaymentProcessorImpl extends GenericService implements AffirmPaymentProcessor {

	private AffirmUtil affirmUtil; 
	private OrderManager orderManager;
	
	@Override
	public AffirmPaymentStatus authorize(AffirmPaymentInfo affirmPaymentInfo) throws CommerceException{
		
		if(isLoggingDebug()) {
			logDebug("Authorize getting called on AffirmPaymentProcessorImpl for order:" + affirmPaymentInfo.getOrder().getId() + ", Auth Txn Id: " + affirmPaymentInfo.getAuthTxnId());
		}
		
		
		try {
			
			AffirmResponse authResponse = this.getAffirmUtil().authorizeCharge(
					affirmPaymentInfo.getOrder().getId(), affirmPaymentInfo.getCheckoutToken());

			// check for Auth error
			String authId = authResponse.getId();
			String transactionId = authResponse.getTransactionId();
			
			if (authResponse.getErrorCode() != AffirmPaymentConstants.SUCCESS_CODE) {
				throw new CommerceException("Error while authorizing payment");
			}
			
			// set this authId on PG
			AffirmPayment pg = getAffirmUtil().getAffirmPaymentGroup(affirmPaymentInfo.getOrder());
			
			pg.setChargeId(authId);
			pg.setAuthTxnId(transactionId);
			
			return new AffirmPaymentStatus(transactionId, affirmPaymentInfo.getAmount(), true, "",	new Date());


		} catch (Exception ex) {
			if (isLoggingError()) {
				logError("Exception while trying to Auth:", ex);
			}
			throw new CommerceException("Error while authorizing payment for order:" + affirmPaymentInfo.getOrder().getId());
		}
		
	}

	@Override
	public AffirmPaymentStatus debit(AffirmPaymentInfo paymentInfo,	AffirmPaymentStatus status) {

		if(isLoggingDebug()){
			logDebug("Debit method called for order:" + paymentInfo.getOrder().getId() + ", charge Id: " + paymentInfo.getChargeId());
		}
		
		AffirmResponse captureResponse = this.getAffirmUtil().captureCharge(paymentInfo.getOrder().getId(), paymentInfo.getChargeId());
		if(captureResponse != null && StringUtils.isNotBlank(captureResponse.getId())){
			return new AffirmPaymentStatus(captureResponse.getId(), paymentInfo.getAmount(), true, null, new Date());
		} else {				
			return new AffirmPaymentStatus(Long.toString(System.currentTimeMillis()), paymentInfo.getAmount(), false, "Capture failed", new Date());
		}


	}
	
	
	/* (non-Javadoc)
	 * @see com.affirm.commerce.payment.AffirmPaymentProcessor#voidAuthorization(com.affirm.commerce.payment.AffirmPaymentInfo)
	 */
	public AffirmPaymentStatus decreaseAuthorizationForPaymentGroup(AffirmPaymentInfo paymentInfo) {

		if(isLoggingDebug()){
			logDebug("Void Authorization method called for order:" + paymentInfo.getOrder().getId() + ", charge Id: " + paymentInfo.getChargeId());
		}
		
		AffirmResponse voidResponse = this.getAffirmUtil().voidCharge(paymentInfo.getChargeId());
		if(voidResponse != null && StringUtils.isNotBlank(voidResponse.getId())){
			return new AffirmPaymentStatus(voidResponse.getId(), paymentInfo.getAmount(), true, null, new Date());
		} else {
			return new AffirmPaymentStatus(Long.toString(System.currentTimeMillis()), paymentInfo.getAmount(), false, "Void failed", new Date());
		}

	}
	
	

	@Override
	public AffirmPaymentStatus credit(AffirmPaymentInfo paymentInfo, AffirmPaymentStatus status) {

		if(isLoggingDebug()){
			logDebug("Credit method called for order:" + paymentInfo.getOrder().getId() + ", charge Id: " + paymentInfo.getChargeId());
		}
		
		AffirmResponse refundResponse = this.getAffirmUtil().refundCharge(paymentInfo.getChargeId());
		if(refundResponse != null && StringUtils.isNotBlank(refundResponse.getId())){
			return new AffirmPaymentStatus(refundResponse.getId(), paymentInfo.getAmount(), true, null,	new Date());
		} else {
			return new AffirmPaymentStatus(Long.toString(System.currentTimeMillis()), paymentInfo.getAmount(), false, "Credit failed",	new Date());
		}

	}

	@Override
	public AffirmPaymentStatus credit(AffirmPaymentInfo paymentInfo, double refundAmount) {

		if (isLoggingDebug()) {
			logDebug("Credit method called for order:" + paymentInfo.getOrder().getId() + ", charge Id: " + paymentInfo.getChargeId() + ", refund Amount = " +refundAmount);
		}

		AffirmResponse refundResponse = this.getAffirmUtil().refundCharge(paymentInfo.getChargeId(),refundAmount);
		if (refundResponse != null && StringUtils.isNotBlank(refundResponse.getId())){
			return new AffirmPaymentStatus(refundResponse.getId(), paymentInfo.getAmount(), true, null,	new Date());
		} else {
			return new AffirmPaymentStatus(Long.toString(System.currentTimeMillis()), paymentInfo.getAmount(), false, "Credit failed", new Date());
		}
		
	}

	public AffirmUtil getAffirmUtil() {
		return affirmUtil;
	}

	public void setAffirmUtil(AffirmUtil affirmUtil) {
		this.affirmUtil = affirmUtil;
	}

	public OrderManager getOrderManager() {
		return orderManager;
	}

	public void setOrderManager(OrderManager orderManager) {
		this.orderManager = orderManager;
	}
	

}
