package com.affirm.rules;

public class AffirmRuleData {
	
	private String dataPromoId;
	private double dataAmount;
	private boolean renderEmptyOparam;
	private String displayName;
	
	public String getDataPromoId() {
		return dataPromoId;
	}
	public void setDataPromoId(String dataPromoId) {
		this.dataPromoId = dataPromoId;
	}
	
	public double getDataAmount() {
		return dataAmount;
	}
	public void setDataAmount(double dataAmount) {
		this.dataAmount = dataAmount;
	}
	
	public boolean isRenderEmptyOparam() {
		return renderEmptyOparam;
	}
	public void setRenderEmptyOparam(boolean renderEmptyOparam) {
		this.renderEmptyOparam = renderEmptyOparam;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
}
