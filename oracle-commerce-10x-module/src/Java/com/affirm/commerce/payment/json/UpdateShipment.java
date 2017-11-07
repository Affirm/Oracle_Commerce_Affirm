package com.affirm.commerce.payment.json;

import com.google.gson.Gson;

public class UpdateShipment {

	private String order_id;
	private String shipping_carrier;
	private String shipping_confirmation;
	private Shipping shipping;

	
	public String getOrder_id() {
		return order_id;
	}

	public void setOrder_id(String order_id) {
		this.order_id = order_id;
	}

	public String getShipping_carrier() {
		return shipping_carrier;
	}

	public void setShipping_carrier(String shipping_carrier) {
		this.shipping_carrier = shipping_carrier;
	}

	public String getShipping_confirmation() {
		return shipping_confirmation;
	}

	public void setShipping_confirmation(String shipping_confirmation) {
		this.shipping_confirmation = shipping_confirmation;
	}

	public Shipping getShipping() {
		return shipping;
	}

	public void setShipping(Shipping shipping) {
		this.shipping = shipping;
	}
	
	public static void main(String[] args) {

		UpdateShipment shipment = new UpdateShipment();
		shipment.setShipping_carrier("ABC");
		shipment.setOrder_id("o12345");
		shipment.setShipping_confirmation("12312313313");

		Name name = new Name();
		name.setFirst("Girish");
		name.setLast("Jaju");
		
		Shipping shipping = new Shipping();
		shipping.setName(name);

		AffirmAddress address = new AffirmAddress();
		address.setLine1("100 Chatham Park");
		address.setLine2("Apt 512");
		address.setCity("PittsBurgh");
		address.setState("PA");
		address.setZipcode("15220");;

		shipping.setAddress(address);

		shipment.setShipping(shipping);

		Gson gson = new Gson();
		System.out.println(gson.toJson(shipment));

	}
}
