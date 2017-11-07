package com.affirm.commerce.payment;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderImpl;
import atg.commerce.order.OrderManager;
import atg.commerce.order.PaymentGroup;
import atg.commerce.payment.PaymentManager;
import atg.commerce.states.PaymentGroupStates;
import atg.commerce.states.StateDefinitions;
import atg.core.util.ContactInfo;
import atg.core.util.StringUtils;
import atg.droplet.DropletException;
import atg.droplet.GenericFormHandler;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import com.affirm.payment.AffirmPaymentGroupStates;

public class AffirmProcessingFormHandler extends GenericFormHandler {

	private String[] orderIds;
	private String orderId;
	private double refundAmount;
	
	public String shipmentCarrier;
	public String shipmentTracking;
	
	private ContactInfo address = new ContactInfo();
	
	private ArrayList<String> refundableStates = new ArrayList<String>();

	private String successUrl;
	private String errorUrl;

	private TransactionManager transactionManager;
	private OrderManager orderManager;
	private PaymentManager paymentManager;
	private AffirmUtil affirmUtil;
	
	private AffirmProcessingManager affirmProcessingManager;

	/**
	 * Method is invoked from the form and captures the previous authorized
	 * payment
	 * 
	 * @param pRequest
	 * @param pResponse
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws CommerceException
	 */
	public boolean handleCapture(DynamoHttpServletRequest pRequest, DynamoHttpServletResponse pResponse) 
			throws ServletException, IOException {
		
		Order atgOrder = loadOrder();

		if (atgOrder == null || getFormError()) {
			return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		}
		
		validatePaymentGroupForSettlement(atgOrder);
		
		if (getFormError()) {
			return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		}

		TransactionManager tm = getTransactionManager();
		TransactionDemarcation td = new TransactionDemarcation();
		boolean rollback = false;
		boolean updateOrderCalled = false;

		try {
			
			td.begin(tm, TransactionDemarcation.REQUIRED);
			try {

				// synchronize on the order
				synchronized (atgOrder) {

					AffirmPayment paymentGroup = getAffirmUtil().getAffirmPaymentGroup(atgOrder);

					if (paymentGroup != null) {
						settlePaymentGroup(paymentGroup, atgOrder);
					}

					// update order
					getOrderManager().updateOrder(atgOrder);
					updateOrderCalled = true;
				}

			} catch (CommerceException e) {
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
				// ensures that if we're marked for rollback, the order object
				// gets invalidated and the cache entries are cleared.
				if (!updateOrderCalled && atgOrder != null) {
					try {
						getOrderManager().updateOrder(atgOrder);
					} catch (CommerceException ce) {
						if (isLoggingError())
							logError("Exception updating order " + getOrderId() + " in finally block", ce);
					}
				}
			}

			td.end();
		} catch (TransactionDemarcationException tde) {
			if (isLoggingError()) {
				logError(tde);
			}

			addFormException(new DropletException("System Error while capture for order: " + orderId));

		}

		return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		
	}

	public void validatePaymentGroupForSettlement(Order atgOrder) {
		
		PaymentGroup affirmPaymentGroup = getAffirmUtil().getAffirmPaymentGroup(atgOrder);
		if (affirmPaymentGroup.getState() == getPaymentGroupStates().getStateValue(PaymentGroupStates.SETTLED)) {
			addFormException(new DropletException("Payment group is already settled. Not trying to settle again."));
			if (isLoggingWarning()) {
				logWarning("Payment group " + affirmPaymentGroup.getId() + " in order " + orderId + " is already settled");
			}
		}
		
	}

	private Order loadOrder() {
		
		if (StringUtils.isBlank(this.getOrderId())) {
			addFormException(new DropletException("No order id passed"));
			return null;
		}
		
		Order atgOrder = null;
		try {
			atgOrder = (OrderImpl) getOrderManager().loadOrder(getOrderId());
		} catch (CommerceException ce) {
			if (isLoggingError()) {
				logError(ce);
			}
			addFormException(new DropletException("Failed to load order with id: " + getOrderId()));
			return null;
		}

		if (atgOrder == null) {
			if (isLoggingError()) {
				logError("Could not load order from the repository for id:" + getOrderId());
			}
			addFormException(new DropletException("Order could not be loaded"));
		}
		
		return atgOrder;
	}

	/**
	 * Method is invoked from the form and voids the previous authorized payment
	 * 
	 * @param pRequest
	 * @param pResponse
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws CommerceException
	 */
	public boolean handleVoid(DynamoHttpServletRequest pRequest, DynamoHttpServletResponse pResponse) 
			throws ServletException, IOException {

		Order atgOrder = loadOrder();

		if (atgOrder == null || getFormError()) {
			return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		}
		
		validateOrderForVoid(atgOrder);
		
		if (getFormError()) {
			return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		}
		
		TransactionManager tm = getTransactionManager();
		TransactionDemarcation td = new TransactionDemarcation();
		boolean rollback = false;
		boolean updateOrderCalled = false;
		
		try {
			
			td.begin(tm, TransactionDemarcation.REQUIRED);
			try {

				// now synchronize on the order
				synchronized (atgOrder) {

					// get AffirmPaymentGroup
					AffirmPayment paymentGroup = getAffirmUtil().getAffirmPaymentGroup(atgOrder);

					if (paymentGroup != null) {
						
						voidPaymentGroup(paymentGroup, atgOrder);
						// if successful, set PG state
						this.setPaymentGroupState(paymentGroup, atgOrder,
								AffirmPaymentGroupStates.VOID,
								"The authorization was voided");

					}
					
					// update order
					getOrderManager().updateOrder(atgOrder);
					updateOrderCalled = true;
					
				}

			} catch (CommerceException ce) {
				if (isLoggingError()) {
					logError("Exception while calling void for order " + getOrderId()
							+ ". Marking for rollback and will try again.", ce);
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
				// if we haven't called updateOrder yet, we need to. This ensures that if we're marked for rollback, the order
				// object gets invalidated and the cache entries are cleared.
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

		} catch (TransactionDemarcationException tde) {

			if (isLoggingError()) {
				logError(tde);
			}

			addFormException(new DropletException("System Error while voiding the Auth for order: " + orderId));

		}
		
		return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		
	}

	/**
	 * Adds form exceptions if there is no Affirm PaymentGroup or if state is anything other than AUTHORIZED
	 * 
	 * @param order
	 */
	public void validateOrderForVoid(Order order) {
		
		AffirmPayment affirmPaymentGroup = getAffirmUtil().getAffirmPaymentGroup(order);
		if (affirmPaymentGroup == null) {
			addFormException(new DropletException("No Affirm PaymentGroup found on order: " + order.getId()));
			return;
		}
		
		// If payment group is anything other than in "AUTHORIZED" state, don't allow void
		if (affirmPaymentGroup.getState() != getPaymentGroupStates().getStateValue(PaymentGroupStates.AUTHORIZED)) {
			addFormException(new DropletException("Payment group is not in authorized state. Can not void."));
		}
		
	}

	/**
	 * Method is invoked from the form and voids the previous authorized payment
	 * 
	 * @param pRequest
	 * @param pResponse
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws CommerceException
	 */
	public boolean handleRefund(DynamoHttpServletRequest pRequest, DynamoHttpServletResponse pResponse)
			throws ServletException, IOException {
		
		Order atgOrder = loadOrder();
		if (atgOrder == null || getFormError()) {
			return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		}
		
		validateRefundAffirmInput(atgOrder);

		if (getFormError()) {
			return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		}
		
		TransactionManager tm = getTransactionManager();
		TransactionDemarcation td = new TransactionDemarcation();
		boolean rollback = false;
		boolean updateOrderCalled = false;

		try {
			
			td.begin(tm, TransactionDemarcation.REQUIRED);
			try {
				
				AffirmPayment paymentGroup = getAffirmUtil().getAffirmPaymentGroup(atgOrder);
				
				double calculatedRefundAmount = 0.0;
				if (getRefundAmount() > 0.0) {
					calculatedRefundAmount = getRefundAmount();
				} else {
					calculatedRefundAmount = paymentGroup.getAmount();
				}
				
				// now synchronize on the order
				synchronized (atgOrder) {

					creditPaymentGroup(paymentGroup, atgOrder, calculatedRefundAmount);
					// if successful, set PG state

					if (paymentGroup.getAmountAuthorized() - paymentGroup.getAmountCredited() > 0) {
						setPaymentGroupState(paymentGroup, atgOrder, AffirmPaymentGroupStates.PARTIALLY_REFUNDED, "The amount was partially refunded");
					} else {
						setPaymentGroupState(paymentGroup, atgOrder, AffirmPaymentGroupStates.REFUNDED, "The amount was refunded");
					}

					// update order
					getOrderManager().updateOrder(atgOrder);
					updateOrderCalled = true;

				}

			} catch (CommerceException ce) {
				if (isLoggingError()) {
					logError("Exception while calling credit for order " + getOrderId() + ". Marking for rollback and will try again.", ce);
				}
				rollback = true;
			} finally {

				if (rollback) {
					try {
						tm.setRollbackOnly();
					} catch (SystemException se) {
						if (isLoggingError()) {
							logError(se);
						}
					}
				}
				
				// If we haven't called updateOrder yet, we need to. This
				// ensures that if we're marked for rollback, the order object
				// gets invalidated and the cache entries are cleared.
				if (!updateOrderCalled && atgOrder != null) {
					try {
						getOrderManager().updateOrder(atgOrder);
					} catch (CommerceException ce) {
						if (isLoggingError()) {
							logError("Exception updating order " + getOrderId() + " in finally block", ce);
						}
					}
				}
			}

			td.end();
			
		} catch (TransactionDemarcationException tde) {
			
			if (isLoggingError()) {
				logError(tde);
			}
			addFormException(new DropletException("System Error while refunding the amount for order: "	+ getOrderId()));

		}

		return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		
	}

	public void validateRefundAffirmInput(Order order) {

		AffirmPayment paymentGroup = getAffirmUtil().getAffirmPaymentGroup(order);

		if (paymentGroup == null) {
			addFormException(new DropletException("Payment Group could not be found on the order:" + order.getId()));
		}
		
		if (!this.getRefundableStates().contains(paymentGroup.getStateAsString())) {
			if (isLoggingInfo()) {
				logInfo("Payment group " + paymentGroup.getId() + " in order " + getOrderId()
						+" has state: " + paymentGroup.getStateAsString() + " which is not refundable.");
			}

			addFormException(new DropletException("Payment group "
					+ paymentGroup.getId() + " in order " + orderId
					+ " is not in refundable state"));
		}
		
		double currentAmountCredited = paymentGroup.getAmountCredited();
		double currentAmountDebited = paymentGroup.getAmountDebited();
		double amountRemainingToCredit = currentAmountDebited - currentAmountCredited;
		
		if (getRefundAmount() > amountRemainingToCredit) {
			if (isLoggingError()) {
				logError("Tried to refund more than amount on payment group.");
			}
			addFormException(new DropletException("Tried to refund more than amount on payment group. Amount: "
					+ getRefundAmount() + ". Amount left to credit: " + amountRemainingToCredit));
		}
		
	}

	/**
	 * Attempt to settle the payment group, unless it is already settled.
	 * 
	 * @param pPaymentGroup
	 *            the payment group to settle
	 * @param pOrder
	 *            the order to which the payment group belongs
	 * @param pOrderUpdateState
	 *            the OrderUpdateState
	 */
	public void settlePaymentGroup(PaymentGroup pPaymentGroup, Order pOrder) {
		
		String orderId = pOrder.getId();
		String pgId = pPaymentGroup.getId();

		if (isLoggingInfo()) {
			logInfo("Debiting unsettled payment group "
					+ pgId
					+ " with state "
					+ getPaymentGroupStates().getStateString(
							pPaymentGroup.getState()) + " for order " + orderId);
		}

		try {
			getPaymentManager().debit(pOrder, pPaymentGroup);
		} catch (CommerceException ce) {
			if (isLoggingError()) {
				logError("Exception debiting paymentGroup " + pgId + " for order " + orderId, ce);
			}
		}
		
	}

	/**
	 * Attempt to void the payment group, unless it is already settled.
	 * 
	 * @param pPaymentGroup
	 *            the payment group to settle
	 * @param pOrder
	 *            the order to which the payment group belongs
	 * @param pOrderUpdateState
	 *            the OrderUpdateState
	 */
	public void voidPaymentGroup(PaymentGroup pPaymentGroup, Order pOrder) {
		
		String orderId = pOrder.getId();
		String pgId = pPaymentGroup.getId();

		if (pPaymentGroup.getState() != getPaymentGroupStates().getStateValue(PaymentGroupStates.AUTHORIZED)) {
			if (isLoggingWarning()) {
				logWarning("Payment group " + pgId + " in order " + orderId + " is not in authorized state, can not be voided");
			}
			return;
		}
		
		if (isLoggingInfo()) {
			logInfo("Voiding unsettled payment group "
					+ pgId
					+ " with state "
					+ getPaymentGroupStates().getStateString(
							pPaymentGroup.getState()) + " for order " + orderId);
		}

		try {
			
			getPaymentManager().decreaseAuthorization(pOrder, pPaymentGroup, pPaymentGroup.getAmount());
			
		} catch (CommerceException ce) {
			if (isLoggingError()) {
				logError("Exception voiding paymentGroup " + pgId + " for order " + orderId, ce);
			}
		}
	}

	/**
	 * Attempt to refund the payment group
	 * 
	 * @param pPaymentGroup
	 *            the payment group to settle
	 * @param pOrder
	 *            the order to which the payment group belongs
	 * @param pOrderUpdateState
	 *            the OrderUpdateState
	 */
	public void creditPaymentGroup(PaymentGroup pPaymentGroup, Order pOrder, double pRefundAmount) {
		
		String orderId = pOrder.getId();
		String pgId = pPaymentGroup.getId();

		if (isLoggingInfo()) {
			logInfo("Refunding settled payment group " + pgId + " with state "
					+ getPaymentGroupStates().getStateString(pPaymentGroup.getState()) + " for order " + orderId);
		}

		if (isLoggingDebug()) {
			logDebug("Refund amount for the order:" + orderId + ", = "
					+ refundAmount + ", total PayGroup Amount = "
					+ pPaymentGroup.getAmount());
		}

		try {
			getPaymentManager().credit(pOrder, pPaymentGroup, pRefundAmount);
		} catch (CommerceException ce) {
			if (isLoggingError()) {
				logError("Exception refunding paymentGroup " + pgId
						+ " for order " + orderId, ce);
			}
		}
		
	}

	/**
	 * Sets the payment group state and logs an info message
	 * 
	 * @param pPaymentGroup
	 *            the payment group whose state should be set
	 * @param pOrder
	 *            the order to which the payment group belongs
	 * @param pNewState
	 *            the new state
	 * @param pReason
	 *            the reason the state is being set
	 */
	private void setPaymentGroupState(PaymentGroup pPaymentGroup, Order pOrder, String pNewState, String pReason) {
		
		int newStateValue = getPaymentGroupStates().getStateValue(pNewState);
		// log what we're doing
		if (isLoggingInfo())
			logInfo((pPaymentGroup.getState() != newStateValue ? "Changing state of"
					: "Same state for")
					+ " payment group "
					+ pPaymentGroup.getId()
					+ " in order "
					+ pOrder.getId()
					+ " from "
					+ getPaymentGroupStates().getStateString(
							pPaymentGroup.getState()).toUpperCase()
					+ " to "
					+ pNewState.toUpperCase()
					+ (pReason != null ? ", for reason: " + pReason : ""));
		if (pPaymentGroup.getState() != newStateValue) {
			// set the state
			pPaymentGroup.setState(newStateValue);
		}
		
	}


	/**
	 * Method is invoked from the form and updates shipping details on order and Affirm
	 * payment
	 * 
	 * @param pRequest
	 * @param pResponse
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws CommerceException
	 */
	public boolean handleUpdateShipping(DynamoHttpServletRequest pRequest, DynamoHttpServletResponse pResponse) 
			throws ServletException, IOException, CommerceException {
		
		boolean update = this.getAffirmProcessingManager().updateShippingDetails(this.getOrderId(), this.getAddress(), this.getShipmentCarrier(), this.getShipmentTracking());
		
		if(!update)	{
			addFormException(new DropletException("Generic error message while updating order: " + this.getOrderId()));
		}
		
		return checkFormRedirect(getSuccessUrl(), getErrorUrl(), pRequest, pResponse);
		
	}
	
	
	
	public String[] getOrderIds() {
		return orderIds;
	}

	public void setOrderIds(String[] orderIds) {
		this.orderIds = orderIds;
	}

	public String getSuccessUrl() {
		return successUrl;
	}

	public void setSuccessUrl(String successUrl) {
		this.successUrl = successUrl;
	}

	public String getErrorUrl() {
		return errorUrl;
	}

	public void setErrorUrl(String errorUrl) {
		this.errorUrl = errorUrl;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public OrderManager getOrderManager() {
		return orderManager;
	}

	public void setOrderManager(OrderManager orderManager) {
		this.orderManager = orderManager;
	}

	private PaymentGroupStates getPaymentGroupStates() {
		return StateDefinitions.PAYMENTGROUPSTATES;
	}

	public PaymentManager getPaymentManager() {
		return paymentManager;
	}

	public void setPaymentManager(PaymentManager paymentManager) {
		this.paymentManager = paymentManager;
	}

	public double getRefundAmount() {
		return refundAmount;
	}

	public void setRefundAmount(double refundAmount) {
		this.refundAmount = refundAmount;
	}

	public ArrayList<String> getRefundableStates() {
		return refundableStates;
	}

	public void setRefundableStates(ArrayList<String> refundableStates) {
		this.refundableStates = refundableStates;
	}

	public String getShipmentCarrier() {
		return shipmentCarrier;
	}

	public void setShipmentCarrier(String shipmentCarrier) {
		this.shipmentCarrier = shipmentCarrier;
	}

	public String getShipmentTracking() {
		return shipmentTracking;
	}

	public void setShipmentTracking(String shipmentTracking) {
		this.shipmentTracking = shipmentTracking;
	}

	public AffirmProcessingManager getAffirmProcessingManager() {
		return affirmProcessingManager;
	}

	public void setAffirmProcessingManager(
			AffirmProcessingManager affirmProcessingManager) {
		this.affirmProcessingManager = affirmProcessingManager;
	}

	public ContactInfo getAddress() {
		return address;
	}

	public void setAddress(ContactInfo address) {
		this.address = address;
	}

	public AffirmUtil getAffirmUtil() {
		return affirmUtil;
	}

	public void setAffirmUtil(AffirmUtil affirmUtil) {
		this.affirmUtil = affirmUtil;
	}
	
	

}
