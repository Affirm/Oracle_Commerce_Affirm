package com.affirm.commerce.payment.json;

import java.util.List;
import java.util.Map;

public class AffirmOrder {
	
	private String order_id;
	private List<AffirmItem> items;
	private Shipping shipping;
	private Billing billing;
	private int shipping_amount;
	private int tax_amount;
	private int total;
	private Merchant merchant;
	private Config config;
	private Map<String,Discount> discounts;
	private String financing_program;
	private AffirmMetaData metadata;
	
	public String getOrder_id() {
		return order_id;
	}
	public void setOrder_id(String order_id) {
		this.order_id = order_id;
	}
	public List<AffirmItem> getItems() {
		return items;
	}
	public void setItems(List<AffirmItem> items) {
		this.items = items;
	}
	public Shipping getShipping() {
		return shipping;
	}
	public void setShipping(Shipping shipping) {
		this.shipping = shipping;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public Merchant getMerchant() {
		return merchant;
	}
	public void setMerchant(Merchant merchant) {
		this.merchant = merchant;
	}
	public Billing getBilling() {
		return billing;
	}
	public void setBilling(Billing billing) {
		this.billing = billing;
	}
	public int getShipping_amount() {
		return shipping_amount;
	}
	public void setShipping_amount(int shipping_amount) {
		this.shipping_amount = shipping_amount;
	}
	public int getTax_amount() {
		return tax_amount;
	}
	public void setTax_amount(int tax_amount) {
		this.tax_amount = tax_amount;
	}
	public Config getConfig() {
		return config;
	}
	public void setConfig(Config config) {
		this.config = config;
	}
	public Map<String, Discount> getDiscounts() {
		return discounts;
	}
	public void setDiscounts(Map<String, Discount> discounts) {
		this.discounts = discounts;
	}
	public String getFinancing_program() {
		return financing_program;
	}
	public void setFinancing_program(String financing_program) {
		this.financing_program = financing_program;
	}
	public AffirmMetaData getMetadata() {
		return metadata;
	}
	public void setMetadata(AffirmMetaData metadata) {
		this.metadata = metadata;
	}

}
