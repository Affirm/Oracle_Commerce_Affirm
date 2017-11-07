package com.affirm.commerce.payment;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.OrderImpl;
import atg.commerce.order.ShippingGroup;
import atg.commerce.payment.PaymentManager;
import atg.core.util.ContactInfo;
import atg.core.util.StringUtils;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.GenericService;
import atg.projects.store.order.StoreOrderManager;

import com.affirm.commerce.payment.response.AffirmResponse;

public class AffirmProcessingManager extends GenericService {
	
	private String paymentMethod;
	private StoreOrderManager orderManager;
	private PaymentManager paymentManager;
	private AffirmUtil affirmUtil;
	private TransactionManager transactionManager;
	private ArrayList<String> refundableStates = new ArrayList<String>();
		
	/**
	 * @param orderId
	 * @param shipmentCarrier
	 * @param shipmentTracking
	 */
	public boolean updateShippingDetails(String orderId, ContactInfo shippingAddress, String shipmentCarrier, String shipmentTracking){

		TransactionManager tm = getTransactionManager();
		TransactionDemarcation td = new TransactionDemarcation();
		boolean rollback = false;
		boolean updateOrderCalled = false;
		OrderImpl atgOrder = null;
		String chargeId = null;

		try {

			atgOrder = (OrderImpl) getOrderManager().loadOrder(orderId);

			if (atgOrder == null) {
				if (isLoggingError()) {
					logError("Could not load order from the repository for id: " + orderId);
				}
				return false;
			}

			td.begin(tm, TransactionDemarcation.REQUIRED);

			try {

				// now synchronize on the order
				synchronized (atgOrder) {

					// get AffirmPaymentGroup
					AffirmPayment paymentGroup = getAffirmUtil().getAffirmPaymentGroup(atgOrder);

					if (paymentGroup != null) {
						chargeId = paymentGroup.getChargeId();
					} else {
						if (isLoggingError()) {
							logError("Trying to update shipping details on Affirm order with no Affirm payment group!");
						}
						// Nothing changed, no reason to rollback. Set updateOrderCalled to true to avoid the rollback logic.
						updateOrderCalled = true;
						return false;
					}

					updateShippingDetails(atgOrder, shippingAddress, shipmentCarrier, shipmentTracking);
					AffirmResponse response = this.getAffirmUtil().updateCharge(chargeId, orderId, shippingAddress, shipmentCarrier, shipmentTracking);
					
					if (! response.isSuccess()) {
						// TODO: Determine how to handle this.
						// Most likely we don't want to fail the ATG shipping detail
						// update because Affirm update failed.
						if (isLoggingWarning()) {
							logWarning("Shipping update: Affirm update failed, ATG update succeeded.");
						}
					}

					// update order
					getOrderManager().updateOrder(atgOrder);
					updateOrderCalled = true;

				}

			} catch (Exception e) {
				if (isLoggingError()) {
					logError("Exception while affirm capturing for order " + orderId + ". Marking for rollback and will try again.", e);
				}
				rollback = true;
			} finally {

				if (rollback) {
					try {
						tm.setRollbackOnly();
					} catch (SystemException se) {
						if (isLoggingError())
							logError(se);
					}
				}
				// if we haven't called updateOrder yet, we need to. This
				// ensures that if we're marked for rollback, the order
				// object
				// gets invalidated and the cache entries are cleared.
				if (!updateOrderCalled && atgOrder != null) {
					try {
						getOrderManager().updateOrder(atgOrder);
					} catch (CommerceException ce) {
						if (isLoggingError())
							logError("Exception updating order " + orderId
									+ " in finally block", ce);
					}
				}
			}

			td.end();
		} 
		catch(CommerceException ce){
			if (isLoggingError())
				logError(ce);
		}
		catch (TransactionDemarcationException tde) {
			if (isLoggingError())
				logError(tde);

			rollback = true;
		}

		return !rollback;

	}

	
	
	@SuppressWarnings("unchecked")
	public void updateShippingDetails(OrderImpl atgOrder, ContactInfo shippingAddress, String shipmentCarrier, String shipmentTracking)	{
		
		List<ShippingGroup> shippingGroups = atgOrder.getShippingGroups();
		if (shippingGroups == null || shippingGroups.isEmpty()) {
			if(isLoggingError()) {
				logError("The order:" + atgOrder.getId() + " does not have any shipping group, returning from updateShippingDetails");
			}
		}
		
		// Unknown business logic here. Update first hardgood shipping group
		for (ShippingGroup shippingGroup : shippingGroups) {
			
			if(shippingGroup instanceof HardgoodShippingGroup) {
	        	
				if(! StringUtils.isBlank(shipmentTracking)) {
					((HardgoodShippingGroup) shippingGroup).setTrackingNumber(shipmentTracking);
				}
				if (! StringUtils.isBlank(shipmentCarrier))	{
					((HardgoodShippingGroup) shippingGroup).setPropertyValue("carrierCode",shipmentCarrier);
				}
				((HardgoodShippingGroup) shippingGroup).setShippingAddress(shippingAddress);

			}
		}
		
	}
		
	
	public String getPaymentMethod() {
		return paymentMethod;
	}
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	public StoreOrderManager getOrderManager() {
		return orderManager;
	}
	public void setOrderManager(StoreOrderManager orderManager) {
		this.orderManager = orderManager;
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
	public TransactionManager getTransactionManager() {
		return transactionManager;
	}
	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	public ArrayList<String> getRefundableStates() {
		return refundableStates;
	}
	public void setRefundableStates(ArrayList<String> refundableStates) {
		this.refundableStates = refundableStates;
	}
	
}
