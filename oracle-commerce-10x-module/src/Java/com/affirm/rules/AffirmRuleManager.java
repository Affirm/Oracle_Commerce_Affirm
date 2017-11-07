package com.affirm.rules;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.affirm.commerce.payment.AffirmPaymentConfiguration;

import atg.commerce.order.AuxiliaryData;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.Order;
import atg.nucleus.GenericService;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositorySorter;
import atg.repository.RepositoryView;
import atg.repository.SortDirective;
import atg.repository.SortDirectives;
import atg.repository.rql.RqlStatement;

public class AffirmRuleManager extends GenericService implements AffirmRuleConstants {

	private Repository productCatalog;
	private RqlStatement affirmRulesRql;
	private RqlStatement affirmCategoryRulesRql;
	private Repository priceListRepository;
	private RqlStatement salePriceRql;
	public AffirmPaymentConfiguration affirmConfiguration;
	
	@SuppressWarnings("unchecked")
	public RepositoryItem findAffirmRuleForCheckout(Order order) {
		
		RepositoryItem rule = null;
		
		List<RepositoryItem> allRules = queryForAllRules();
		if (allRules == null || allRules.size() == 0) {
			return rule;
		}
		
		HashSet<RepositoryItem> matchingRules = new HashSet<RepositoryItem>();
		
		List<CommerceItem> commerceItems = (List<CommerceItem>)order.getCommerceItems();
		
		// Loop over each commerce item, and evaluate all rules for each item.
		for (CommerceItem item : commerceItems) {
			
			AuxiliaryData auxData = item.getAuxiliaryData();
			if (auxData == null) {
				continue;
			}
			
			RepositoryItem cartProduct = (RepositoryItem)auxData.getProductRef();
			if (cartProduct == null) {
				continue;
			}
			
			for (RepositoryItem affirmRule : allRules) {
				if (ruleMatches(affirmRule, order, cartProduct, null)) {
					if (isLoggingDebug()) {
						logDebug("Found matching rule: " + affirmRule.getPropertyValue(PROPERTY_DISPLAY_NAME) 
								+ " with priority: " + affirmRule.getPropertyValue(PROPERTY_PRIORITY));
					}
					matchingRules.add(affirmRule);
				}
			}
			
		}
			
		if (matchingRules.size() > 0) {
			RepositoryItem bestRule = findHighestPriortyMatch(matchingRules);
			if (bestRule != null) {
				if (isLoggingDebug()) {
					logDebug("Best rule for cart: " + bestRule.getPropertyValue(PROPERTY_DISPLAY_NAME));
				}
				return bestRule;
			}
		}

		return rule;
		
	}

	/**
	 * Given a list of affirm rules, return the one with highest priority
	 * @param matchingRules
	 * @return
	 */
	private RepositoryItem findHighestPriortyMatch(HashSet<RepositoryItem> matchingRules) {
		
		RepositoryItem[] rules = new RepositoryItem[matchingRules.size()];
		rules = matchingRules.toArray(rules);
		SortDirective prioritySort = new SortDirective(PROPERTY_PRIORITY, SortDirective.DIR_ASCENDING);
		SortDirectives sortDirectives = new SortDirectives();
		sortDirectives.addDirective(prioritySort);
		RepositorySorter.sort(rules, sortDirectives);
		if (rules.length > 0) {
			return rules[0];
		}
		return null;
		
	}

	/**
	 * Given an order and product or SKU, return the Affirm rule that matches
	 *
	 */
	public RepositoryItem findAffirmRuleForProductDetailPage(Order order, RepositoryItem product, RepositoryItem sku) {
		
		if (product == null) {
			if (isLoggingError()) {
				logError("Must pass a product to evaluate Affirm rule on PDP");
			}
			return null;
		}

		RepositoryItem rule = null;
		
		// 1. Query for all Affirm rules
		// 2. Iterate over each one and evaluate if order/product/sku fits
		//    For exmaple:
		//      if rule type = cart_total, then return rule if order amount is greater than cart total
		//      if rule type = product, then return rule if product matches, and check exclusivity
		//      if rule type = category, then return rule if product.parentCategories includes category
		List<RepositoryItem> allRules = queryForAllRules();
		if (allRules == null || allRules.size() == 0) {
			return rule;
		}
		
		for (RepositoryItem affirmRule : allRules) {
			if (ruleMatches(affirmRule, order, product, sku)) {
				rule = affirmRule;
				break;
			}
		}
		
		if (rule == null) {
			rule = getDefaultAffirmRule();
		}

		return rule;

	}
	
	public RepositoryItem findAffirmRuleForCategoryPage(Order order, RepositoryItem category, RepositoryItem sku) {
		
		if (category == null) {
			if (isLoggingWarning()) {
				logWarning("Tried to find a category rule but no category param passed in.");
			}
			return null;
		}
		
		// First check for category-specific rule, if none found, search for cart rule
		RepositoryItem rule = null;
		
		List<RepositoryItem> allRules = queryForCategoryRules();
		if (allRules != null && allRules.size() > 0) {
			for (RepositoryItem categoryRule : allRules) {
				RepositoryItem ruleCategory = (RepositoryItem)categoryRule.getPropertyValue("category");
				if (ruleCategory.equals(category)) {
					return rule;
				}
			}
		}
		
		// No category rule found. Search for cart rule.
		rule = findAffirmRuleForCheckout(order);
		
		if (rule == null) {
			// Still no rule? Return default.
			rule = getDefaultAffirmRule();
		}
		
		return rule;
		
	}
	
	public RepositoryItem getDefaultAffirmRule() {
		
		String defaultRuleId = getAffirmConfiguration().getDefaultRuleId();
		RepositoryItem defaultRule = null;
		
		if (defaultRuleId == null) {
			if (isLoggingError()) {
				logError("No default rule has been specified in the AffirmRuleManager.properties file. That should be done.");
			}
			return null;
		}
		
		try {
			
			defaultRule = getProductCatalog().getItem(defaultRuleId, ITEM_DESCRIPTOR_AFFIRM_RULE);
			if (defaultRule == null) {
				if (isLoggingError()) {
					logError("There is no Affirm rule with this id: " + defaultRuleId);
				}
			}
			
		} catch (RepositoryException e) {
			if (isLoggingError()) {
				logError(e);
			}
		}
		
		if (defaultRule != null) {
			if (isLoggingDebug()) {
				logDebug("Returning default rule: " + defaultRuleId);
			}
		} else {
			if (isLoggingDebug()) {
				logDebug("Returning null. No rule with the default id: " + defaultRuleId);
			}
		}
		
		return defaultRule;
		
	}

	private boolean ruleMatches(RepositoryItem affirmRule, Order order,	RepositoryItem product, RepositoryItem sku) {

		String ruleType = (String)affirmRule.getPropertyValue(PROPERTY_TYPE);
		if (ruleType.equals(PROPERTY_VALUE_TYPE_CART_TOTAL)) {
			return evaluateCartRule(affirmRule, order);
		}
		
		if (ruleType.equals(PROPERTY_VALUE_TYPE_PRODUCT)) {
			// If exclusive, we need to check cart to see if other items are in there
			return evaluateProductRule(affirmRule, order, product);
		}
		
		if (ruleType.equals(PROPERTY_VALUE_TYPE_PRODUCT_AMOUNT)) {
			return evaluateProductAmountRule(affirmRule, product);
		}
		
		if (ruleType.equals(PROPERTY_VALUE_TYPE_SKU)) {
			RepositoryItem ruleSku = (RepositoryItem)affirmRule.getPropertyValue(PROPERTY_SKU);
			if (sku != null && ruleSku != null && ruleSku.equals(sku)) {
				return true;
			}
		}
		
		if (ruleType.equals(PROPERTY_VALUE_TYPE_CATEGORY)) {
			return evaluateCategoryRule(affirmRule, product, order);
		}
		
		if (ruleType.equals(PROPERTY_VALUE_TYPE_TIME)) {
			return evaluateTimeRule(affirmRule);
		}
		
		return false;
		
	}

	@SuppressWarnings("unchecked")
	private boolean evaluateProductRule(RepositoryItem affirmRule, Order order,	RepositoryItem product) {
		
		Boolean exclusiveFlag = (Boolean) affirmRule.getPropertyValue(PROPERTY_EXCLUSIVE_FLAG);
		boolean exclusive = false;
		
		if (exclusiveFlag != null && exclusiveFlag.booleanValue()) {
			exclusive = true;
		}
		
		// Fast fail. If no match, just return false
		if (! doesProductMatchRuleProduct(affirmRule, product)) {
			return false;
		}
		
		// If not exclusive, we have a match
		if (! exclusive) {
			return true;
		}
		
		// Exclusive, and product match. Make sure there are no items in cart that don't match the rule products.
		List<CommerceItem> commerceItems = (List<CommerceItem>)order.getCommerceItems();
		for (CommerceItem item : commerceItems) {
			
			AuxiliaryData auxData = item.getAuxiliaryData();
			
			if (auxData == null) {
				continue;
			}
			
			RepositoryItem cartProduct = (RepositoryItem)auxData.getProductRef();
			if (cartProduct == null) {
				continue;
			}
			
			List<RepositoryItem> ruleProducts = (List<RepositoryItem>)affirmRule.getPropertyValue(PROPERTY_PRODUCTS);
			
			if (! ruleProducts.contains(cartProduct)) {
				return false;
			}
			
		}
		
		if (isLoggingDebug()) {
			logDebug("Found match to exclusive product rule: " + (String)affirmRule.getPropertyValue(PROPERTY_DISPLAY_NAME));
		}
		
		return true;
		
	}

	@SuppressWarnings("unchecked")
	private boolean doesProductMatchRuleProduct(RepositoryItem affirmRule, RepositoryItem product) {
		
		if (product == null) {
			return false;
		}
		
		List<RepositoryItem> products = (List<RepositoryItem>)affirmRule.getPropertyValue(PROPERTY_PRODUCTS);
		for (RepositoryItem ruleProduct : products) {
			if (product.equals(ruleProduct)) {
				return true;
			}
		}
		
		return false;
		
	}

	private boolean evaluateTimeRule(RepositoryItem affirmRule) {

		Timestamp startDate = (Timestamp)affirmRule.getPropertyValue(PROPERTY_START_DATE);
		Timestamp endDate = (Timestamp)affirmRule.getPropertyValue(PROPERTY_END_DATE);
		
		// If no start date, or future start date, return false.
		if (startDate == null || startDate.getTime() > System.currentTimeMillis()) {
			return false;
		}
		
		// Allow an empty end date. If null, or if end date is in the future, it is valid
		if (endDate == null || endDate.getTime() > System.currentTimeMillis()) {
			if (isLoggingDebug()) {
				logDebug("Found match for time-based rule: " + (String)affirmRule.getPropertyValue(PROPERTY_DISPLAY_NAME));
			}
			return true;
		}
		
		return false;
		
	}

	@SuppressWarnings("unchecked")
	private boolean evaluateCategoryRule(RepositoryItem affirmRule,	RepositoryItem product, Order order) {

		RepositoryItem category = (RepositoryItem)affirmRule.getPropertyValue(PROPERTY_CATEGORY);
		if (category == null) {
			if (isLoggingError()) {
				logError("Category rule set up, but no category associated");
			}
			return false;
		}
		
		List<RepositoryItem> ancestorCategories = (List<RepositoryItem>)product.getPropertyValue("ancestorCategories");
		if (ancestorCategories == null || ancestorCategories.size() == 0) {
			return false;
		}
		
		if (! ancestorCategories.contains(category)) {
			if (isLoggingDebug()) {
				logDebug("Found category match for product: " + product.getRepositoryId());
			}
			return false;
		}
		
		// If not exclusive, we're done. If exclusive, we need to check ancestorCategories of items in cart.
		Boolean exclusiveFlag = (Boolean) affirmRule.getPropertyValue(PROPERTY_EXCLUSIVE_FLAG);
		boolean exclusive = false;
		
		if (exclusiveFlag != null && exclusiveFlag.booleanValue()) {
			exclusive = true;
		}
		
		if (! exclusive) {
			if (isLoggingDebug()) {
				logDebug("Found matching category rule: " + (String)affirmRule.getPropertyValue(PROPERTY_DISPLAY_NAME));
			}
			return true;
		}
		
		if (allCartItemsInCategory(order, category)) {
			return true;
		}
		
		return false;
		
	}

	@SuppressWarnings("unchecked")
	private boolean allCartItemsInCategory(Order order, RepositoryItem category) {
		
		List<CommerceItem> commerceItems = (List<CommerceItem>)order.getCommerceItems();
		
		for (CommerceItem item : commerceItems) {
			
			AuxiliaryData auxData = item.getAuxiliaryData();
			if (auxData == null) {
				continue;
			}
			
			RepositoryItem cartProduct = (RepositoryItem)auxData.getProductRef();
			if (cartProduct == null) {
				continue;
			}
			
			List<RepositoryItem> ancestorCategories = (List<RepositoryItem>) cartProduct.getPropertyValue("ancestorCategories");
			if (ancestorCategories == null || ancestorCategories.size() == 0) {
				return false;
			}
			
			if (! ancestorCategories.contains(category)) {
				if (isLoggingDebug()) {
					logDebug("Found category match for product: " + cartProduct.getRepositoryId());
				}
				return false;
			}
		}
		
		// Didn't find any items in the order where ancestorCategories did not include this category
		return true;
		
	}

	private boolean evaluateProductAmountRule(RepositoryItem affirmRule, RepositoryItem product) {
		
		if (isLoggingInfo()) {
			logInfo("Using price list lookup to determine product price");
		}
		
		Double ruleProductAmount = (Double) affirmRule.getPropertyValue(PROPERTY_AMOUNT);
		if (ruleProductAmount == null) {
			if (isLoggingError()) {
				logError("Found product amount rule, but no amount. Can not evaluate. Rule: " + (String)affirmRule.getPropertyValue(PROPERTY_DISPLAY_NAME));
			}
			return false;
		}
		
		double productPrice = findProductPrice(product);
		
		if (productPrice > ruleProductAmount) {
			if (isLoggingDebug()) {
				logDebug("Found matching product amount rule: " + (String)affirmRule.getPropertyValue(PROPERTY_DISPLAY_NAME));
			}
			return true;
		}
        
		return false;
		
	}

	@SuppressWarnings({ "unchecked" })
	public double findProductPrice(RepositoryItem product) {
		
		List<RepositoryItem> childSkus = (List<RepositoryItem>)product.getPropertyValue("childSKUs");
		if (childSkus == null || childSkus.size() == 0) {
			return 0.0;
		}
		
		if (childSkus.size() > 1) {
			if (isLoggingWarning()) {
				logWarning("Product has multiple child SKUs. Using the first one that has a price: " + product.getRepositoryId());
			}
		}
		
		for (RepositoryItem sku : childSkus) {
			
			Object[] params = new Object[] { "salePrices", sku.getRepositoryId() };
			try {
				
				RepositoryView view = getPriceListRepository().getView("price");
				RepositoryItem[] price = getSalePriceRql().executeQuery(view, params);
				
				if (price == null || price.length != 1) {
					continue;
				}
				
				Double repositoryPrice = (Double)price[0].getPropertyValue("listPrice");
				if (repositoryPrice != null) {
					double skuPrice = repositoryPrice.doubleValue();
					if (isLoggingDebug()) {
						logDebug("Returning price: " + skuPrice + " from SKU: " + sku.getRepositoryId());
					}
					return skuPrice;
				}
				
			} catch (RepositoryException e) {
				if (isLoggingError()) {
					logError(e);
				}
			}
			
		}
		
		// No sale price found, look for list price
		for (RepositoryItem sku : childSkus) {
			
			Object[] params = new Object[] { "listPrices", sku.getRepositoryId() };
			try {
				
				RepositoryView view = getPriceListRepository().getView("price");
				RepositoryItem[] price = getSalePriceRql().executeQuery(view, params);
				
				if (price == null || price.length != 1) {
					continue;
				}
				
				Double repositoryPrice = (Double)price[0].getPropertyValue("listPrice");
				if (repositoryPrice != null) {
					return repositoryPrice.doubleValue();
				}
				
			} catch (RepositoryException e) {
				if (isLoggingError()) {
					logError(e);
				}
			}
			
		}

		if (isLoggingWarning()) {
			logWarning("No price found for product: " + product.getRepositoryId());
		}
		return 0.0;
		
	}

	private boolean evaluateCartRule(RepositoryItem affirmRule, Order order) {
		
		if (isLoggingDebug()) {
			logDebug("Evaluating cart total rule");
		}
		
		double totalCartValue = findCartTotal(order);
		Double ruleAmount = (Double)affirmRule.getPropertyValue("amount");
		
		if (ruleAmount != null) {
			if (totalCartValue > ruleAmount.doubleValue()) {
				if (isLoggingDebug()) {
					logDebug("Found matching cart total rule: " + (String) affirmRule.getPropertyValue(PROPERTY_DISPLAY_NAME));
				}
				return true;
			}
		}
		
		return false;
		
	}
	
	public double findCartTotal(Order order) {
		return order.getPriceInfo().getTotal();
	}

	public List<RepositoryItem> queryForAllRules() {
		
		Object[] params = new Object[] { };
		List<RepositoryItem> affirmRules = new ArrayList<RepositoryItem>();
		
		try {
			
			RepositoryView view = getProductCatalog().getView(ITEM_DESCRIPTOR_AFFIRM_RULE);
	        RepositoryItem[] allRules = getAffirmRulesRql().executeQuery(view, params);
	        if (allRules != null && allRules.length > 0) {
	        	affirmRules = Arrays.asList(allRules);
	        }
	        
		} catch (RepositoryException e) {
			if (isLoggingError()) {
				logError(e);
			}
		}

		return affirmRules;
		
	}
	
	public List<RepositoryItem> queryForCategoryRules() {
		
		Object[] params = new Object[] { };
		List<RepositoryItem> affirmRules = new ArrayList<RepositoryItem>();
		
		try {
			
			RepositoryView view = getProductCatalog().getView(ITEM_DESCRIPTOR_AFFIRM_RULE);
	        RepositoryItem[] allRules = getAffirmCategoryRulesRql().executeQuery(view, params);
	        if (allRules != null && allRules.length > 0) {
	        	affirmRules = Arrays.asList(allRules);
	        }
	        
		} catch (RepositoryException e) {
			if (isLoggingError()) {
				logError(e);
			}
		}

		return affirmRules;
		
	}
	

	public AffirmRuleData calculateAffirmRuleData(RepositoryItem rule, RepositoryItem product, Order order, String pageType) {

		String ruleType = (String)rule.getPropertyValue(PROPERTY_TYPE);
		AffirmRuleData ruleData = null;
		
		// We always use product price on PDP and category pages. Return nothing if we don't have a product
		if (pageType.equals(PDP_PAGE_TYPE) || pageType.equals(CATEGORY_PAGE_TYPE)) {
			if (product == null) {
				ruleData = new AffirmRuleData();
				ruleData.setRenderEmptyOparam(true);
				return ruleData;
			}
		}
		
		if (ruleType.equals(PROPERTY_VALUE_TYPE_PRODUCT)) {
			ruleData = calculateProductRuleData(rule, product, order, pageType);
		} else if (ruleType.equals(PROPERTY_VALUE_TYPE_CATEGORY)) {
			ruleData = calculateCategoryRuleData(rule, product, order, pageType);
		} else if (ruleType.equals(PROPERTY_VALUE_TYPE_TIME)) {
			ruleData = calculateTimeRuleData(rule, product, order, pageType);
		} else {
			ruleData = calculateCartRuleData(rule, product, order, pageType);
		}

		if (ruleData.getDataAmount() < getAffirmConfiguration().getMinimumAmount()) {
			if (isLoggingDebug()) {
				logDebug("Not showing Affirm rule because minimum amount was not met.");
			}
			ruleData.setRenderEmptyOparam(true);
			return ruleData;
		}

		ruleData.setDataPromoId((String)rule.getPropertyValue(PROPERTY_DATA_PROMO_ID));
		ruleData.setDisplayName((String)rule.getPropertyValue(PROPERTY_DISPLAY_NAME));

		return ruleData;
		
	}

	private AffirmRuleData calculateTimeRuleData(RepositoryItem rule, RepositoryItem product, Order order, String pageType) {
		
		// If we get a product, use it, if not, use cart
		AffirmRuleData ruleData = new AffirmRuleData();
		double amount = 0.0;
		
		if (pageType.equals(CART_PAGE_TYPE)) {
			amount = findCartTotal(order);
		} else {
			if (product != null) {
				amount = findProductPrice(product);
			} else {		
				ruleData.setRenderEmptyOparam(true);
			}
		}
		ruleData.setDataAmount(amount);
		return ruleData;
		
	}

	private AffirmRuleData calculateCartRuleData(RepositoryItem rule, RepositoryItem product, Order order, String pageType) {
		
		// Assume we need the full cart amount for amount param
		AffirmRuleData ruleData = new AffirmRuleData();
		double amount = 0.0;
		
		if (pageType.equals(CART_PAGE_TYPE)) {
			amount = findCartTotal(order);
		} else {
			if (product == null) {
				ruleData.setRenderEmptyOparam(true);
			} else {
				amount = findProductPrice(product);
			}
		}
		
		if (amount < getAffirmConfiguration().getMinimumAmount()) {
			ruleData.setRenderEmptyOparam(true);
		}
		
		ruleData.setDataAmount(amount);
		return ruleData;
		
	}

	/*
	 * If we got a product passed in, this is a browse page, 
	 * and we just use that product for the price and data amount.
	 * 
	 * If no product, then we sum up the total value of items in the cart
	 * for the data amount.
	 */
	private AffirmRuleData calculateCategoryRuleData(RepositoryItem rule, RepositoryItem product, Order order, String pageType) {

		AffirmRuleData ruleData = new AffirmRuleData();

		double amount = 0.0;
		
		if (pageType.equals(CART_PAGE_TYPE)) {
			amount = findCartTotal(order);
		} else {
			if (product != null) {
				amount = findProductPrice(product);
			} else {
				ruleData.setRenderEmptyOparam(true);
			}
		}
		
		ruleData.setDataAmount(amount);
		return ruleData;
		
	}

	private AffirmRuleData calculateProductRuleData(RepositoryItem rule, RepositoryItem product, Order order, String pageType) {

		AffirmRuleData ruleData = new AffirmRuleData();
		
		if (product == null && !(pageType.equals(CART_PAGE_TYPE))) {
			// This is a product or category rule on a product/category page, but no product. Skip it.
			if (isLoggingError()) {
				logError("No product passed when calculating rule price.");
			}
			ruleData.setRenderEmptyOparam(true);
			return ruleData;
		}
		
		if (pageType.equals(CART_PAGE_TYPE)) {
			
			ruleData.setDataAmount(findCartTotal(order));
			
		} else {
			
			double productPrice = findProductPrice(product);
			if (productPrice < getAffirmConfiguration().getMinimumAmount()) {
				// Skip javascript call if less than the minimum amount
				if (isLoggingDebug()) {
					logDebug("Amount less than minimum. Skip call to Affirm.");
				}
				ruleData.setRenderEmptyOparam(true);
				return ruleData;
			}
			ruleData.setDataAmount(productPrice);
			
		}
		return ruleData;
		
	}

	public Repository getProductCatalog() {
		return productCatalog;
	}

	public void setProductCatalog(Repository productCatalog) {
		this.productCatalog = productCatalog;
	}

	public RqlStatement getAffirmRulesRql() {
		return affirmRulesRql;
	}

	public void setAffirmRulesRql(RqlStatement affirmRulesRql) {
		this.affirmRulesRql = affirmRulesRql;
	}

	public Repository getPriceListRepository() {
		return priceListRepository;
	}

	public void setPriceListRepository(Repository priceListRepository) {
		this.priceListRepository = priceListRepository;
	}

	public RqlStatement getSalePriceRql() {
		return salePriceRql;
	}

	public void setSalePriceRql(RqlStatement salePriceRql) {
		this.salePriceRql = salePriceRql;
	}

	public RqlStatement getAffirmCategoryRulesRql() {
		return affirmCategoryRulesRql;
	}

	public void setAffirmCategoryRulesRql(RqlStatement affirmCategoryRulesRql) {
		this.affirmCategoryRulesRql = affirmCategoryRulesRql;
	}

	public AffirmPaymentConfiguration getAffirmConfiguration() {
		return affirmConfiguration;
	}

	public void setAffirmConfiguration(
			AffirmPaymentConfiguration affirmConfiguration) {
		this.affirmConfiguration = affirmConfiguration;
	}


}
