package com.affirm.commerce.payment;

import atg.nucleus.GenericService;

/**
 * Component to hold Affirm configuration properties
 * 
 * @author dev
 */
public class AffirmPaymentConfiguration extends GenericService {

	private String apiVersion;
	private String apiUrl;
	private String publicAPIKey;
	private String privateAPIKey;
	private String jsUrl;
	private String defaultRuleId;
	private boolean enabled;
	private boolean capturePaymentOnOrderSubmit;
	private double minimumAmount;
	private String affirmCancelUrl;
	private String affirmConfirmationUrl;
	private String moduleVersion;

	public String getApiUrl() {
		return apiUrl;
	}

	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public String getPublicAPIKey() {
		return publicAPIKey;
	}

	public void setPublicAPIKey(String publicAPIKey) {
		this.publicAPIKey = publicAPIKey;
	}

	public String getPrivateAPIKey() {
		return privateAPIKey;
	}

	public void setPrivateAPIKey(String privateAPIKey) {
		this.privateAPIKey = privateAPIKey;
	}

	public String getJsUrl() {
		return jsUrl;
	}

	public void setJsUrl(String jsUrl) {
		this.jsUrl = jsUrl;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isCapturePaymentOnOrderSubmit() {
		return capturePaymentOnOrderSubmit;
	}

	public void setCapturePaymentOnOrderSubmit(boolean capturePaymentOnOrderSubmit) {
		this.capturePaymentOnOrderSubmit = capturePaymentOnOrderSubmit;
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public String getDefaultRuleId() {
		return defaultRuleId;
	}

	public void setDefaultRuleId(String defaultRuleId) {
		this.defaultRuleId = defaultRuleId;
	}

	public double getMinimumAmount() {
		return minimumAmount;
	}

	public void setMinimumAmount(double minimumAmount) {
		this.minimumAmount = minimumAmount;
	}

	public String getAffirmCancelUrl() {
		return affirmCancelUrl;
	}

	public void setAffirmCancelUrl(String affirmCancelUrl) {
		this.affirmCancelUrl = affirmCancelUrl;
	}

	public String getAffirmConfirmationUrl() {
		return affirmConfirmationUrl;
	}

	public void setAffirmConfirmationUrl(String affirmConfirmationUrl) {
		this.affirmConfirmationUrl = affirmConfirmationUrl;
	}

	public String getModuleVersion() {
		return moduleVersion;
	}

	public void setModuleVersion(String moduleVersion) {
		this.moduleVersion = moduleVersion;
	}

}
