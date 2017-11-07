package com.affirm.commerce.payment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.transaction.Transaction;

import atg.commerce.CommerceException;
import atg.commerce.order.CreditCard;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.InvalidParameterException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderTools;
import atg.commerce.order.PaymentGroup;
import atg.commerce.order.PaymentGroupRelationship;
import atg.commerce.order.RelationshipTypes;
import atg.commerce.order.purchase.ShippingGroupContainerService;
import atg.commerce.util.RepeatingRequestMonitor;
import atg.core.util.Address;
import atg.core.util.StringUtils;
import atg.droplet.DropletException;
import atg.droplet.DropletFormException;
import atg.projects.store.logging.LogUtils;
import atg.projects.store.order.purchase.BillingInfoFormHandler;
import atg.repository.RepositoryItem;
import atg.service.pipeline.PipelineResult;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

/**
 * @author dev
 * 
 */
public class AffirmPaymentFormHandler extends BillingInfoFormHandler {

	protected static final String OP_NAME = "AffirmPaymentFormHandler";
	private String paymentMethod;
	private String checkoutToken;
	private AffirmUtil affirmUtil;
	
	private String cancelErrorUrl;
	private String cancelSuccessUrl;
	
	private ShippingGroupContainerService shippingGroupContainerService;

	/**
	 * Move to confirmation using new billing address and Affirm Payment Group.
	 * 
	 * @param pRequest a <code>DynamoHttpServletRequest</code> value.
	 * @param pResponse a <code>DynamoHttpServletResponse</code> value.
	 * @return a boolean value.
	 * @exception ServletException if an error occurs.
	 * @exception IOException if an error occurs.
	 * @throws CommerceException
	 */
	public boolean handleAffirmPaymentWithNewAddress(
			DynamoHttpServletRequest pRequest,
			DynamoHttpServletResponse pResponse) throws ServletException,
			IOException, CommerceException {

		setUsingProfileCreditCard(false);
		setUsingSavedAddress(false);
		setSaveBillingAddress(false);

		Transaction tr = null;

		try {
			tr = ensureTransaction();

			// Check if session has expired, redirect to sessionExpired URL:
			if (!checkFormRedirect(null, getMoveToConfirmErrorURL(), pRequest, pResponse)) {
				if (isLoggingDebug()) {
					logDebug("Form error at beginning of handleAffirmPaymentWithNewAddress, redirecting.");
				}
				return false;
			}

			synchronized (getOrder()) {

				preAffirmPaymentWithNewAddress(pRequest, pResponse);
				
				if (getFormError()) {
					return moveToConfirmExceptionHandling(pRequest, pResponse);
				}
				
				// Move to Confirm page
				processOrderBilling(pRequest, pResponse);

				if (getFormError()) {
					return moveToConfirmExceptionHandling(pRequest, pResponse);
				}
			}

			postAffirmPaymentWithNewAddress(pRequest, pResponse);

			// synchronized
			return checkFormRedirect(getMoveToConfirmSuccessURL(), getMoveToConfirmErrorURL(), pRequest, pResponse);
			
		} finally {
			if (tr != null) {
				commitTransaction(tr);
			}
		}

		
	}

	/**
	 * Move to confirmation using saved billing address and Affirm Payment
	 * Group.
	 * 
	 * @param pRequest
	 *            a <code>DynamoHttpServletRequest</code> value.
	 * @param pResponse
	 *            a <code>DynamoHttpServletResponse</code> value.
	 * @return a boolean value
	 * @exception ServletException
	 *                if an error occurs.
	 * @exception IOException
	 *                if an error occurs.
	 * @throws CommerceException
	 */
	public boolean handleAffirmPaymentWithSavedAddress(
			DynamoHttpServletRequest pRequest,
			DynamoHttpServletResponse pResponse) throws ServletException,
			IOException, CommerceException {

		setUsingProfileCreditCard(false);
		setUsingSavedAddress(true);
		setSaveBillingAddress(false);
		

		Transaction tr = null;

		try {
			tr = ensureTransaction();

			// Check if session has expired, redirect to sessionExpired URL:
			if (!checkFormRedirect(null, getMoveToConfirmErrorURL(), pRequest, pResponse)) {
				if (isLoggingDebug()) {
					logDebug("Form error at beginning of handleAffirmPaymentWithSavedAddress, redirecting.");
				}
				return false;
			}

			synchronized (getOrder()) {

				preAffirmPaymentWithSavedAddress(pRequest, pResponse);

				if (getFormError()) {
					return moveToConfirmExceptionHandling(pRequest, pResponse);
				}

				// Move to Confirm page
				processOrderBilling(pRequest, pResponse);

				if (getFormError()) {
					return moveToConfirmExceptionHandling(pRequest, pResponse);
				}
			}

			postAffirmPaymentWithSavedAddress(pRequest, pResponse);

			// synchronized
			return checkFormRedirect(getMoveToConfirmSuccessURL(),
					getMoveToConfirmErrorURL(), pRequest, pResponse);
		} finally {
			if (tr != null) {
				commitTransaction(tr);
			}
		}
	}

	/**
	 * Setup credit card payment and validate user input.
	 * 
	 * @param pRequest
	 *            a <code>DynamoHttpServletRequest</code> value.
	 * @param pResponse
	 *            a <code>DynamoHttpServletResponse</code> value.
	 * @exception ServletException
	 *                if an error occurs.
	 * @exception IOException
	 *                if an error occurs.
	 * @throws CommerceException
	 */
	protected void preAffirmPaymentWithSavedAddress(
			DynamoHttpServletRequest pRequest,
			DynamoHttpServletResponse pResponse) throws ServletException,
			IOException, CommerceException {

		// tenderCoupon(pRequest, pResponse);

		if (getFormError()) {
			return;
		}

		if (getOrder().getPriceInfo().getTotal() > 0) {
			// if order's amount is not covered by store credits
			// add credit card payment groups to order

			this.getOrderManager().getPaymentGroupManager().removeAllPaymentGroupsFromOrder(this.getOrder());
			setupAffirmPaymentGroup(pRequest, pResponse, this.getOrder());
			addAffirmPaymentBillingAddress(pRequest, pResponse);

			
			if (getFormError()) {
				return;
			}

		}
	}

	
	/**
	 * Setup credit card payment and validate user input.
	 * 
	 * @param pRequest
	 *            a <code>DynamoHttpServletRequest</code> value.
	 * @param pResponse
	 *            a <code>DynamoHttpServletResponse</code> value.
	 * @exception ServletException
	 *                if an error occurs.
	 * @exception IOException
	 *                if an error occurs.
	 * @throws CommerceException
	 */
	protected void preAffirmPaymentWithNewAddress(
			DynamoHttpServletRequest pRequest,
			DynamoHttpServletResponse pResponse) throws ServletException,
			IOException, CommerceException {

		// tenderCoupon(pRequest, pResponse);

		if (getFormError()) {
			return;
		}

		if (getOrder().getPriceInfo().getTotal() > 0) {
			// if order's amount is not covered by store credits
			// add credit card payment groups to order
			this.getOrderManager().getPaymentGroupManager().removeAllPaymentGroupsFromOrder(this.getOrder());
			setupAffirmPaymentGroup(pRequest, pResponse, this.getOrder());
			AffirmPayment paymentGroup = this.getAffirmUtil().getAffirmPaymentGroup(this.getOrder());
			paymentGroup.setBillingAddress(this.getCreditCardBillingAddress());
			
			if (getFormError()) {
				return;
			}

		}
	}
	
	
	/**
	 * Add newly created card to profile, update checkout progress level.
	 * 
	 * @param pRequest
	 *            a <code>DynamoHttpServletRequest</code> value.
	 * @param pResponse
	 *            a <code>DynamoHttpServletResponse</code> value.
	 * @exception ServletException
	 *                if an error occurs.
	 * @exception IOException
	 *                if an error occurs.
	 */
	protected void postAffirmPaymentWithNewAddress(
			DynamoHttpServletRequest pRequest,
			DynamoHttpServletResponse pResponse) throws ServletException,
			IOException {
		updateCheckoutProgressState();
	}
	
	
	/**
	 * Add newly created card to profile, update checkout progress level.
	 * 
	 * @param pRequest a <code>DynamoHttpServletRequest</code> value.
	 * @param pResponse a <code>DynamoHttpServletResponse</code> value.
	 * @exception ServletException if an error occurs.
	 * @exception IOException if an error occurs.
	 */
	protected void postAffirmPaymentWithSavedAddress(
			DynamoHttpServletRequest pRequest,
			DynamoHttpServletResponse pResponse) throws ServletException,
			IOException {
		updateCheckoutProgressState();
	}


	public void setupAffirmPaymentGroup(DynamoHttpServletRequest request,
			DynamoHttpServletResponse response, Order order)
			throws InvalidParameterException, CommerceException {

		addPaymentGroupRelationships(this.getPaymentMethod(), order);

	}


	//TODO Since we are deleting all the PGs while setting affirm pg, we can remove this method
	//But this can be called from setupAffirmPaymentGroup method in case we want to keep
	//PGs like Store Credit etc.
	@SuppressWarnings("unchecked")
	public boolean removeUnusedRelationships(Order order) throws CommerceException {
		
		List<PaymentGroupRelationship> pgrels = order.getPaymentGroupRelationships();
		PaymentGroup pg = null;
		List<String> removeRels = new ArrayList<String>();
		for (PaymentGroupRelationship pgrel : pgrels) {
			//
			if (pgrel.getRelationshipType() == RelationshipTypes.ORDERAMOUNTREMAINING) {
				pg = pgrel.getPaymentGroup();
				if (pg instanceof CreditCard) {
					removeRels.add(pg.getId());
				}
			}
		}
		
		// now remove relationships
		for (String removeId : removeRels) {
			getOrderManager().getPaymentGroupManager().removeAllRelationshipsFromPaymentGroup(order, removeId);
			getOrderManager().getPaymentGroupManager().removePaymentGroupFromOrder(order, removeId);
		}
		return true;
		
	}

	/**
	 * add pg relationship to order for selected payment type. there should be a
	 * PG for the selected payment type on the order
	 * 
	 * @return
	 * @throws CommerceException
	 */
	protected void addPaymentGroupRelationships(String paymentType, Order order) throws CommerceException {
		
		PaymentGroup pg = null;
		pg = findPGByPaymentType(order, paymentType);

		if (pg == null) {
			pg = getOrderManager().getPaymentGroupManager().createPaymentGroup(paymentType);
			getOrderManager().getPaymentGroupManager().addPaymentGroupToOrder(order, pg);
		}

		if (null != pg) {
			getOrderManager().addRemainingOrderAmountToPaymentGroup(order, pg.getId());
		}
		
	}

	/**
	 * @param paymentType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected PaymentGroup findPGByPaymentType(Order order, String paymentType) {
		
		List<PaymentGroup> pgList = order.getPaymentGroups();
		PaymentGroup pg = null;
		for (PaymentGroup paymentGroup : pgList) {
			if (paymentGroup.getPaymentGroupClassType().equals(paymentType)) {
				pg = paymentGroup;
				break;
			}
		}
		return pg;
		
	}

	/**
	 * confirms affirm payment
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public boolean handleAffirmConfirmPayment(DynamoHttpServletRequest request,	DynamoHttpServletResponse response) 
			throws ServletException, IOException {
		
		RepeatingRequestMonitor rrm = this.getRepeatingRequestMonitor();
		final String myHandleMethod = "AffirmPaymentInfoFormHandler.handleAffirmConfirmPayment";
		if (rrm == null || rrm.isUniqueRequestEntry(myHandleMethod)) {
			try {
				return this.executeAffirmConfirmPayment(request, response);
			} catch (CommerceException e) {
				if(isLoggingError()){
					logError("Exception in affirmConfirmPayment for order: " + getOrder().getId(), e);
				}
				return false;
			} finally {
				if (rrm != null) {
					rrm.removeRequestEntry(myHandleMethod);
				}
			}
		} else {
			if (isLoggingInfo()) {
				logInfo("Detected user double-submission for " + myHandleMethod
						+ ", ignoring the duplicate request.");
			}
			return false;
		}
		
	}
	
	
	
	/**
	 * confirms affirm payment
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public boolean handleCancelAffirmPayment(DynamoHttpServletRequest request,
			DynamoHttpServletResponse response) throws ServletException,
			IOException {
		RepeatingRequestMonitor rrm = this.getRepeatingRequestMonitor();
		final String myHandleMethod = "AffirmPaymentInfoFormHandler.handleCancelAffirmPayment";
		if (rrm == null || rrm.isUniqueRequestEntry(myHandleMethod)) {
			try {
				return this.executeCancelAffirmPayment(request, response);
			} catch (CommerceException e) {
				if(isLoggingError()){
					logError("Exception in cancelAffirmPayment for order: " + getOrder().getId(), e);
				}
				return false;
			} finally {
				if (rrm != null) {
					rrm.removeRequestEntry(myHandleMethod);
				}
			}
		} else {
			if (isLoggingInfo()) {
				logInfo("Detected user double-submission for " + myHandleMethod
						+ ", ignoring the duplicate request.");
			}
			return false;
		}
	}

	
	/**
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws CommerceException 
	 */
	private boolean executeCancelAffirmPayment(
			DynamoHttpServletRequest request, DynamoHttpServletResponse response)
			throws ServletException, IOException, CommerceException {
		if (isLoggingDebug()) {
			logDebug("handleCancelAffiirmPayment() called from affirm callback jsp for order# "
					+ getOrder().getId());
		}

		// cart express checkout orders validate token earlier in the flow
		if (getAffirmUtil().isAffirmCheckout(this.getOrder())) {
			AffirmPayment pg = (AffirmPayment) this.findPGByPaymentType(
					this.getOrder(), this.getPaymentMethod());
			
			if(pg != null){
				this.getOrderManager().getPaymentGroupManager().removePaymentGroupFromOrder(this.getOrder(), pg.getId());
				this.getOrderManager().updateOrder(this.getOrder());

			}
		}

		return checkFormRedirect(this.getCancelSuccessUrl(),
				this.getCancelErrorUrl(), request, response);
	}
	
	/**
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws CommerceException 
	 */
	private boolean executeAffirmConfirmPayment(
			DynamoHttpServletRequest request, DynamoHttpServletResponse response)
			throws ServletException, IOException, CommerceException {
		if (isLoggingDebug()) {
			logDebug("handleAffiirmConfirmPayment() called from affirm callback jsp for order# "
					+ getOrder().getId());
		}

		// cart express checkout orders validate token earlier in the flow
		if (getAffirmUtil().isAffirmCheckout(this.getOrder())) {
			if (StringUtils.isBlank(this.getCheckoutToken())) {

				addFormException(new DropletException(
						"Affirm checkout on order " + this.getOrder().getId()
								+ " without checkout token"));
				return checkFormRedirect(getMoveToConfirmSuccessURL(),
						getMoveToConfirmErrorURL(), request, response);
			}
		} else {
			addFormException(new DropletException(
					"Not affirm checkout on order " + this.getOrder().getId()));
			return checkFormRedirect(getMoveToConfirmSuccessURL(),
					getMoveToConfirmErrorURL(), request, response);

		}
		// TODO Do we need any validations now? Add validation and Form Redirect
		// code here
		
		//Auth shoudl happen in the payment processing pipeline
		//this.completeAffirmPaymentOnOrder(request, response);
		
		AffirmPayment pg = (AffirmPayment) this.findPGByPaymentType(
				this.getOrder(), this.getPaymentMethod());
		pg.setCheckoutToken(this.getCheckoutToken());
		this.getOrderManager().updateOrder(this.getOrder());
		
		
		doCommitOrder(this.getOrder(), request, response);

		return checkFormRedirect(getMoveToConfirmSuccessURL(),
				getMoveToConfirmErrorURL(), request, response);
	}

	
	
	/**
	 * Calls commitOrder
	 * @param order
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doCommitOrder(Order order, DynamoHttpServletRequest request,
			DynamoHttpServletResponse response) throws ServletException,
			IOException {
		String orderId = null;
		try {
			orderId = order.getId();
			if (isLoggingInfo()) {
				logInfo("In doCommitOrder() for orderId: " + orderId);
			}
			// Need any validation? validateOrder(request, response);
			commitOrder(order, request, response);
			if (!getFormError()) {
				// any post commit should go here
			}
			if (isLoggingInfo()) {
				logInfo("Done doCommitOrder() for orderId: " + orderId);
			}
		} catch (Exception e) {

			// TODO should we re-initialize payment?

			if (isLoggingError()) {
				logError("Exception while running doCommit()  -- see exception below,  orderId: "
						+ orderId);
				logError(LogUtils.formatMajor(""), e);
				logError(LogUtils.formatMajor("Error message from exception: "
						+ e.getMessage()));
			}
			// process exception
			processException(e, MSG_ERROR_MOVE_TO_CONFIRM, request, response);
		}
	}

	/**
	 * This method is called between <code>preCommitOrder</code> and
	 * <code>postCommitOrder</code>. This method calls the
	 * <code>getProcessOrderMap(Locale)</code> to get the process order map and
	 * calls the Order Manager to process the order.
	 * 
	 * @param pOrder
	 * @param pRequest
	 * @param pResponse
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	protected void commitOrder(Order pOrder, DynamoHttpServletRequest pRequest,
			DynamoHttpServletResponse pResponse) throws ServletException,
			IOException {
		String orderId = pOrder.getId();
		boolean isPiplineError = false;
		boolean isPiplineException = false;
		try {

			HashMap<String, Object> extraParams = this.getOrderManager()
					.getProcessOrderMap(getUserLocale(), null);
			extraParams.put("profile", getProfile());

			PipelineResult result = null;

			result = getOrderManager().processOrder(pOrder, extraParams);

			logPipelineErrors(result, orderId);
			if (!processPipelineErrors(result)
					&& (!isTransactionMarkedAsRollBack())) {
				if (getShoppingCart() != null) {
					getShoppingCart().setLast(pOrder);
					getShoppingCart().setCurrent(null);
					
				    // Wipe out the shipping addresses stored in the shipping group map for security reasons.
				    if(getProfile().isTransient()) {
				      getShippingGroupContainerService().removeAllShippingGroups();
				    }
					
				}
				if (isLoggingInfo()) {
					logInfo("In commitOrder processsOrder success for orderId: "
							+ orderId);
				}
			} else {
				if (isLoggingError()) {
					logError("commitOrder processOrder() returned pipeline errors, see above for pipeline key/value errors for orderId: "
							+ orderId);
				}

				isPiplineError = true;
			}

			if (isLoggingInfo()) {
				logInfo("Done commitOrder for orderId: " + orderId);
			}
		} catch (Exception exc) {
			isPiplineException = true;
			if (isLoggingError()) {
				logError(
						"commitOrder failed (see exception below) - for orderId: "
								+ orderId, exc);
			}
			addFormException(new DropletException("Error Committing order"));
		} finally {
			if (isPiplineError || isPiplineException) {
				if (isLoggingError()) {
					logError("commitOrder isPiplineError = " + isPiplineError
							+ " isPiplineException = " + isPiplineException
							+ " , calling doVoidAuth for orderId: " + orderId);
				}
				// TODO should we void in case of exception
				// doRevertPayment(pOrder);
			}
		}
	}

	/**
	 * logs the raw key/value pair from the pipeline ATG's
	 * processPiplelineErrors uses loggingdebug of this formhandler's instance,
	 * if we turn on debugging on this formhandler in production there would bee
	 * too much logging so we log the raw key/value here and let atg code
	 * process actual pipeline errors
	 * 
	 * @param result
	 * @param orderId
	 */
	protected void logPipelineErrors(PipelineResult result, String orderId) {
		if (result != null && result.hasErrors()) {
			Object[] errorKeys = result.getErrorKeys();
			if (errorKeys != null) {
				for (int index = 0; index < errorKeys.length; index++) {
					Object error = result.getError(errorKeys[index]);
					if (isLoggingError()) {
						logError("PipelineError: key=" + errorKeys[index]
								+ "; error=" + error + " ;orderId: " + orderId);
					}
				}
			}
		}
	}
	
	 /**
	   * This method add and validates the billing Address to the credit card if order payment
	   * is payed by the credit card.
	   *
	   * @param pRequest
	   *          a <code>DynamoHttpServletRequest</code> value.
	   * @param pResponse
	   *          a <code>DynamoHttpServletResponse</code> value.
	   * @exception ServletException
	   *              if an error occurs.
	   * @exception IOException
	   *              if an error occurs.
	   */
	public void addAffirmPaymentBillingAddress(DynamoHttpServletRequest pRequest, DynamoHttpServletResponse pResponse)
			throws ServletException, IOException {

		try {

			// If the user chooses a profile address, copy it to the affirmpayment group
			AffirmPayment pg = getAffirmUtil().getAffirmPaymentGroup(getOrder());
			addBillingAddressToAffirmPayment(pg, getStoredAddressSelection(), getProfile(), getOrder());
			
		} catch (CommerceException ce) {
			addFormException(new DropletFormException(ce.getMessage(),null));
		}
		
	}
	

	/**
	 * This method checks to see if the user chose a profile address. If so, the
	 * address is copied from the address book to the affirm payment.
	 * 
	 * @param pAffirmPayment the card.
	 * @param pStoredAddressSelection the stored address selection.
	 * @param pProfile the profile.
	 * @param pOrder the order.
	 * 
	 * @throws CommerceException indicates that a severe error occured while performing a commerce operation.
	 */
	@SuppressWarnings("rawtypes")
	protected void addBillingAddressToAffirmPayment(AffirmPayment pAffirmPayment,
			String pStoredAddressSelection,
			RepositoryItem pProfile, 
			Order pOrder)
					throws CommerceException {

		if (isLoggingDebug()) {
			logDebug("Copying address: " + pStoredAddressSelection + " to affirm payment.");
		}

		// User chose a stored address, copy it from the address book to the current payment group.
		try {

			String firstname = null, middlename = null, lastname = null;

			// Look in the  the ShippingGroupContainerService.shippingGroupMap for addresses.
			Map addresses = getShippingGroupContainerService().getShippingGroupMap();
			Object shippingGroup = addresses.get(pStoredAddressSelection);

			if(shippingGroup instanceof HardgoodShippingGroup) {
				Address storedAddress = ((HardgoodShippingGroup) shippingGroup).getShippingAddress();

				// Save the name
				firstname = storedAddress.getFirstName();
				middlename = storedAddress.getMiddleName();
				lastname = storedAddress.getLastName();

				// Copy address
				OrderTools.copyAddress(storedAddress, pAffirmPayment.getBillingAddress());
			}

			// Copy preserved details
			pAffirmPayment.getBillingAddress().setFirstName(firstname);
			pAffirmPayment.getBillingAddress().setMiddleName(middlename);
			pAffirmPayment.getBillingAddress().setLastName(lastname);

		} 
		catch (CommerceException ce) {
			if (isLoggingError()) {
				logError(LogUtils.formatMajor("Error copying address: "), ce);
			}
			throw ce;
		}

	}

	  
	
	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getCheckoutToken() {
		return checkoutToken;
	}

	public void setCheckoutToken(String checkoutToken) {
		this.checkoutToken = checkoutToken;
	}

	public AffirmUtil getAffirmUtil() {
		return affirmUtil;
	}

	public void setAffirmUtil(AffirmUtil affirmUtil) {
		this.affirmUtil = affirmUtil;
	}

	public ShippingGroupContainerService getShippingGroupContainerService() {
		return shippingGroupContainerService;
	}

	public void setShippingGroupContainerService(
			ShippingGroupContainerService shippingGroupContainerService) {
		this.shippingGroupContainerService = shippingGroupContainerService;
	}

	public String getCancelErrorUrl() {
		return cancelErrorUrl;
	}

	public void setCancelErrorUrl(String cancelErrorUrl) {
		this.cancelErrorUrl = cancelErrorUrl;
	}

	public String getCancelSuccessUrl() {
		return cancelSuccessUrl;
	}

	public void setCancelSuccessUrl(String cancelSuccessUrl) {
		this.cancelSuccessUrl = cancelSuccessUrl;
	}

}
