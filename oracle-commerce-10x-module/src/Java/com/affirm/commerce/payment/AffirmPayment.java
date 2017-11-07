package com.affirm.commerce.payment;

import atg.commerce.CommerceException;
import atg.commerce.order.OrderTools;
import atg.commerce.order.PaymentGroupImpl;
import atg.commerce.order.RepositoryAddress;
import atg.commerce.order.RepositoryContactInfo;
import atg.core.util.Address;

import com.affirm.payment.AffirmPaymentConstants;

/**
 * Represents a payment group for Affirm extend base class and inherits basic
 * payment group functionality
 * 
 * @author dev
 * 
 */
public class AffirmPayment extends PaymentGroupImpl {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Address mBillingAddress = null;
	
	public String getAuthTxnId() {
		return (String) getPropertyValue(AffirmPaymentConstants.AUTH_TXN_ID);
	}

	public void setAuthTxnId(String authTxnId) {
		setPropertyValue(AffirmPaymentConstants.AUTH_TXN_ID, authTxnId);
	}
	
	public String getChargeId() {
		return (String) getPropertyValue(AffirmPaymentConstants.CHARGE_ID);
	}

	public void setChargeId(String chargeId) {
		setPropertyValue(AffirmPaymentConstants.CHARGE_ID, chargeId);
	}

	public String getCheckoutToken() {
		return (String) getPropertyValue(AffirmPaymentConstants.CHECKOUT_TOKEN);
	}

	public void setCheckoutToken(String checkoutToken) {
		setPropertyValue(AffirmPaymentConstants.CHECKOUT_TOKEN, checkoutToken);
	}
	
	
	public Address getBillingAddress()
	{
		return mBillingAddress;
	}
	
	public void setBillingAddress(Address pBillingAddress) {
		if (!(pBillingAddress instanceof RepositoryContactInfo) && !(pBillingAddress instanceof RepositoryAddress)
				&& pBillingAddress != null) {
			try {
				if (this.mBillingAddress == null) {
					this.mBillingAddress = (Address) pBillingAddress.getClass().newInstance();
				}

				OrderTools.copyAddress(pBillingAddress, this.mBillingAddress);
			} catch (InstantiationException arg2) {
				throw new RuntimeException(arg2.getMessage());
			} catch (IllegalAccessException arg3) {
				throw new RuntimeException(arg3.getMessage());
			} catch (CommerceException arg4) {
				throw new RuntimeException(arg4.getMessage());
			}
		} else {
			if (this.mBillingAddress != null) {
				this.mBillingAddress.deleteObservers();
			}

			this.mBillingAddress = pBillingAddress;
			this.mBillingAddress.addObserver(this);
		}

		this.setSaveAllProperties(true);
	}

}
