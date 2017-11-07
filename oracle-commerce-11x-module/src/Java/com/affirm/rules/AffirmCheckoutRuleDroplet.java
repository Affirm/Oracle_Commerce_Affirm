package com.affirm.rules;

import java.io.IOException;

import javax.servlet.ServletException;

import com.affirm.commerce.payment.AffirmPaymentConfiguration;

import atg.commerce.order.Order;
import atg.nucleus.naming.ParameterName;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.DynamoServlet;

public class AffirmCheckoutRuleDroplet extends DynamoServlet implements AffirmRuleConstants {
	
	private static final ParameterName OPARAM_OUTPUT =  ParameterName.getParameterName("output");
	private static final ParameterName OPARAM_EMPTY =  ParameterName.getParameterName("empty");
	private static final ParameterName OPARAM_ERROR =  ParameterName.getParameterName("error");

	private static final String DATA_PROMO_ID_PARAM = "dataPromoId";
	private static final String DATA_AMOUNT_PARAM = "dataAmount";
	private static final String ERROR_MESSAGE = "errorMessage";
	
	private static final ParameterName INPUT_PARAM_ORDER =  ParameterName.getParameterName("order");
	
	public AffirmRuleManager affirmRuleManager;
	public AffirmPaymentConfiguration affirmConfiguration;
	private String defaultFinancialProductKey;
	
	public void service(DynamoHttpServletRequest request, DynamoHttpServletResponse response)
			throws ServletException, IOException {
		
		if (! getAffirmConfiguration().isEnabled()) {
			if (isLoggingDebug()) {
				logDebug("Affirm not enabled.");
			}
			request.serviceParameter(OPARAM_EMPTY, request, response);
			return;
		}
		
		Order order = (Order)request.getObjectParameter(INPUT_PARAM_ORDER);
		if(order == null){
			request.setParameter(ERROR_MESSAGE, "No Order passed");
			request.serviceParameter(OPARAM_ERROR, request, response);
			return;			
		}
		
		RepositoryItem rule = getAffirmRuleManager().findAffirmRuleForCheckout(order);
		
		if (rule == null) {
			if (isLoggingDebug()) {
				logDebug("No matching checkout rule found. Getting default rule.");
			}
			rule = getAffirmRuleManager().getDefaultAffirmRule();
		}
		
		String financingProgramId = "";
		String dataPromoId = "";
		
		if (rule == null) {
			// No rule found based on order, and no default rule. Therefore, we don't have a financing
			// program or data promo id, but we still want to show the order total in the data amount.
			if (isLoggingWarning()) {
				logWarning("No Affirm rules found for this order: " + order.getId() + " and no default rule defined.");
			}
		} else {
			financingProgramId = (String)rule.getPropertyValue(PROPERTY_PROGRAM_ID);
			dataPromoId = (String)rule.getPropertyValue(PROPERTY_DATA_PROMO_ID);
		}
		
		
		double cartTotal = getAffirmRuleManager().findCartTotal(order);
		cartTotal = cartTotal * 100;

		if(isLoggingDebug()){
			logDebug("Financing program id for order: " + order.getId() +", is :" + financingProgramId);
			logDebug("Data promo id for order: " + order.getId() + " is : " + dataPromoId);
			logDebug("Data amount for order: " + order.getId() + "is: " + cartTotal);
		}

		request.setParameter(DATA_PROMO_ID_PARAM, dataPromoId);
		request.setParameter(DATA_AMOUNT_PARAM, (int)Math.round(cartTotal));
		request.setParameter(PROPERTY_PROGRAM_ID, financingProgramId);
		request.serviceParameter(OPARAM_OUTPUT, request, response);
		return;
		
	}

	public AffirmRuleManager getAffirmRuleManager() {
		return affirmRuleManager;
	}

	public void setAffirmRuleManager(AffirmRuleManager affirmRuleManager) {
		this.affirmRuleManager = affirmRuleManager;
	}

	public String getDefaultFinancialProductKey() {
		return defaultFinancialProductKey;
	}

	public void setDefaultFinancialProductKey(String defaultFinancialProductKey) {
		this.defaultFinancialProductKey = defaultFinancialProductKey;
	}

	public AffirmPaymentConfiguration getAffirmConfiguration() {
		return affirmConfiguration;
	}

	public void setAffirmConfiguration(
			AffirmPaymentConfiguration affirmConfiguration) {
		this.affirmConfiguration = affirmConfiguration;
	}
	
}
