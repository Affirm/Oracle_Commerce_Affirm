package com.affirm.commerce.payment;

import java.io.IOException;

import javax.servlet.ServletException;

import com.affirm.commerce.payment.response.AffirmResponse;

import atg.commerce.CommerceException;
import atg.commerce.order.purchase.PurchaseProcessFormHandler;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

public class AffirmCheckoutFormHandler extends PurchaseProcessFormHandler {
	
	private AffirmUtil affirmUtil;
	private String affirmSuccessUrl;
	private String affirmFailureUrl;
	
	public boolean handleAffirmCheckout(DynamoHttpServletRequest request, DynamoHttpServletResponse response) 
			throws ServletException, IOException, CommerceException {

		AffirmResponse affirmResponse = getAffirmUtil().startCheckout(getOrder());
		
		if (affirmResponse.isSuccess()) {
			setAffirmSuccessUrl(affirmResponse.getRedirectUrl());
		}
		
		return checkFormRedirect(getAffirmSuccessUrl(), getAffirmFailureUrl(), request, response);
		
	}

	public AffirmUtil getAffirmUtil() {
		return affirmUtil;
	}

	public void setAffirmUtil(AffirmUtil affirmUtil) {
		this.affirmUtil = affirmUtil;
	}

	public String getAffirmSuccessUrl() {
		return affirmSuccessUrl;
	}

	public void setAffirmSuccessUrl(String affirmSuccessUrl) {
		this.affirmSuccessUrl = affirmSuccessUrl;
	}

	public String getAffirmFailureUrl() {
		return affirmFailureUrl;
	}

	public void setAffirmFailureUrl(String affirmFailureUrl) {
		this.affirmFailureUrl = affirmFailureUrl;
	}

}
