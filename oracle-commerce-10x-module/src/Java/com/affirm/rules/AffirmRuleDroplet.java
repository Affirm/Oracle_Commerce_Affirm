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

public class AffirmRuleDroplet extends DynamoServlet implements AffirmRuleConstants {
	
	private static final ParameterName OPARAM_OUTPUT =  ParameterName.getParameterName("output");
	private static final ParameterName OPARAM_EMPTY =  ParameterName.getParameterName("empty");
	
	private static final String DATA_PROMO_ID_PARAM = "dataPromoId";
	private static final String DATA_AMOUNT_PARAM = "dataAmount";
	private static final String AFFIRM_RULE_NAME = "ruleName";
	
	private static final ParameterName INPUT_PARAM_ORDER =  ParameterName.getParameterName("order");
	private static final ParameterName INPUT_PARAM_PRODUCT =  ParameterName.getParameterName("product");
	private static final ParameterName INPUT_PARAM_SKU =  ParameterName.getParameterName("sku");
	private static final ParameterName INPUT_PARAM_PAGE_TYPE =  ParameterName.getParameterName("pageType");
	
	public AffirmRuleManager affirmRuleManager;
	private AffirmPaymentConfiguration affirmPaymentConfiguration;
	
	public void service(DynamoHttpServletRequest request, DynamoHttpServletResponse response) throws ServletException, IOException {
		
		if (! getAffirmPaymentConfiguration().isEnabled()) {
			// If not enabled, then just service empty oparam
			request.serviceParameter(OPARAM_EMPTY, request, response);
			return;
		}
		
		Order order = (Order)request.getObjectParameter(INPUT_PARAM_ORDER);
		RepositoryItem product = (RepositoryItem)request.getObjectParameter(INPUT_PARAM_PRODUCT);
		RepositoryItem sku = (RepositoryItem)request.getObjectParameter(INPUT_PARAM_SKU);
		String pageType = request.getParameter(INPUT_PARAM_PAGE_TYPE);
		
		if (pageType == null) {
			if (isLoggingError()) {
				logError("Affirm page type can not be null. Try again.");
			}
			request.serviceParameter(OPARAM_EMPTY, request, response);
			return;
		}

		RepositoryItem rule = null;
		
		if (pageType.equals(PDP_PAGE_TYPE)) {
			rule = getAffirmRuleManager().findAffirmRuleForProductDetailPage(order, product, sku);
		} else if (pageType.equals(CATEGORY_PAGE_TYPE) && product != null) {
			// Unclear if we might need different rules on category page
			rule = getAffirmRuleManager().findAffirmRuleForProductDetailPage(order, product, sku);
		} else 	if (pageType.equals(CART_PAGE_TYPE)) {
			rule = getAffirmRuleManager().findAffirmRuleForCheckout(order);
		} else {
			if (isLoggingError()) {
				logError("Didn't recognize page type: " + pageType + " or incorrect params.");
			}
			request.serviceParameter(OPARAM_EMPTY, request, response);
			return;
		}
		
		if (rule != null) {
			
			AffirmRuleData ruleData = getAffirmRuleManager().calculateAffirmRuleData(rule, product, order, pageType);
			
			if (ruleData.isRenderEmptyOparam()) {
				request.serviceParameter(OPARAM_EMPTY, request, response);
				return;
			}
			
			request.setParameter(DATA_AMOUNT_PARAM, (int)Math.round(ruleData.getDataAmount() * 100));			
            request.setParameter(DATA_PROMO_ID_PARAM, ruleData.getDataPromoId());
            request.setParameter(AFFIRM_RULE_NAME, ruleData.getDisplayName());
            request.serviceParameter(OPARAM_OUTPUT, request, response);
            return;
            
		}
				
		// Didn't find a matching rule, and no default specified. Service empty oparam.
		request.serviceParameter(OPARAM_EMPTY, request, response);
		return;
		
	}


	public AffirmRuleManager getAffirmRuleManager() {
		return affirmRuleManager;
	}

	public void setAffirmRuleManager(AffirmRuleManager affirmRuleManager) {
		this.affirmRuleManager = affirmRuleManager;
	}

	public AffirmPaymentConfiguration getAffirmPaymentConfiguration() {
		return affirmPaymentConfiguration;
	}

	public void setAffirmPaymentConfiguration(AffirmPaymentConfiguration affirmPaymentConfiguration) {
		this.affirmPaymentConfiguration = affirmPaymentConfiguration;
	}

}
