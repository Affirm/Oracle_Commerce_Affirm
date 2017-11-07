package com.affirm.commerce.payment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CreditCard;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.Order;
import atg.commerce.order.PaymentGroup;
import atg.commerce.order.PaymentGroupManager;
import atg.commerce.order.PaymentGroupRelationship;
import atg.commerce.order.RelationshipTypes;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.SimpleOrderManager;
import atg.commerce.pricing.PricingAdjustment;
import atg.core.util.ContactInfo;
import atg.core.util.StringUtils;
import atg.nucleus.GenericService;
import atg.repository.RepositoryItem;

import com.affirm.commerce.payment.json.AffirmAddress;
import com.affirm.commerce.payment.json.AffirmItem;
import com.affirm.commerce.payment.json.AffirmMetaData;
import com.affirm.commerce.payment.json.AffirmOrder;
import com.affirm.commerce.payment.json.Billing;
import com.affirm.commerce.payment.json.Discount;
import com.affirm.commerce.payment.json.Merchant;
import com.affirm.commerce.payment.json.Name;
import com.affirm.commerce.payment.json.Shipping;
import com.affirm.commerce.payment.json.UpdateShipment;
import com.affirm.commerce.payment.response.AffirmResponse;
import com.affirm.payment.AffirmPaymentConstants;
import com.affirm.rules.AffirmRuleConstants;
import com.affirm.rules.AffirmRuleManager;
import com.google.gson.Gson;

/**
 * Utility class for making API calls for Affirm for various payment operations.
 * 
 */
public class AffirmUtil extends GenericService implements AffirmRuleConstants {

	private AffirmPaymentConfiguration configuration;
	private AffirmRuleManager ruleManager;
	private SimpleOrderManager orderManager;
	private NumberFormat numberFormat = DecimalFormat.getInstance();
	private PaymentGroupManager paymentGroupManager;
	
	private NumberFormat getNumberFormat() {
		numberFormat.setMaximumFractionDigits(0);
		numberFormat.setGroupingUsed(false);
		return numberFormat;
	}
	
	/**
	 * Makes call to authorize the charge using checkout token returned from Affirm earlier.
	 * 
	 * @param orderId
	 * @param checkoutToken
	 * @return
	 */
	public AffirmResponse authorizeCharge(String orderId, String checkoutToken) {
		
		AffirmResponse authResponse = new AffirmResponse();
		
		try {
			
			URL url = new URL(this.getConfiguration().getApiUrl() + "/charges");
			
			String content = "{\"checkout_token\": \"" + checkoutToken + "\"}";
			String result = makeApiCall(url, content);
			
			if (StringUtils.isEmpty(result)) {
				authResponse.setErrorMessage("Exception during Authorization for order");
				authResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_AUTH);
				return authResponse;
			}
			
			JSONParser parser = new JSONParser();
			JSONObject obj  = (JSONObject)parser.parse(result.toString());

			String authId = (String) obj.get("id");
			String transactionId = null;
			
			if(obj.get("events") != null) {
				JSONArray eventsArray = (JSONArray)obj.get("events");
				JSONObject eventObj = (JSONObject)eventsArray.get(0);
				if (eventObj != null) {
					transactionId = (String) eventObj.get("id");
				}
			}
			
			if (isLoggingDebug()) {
				logDebug("Auth Id For Order:" + orderId + " is:" + authId + ",txn id =" + transactionId);
			}
			
			authResponse.setSuccess(true);
			authResponse.setId(authId);
			authResponse.setTransactionId(transactionId);
			
		} catch (MalformedURLException e) {
			if (isLoggingError()) {
				logError(e);
			}
		} catch (ParseException e) {
			if (isLoggingError()) {
				logError(e);
			}
		}
		
		return authResponse;
		
	}
	

	/**
	 * Makes call to capture the charge for the orderId and chargeId (returned during authorization call)
	 * @param orderId
	 * @param chargeId
	 * @return
	 */
	public AffirmResponse captureCharge(String orderId, String chargeId) {
		
		AffirmResponse captureResponse = new AffirmResponse();
		
		try {
			
			URL url = new URL(this.getConfiguration().getApiUrl() + "/charges/" + chargeId + "/capture");
			String content = "{\"order_id\": \"" + orderId + "\"}";
			
			String result = makeApiCall(url, content);
			
			JSONParser parser = new JSONParser();
			
			JSONObject obj = (JSONObject)parser.parse(result);
			String transactionId = (String) obj.get("transaction_id");
			String id = (String) obj.get("id");
			if(isLoggingDebug()) {
				logDebug("Transaction Id for order:" + orderId + " is:" + transactionId);
			}
			
			captureResponse.setSuccess(true);
			captureResponse.setTransactionId(transactionId);
			captureResponse.setId(id);

		} catch (ParseException e) {
			
			if (isLoggingError()) {
				logError("Exception during Capture for order:" + orderId + ",chargeId: " + chargeId, e);
			}
			captureResponse.setErrorMessage("Exception during capturing for order");
			captureResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_DEBIT);
			
		} catch (MalformedURLException e) {
			
			if (isLoggingError()) {
				logError("Exception during Capture for order:" + orderId + ",chargeId: " + chargeId, e);
			}
			captureResponse.setErrorMessage("Exception during capturing for order");
			captureResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_DEBIT);
			
		}

		return captureResponse;
		
	}



	/**
	 * Creates and return HttpURLConnection object for the url passed.
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws ProtocolException
	 */
	private HttpURLConnection getHttpUrlConnection(URL url) throws IOException,	ProtocolException {
		
		HttpURLConnection httpsConnection = (HttpURLConnection) url.openConnection();

		// Set request properties
		httpsConnection.setRequestMethod("POST");
		httpsConnection.setDoOutput(true);
		httpsConnection.setRequestProperty("Content-Type", "application/json");
		
		String publicKey = this.getConfiguration().getPublicAPIKey();
		String privateKey = this.getConfiguration().getPrivateAPIKey();
		byte[] authKeys =  (publicKey + ":" + privateKey).getBytes();

		httpsConnection.setRequestProperty("Authorization",	"Basic " + DatatypeConverter.printBase64Binary(authKeys));
		
		return httpsConnection;
		
	}
	
	
	/**
	 * Makes void API call for the charge id (returned from the Auth call)
	 * @param orderId
	 * @param chargeId
	 * @return
	 */
	public AffirmResponse voidCharge(String chargeId) {
		
		AffirmResponse voidResponse = new AffirmResponse();
		
		try {
			
			URL url = new URL(this.getConfiguration().getApiUrl() + "/charges/" + chargeId + "/void");
			String content = "{}";
			
			String result = makeApiCall(url, content);
			
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(result);
			String transactionId = (String) obj.get("transaction_id");
			String orderId = (String) obj.get("order_id");
			String id = (String) obj.get("id");
			
			if(isLoggingDebug()) {
				logDebug("Transaction Id for order:" + orderId + " is:" + transactionId);
			}
			
			voidResponse.setSuccess(true);
			voidResponse.setTransactionId(transactionId);
			voidResponse.setOrderId(orderId);
			voidResponse.setId(id);

		} catch (ParseException e) {
			
			if (isLoggingError()) {
				logError("Exception during VOID for chargeId: " + chargeId, e);
			}
			voidResponse.setErrorMessage("Exception during void for order");
			voidResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_VOID);
			
		} catch (MalformedURLException e) {
			
			if (isLoggingError()) {
				logError("Exception during VOID for chargeId: " + chargeId, e);
			}
			voidResponse.setErrorMessage("Exception during void for order");
			voidResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_VOID);
			
		}

		return voidResponse;
		
	}
	
	
	/**
	 * Makes refund API call for the charge id (returned from the Auth call)
	 * @param orderId
	 * @param chargeId
	 * @return
	 */
	public AffirmResponse refundCharge(String chargeId) {
		
		AffirmResponse refundResponse = new AffirmResponse();
		
		try {
			
			URL url = new URL(this.getConfiguration().getApiUrl() + "/charges/" + chargeId + "/refund");
			String content = "{}";
			
			String result = makeApiCall(url, content);
						
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(result);
			String transactionId = (String) obj.get("transaction_id");
			String orderId = (String) obj.get("order_id");
			String id = (String) obj.get("id");
			
			if(isLoggingDebug()) {
				logDebug("Transaction Id for order:" + orderId + " is:" + transactionId);
			}
			
			refundResponse.setSuccess(true);
			refundResponse.setTransactionId(transactionId);
			refundResponse.setOrderId(orderId);
			refundResponse.setId(id);
			
		} catch (ParseException e) {
			
			if (isLoggingError()) {
				logError("Exception during VOID for chargeId: " + chargeId, e);
			}
			refundResponse.setErrorMessage("Exception during refund for order");
			refundResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_CREDIT);
			
		} catch (MalformedURLException e) {

			if (isLoggingError()) {
				logError("Exception during VOID for chargeId: " + chargeId, e);
			}
			refundResponse.setErrorMessage("Exception during refund for order");
			refundResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_CREDIT);
			
		}

		return refundResponse;
		
	}
	
	/**
	 * Makes refund API call for the charge id (returned from the Auth call)
	 * @param orderId
	 * @param chargeId
	 * @return
	 */
	public AffirmResponse refundCharge(String chargeId, double refundAmount) {
		
		if(isLoggingDebug()) {
			logDebug("RefundCharge called for chargeId:" + chargeId + ", amount = " + refundAmount);
		}
		
		AffirmResponse refundResponse = new AffirmResponse();
		
		try {
			
			URL url = new URL(this.getConfiguration().getApiUrl() + "/charges/" + chargeId + "/refund");
			String content = "{\"amount\": \"" + getNumberFormat().format(refundAmount * 100)+ "\"}";
			
			if(isLoggingDebug()) {
				logDebug("RefundCharge called for chargeId:" + chargeId + ", amount = " + refundAmount +" --> Content=" + content);
			}
			
			String result = makeApiCall(url, content);
			
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(result.toString());
			String transactionId = (String) obj.get("transaction_id");
			String orderId = (String) obj.get("order_id");
			String id = (String) obj.get("id");
			
			if(isLoggingDebug()) {
				logDebug("Transaction Id for order:" + orderId + " is:" + transactionId);
			}
			
			refundResponse.setSuccess(true);
			refundResponse.setTransactionId(transactionId);
			refundResponse.setOrderId(orderId);
			refundResponse.setId(id);

		} catch (ParseException e) {
			if (isLoggingError()) {
				logError("Exception during refund for chargeId: " + chargeId, e);
			}
			refundResponse.setErrorMessage("Exception during refund for order");
			refundResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_CREDIT);
		} catch (MalformedURLException e) {
			if (isLoggingError()) {
				logError("Exception during refund for chargeId: " + chargeId, e);
			}
			refundResponse.setErrorMessage("Exception during refund for order");
			refundResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_CREDIT);
		}

		return refundResponse;
	}
	
	
	/**
	 * Makes update API call for updating the shipping carrier and tracking
	 * @param orderId
	 * @param chargeId
	 * @return
	 */
	public AffirmResponse updateCharge(String chargeId, String orderId, ContactInfo shipAddress, String shipmentCarrier, String shipmentTrackingNumber) {
		
		if(isLoggingDebug()) {
			logDebug("updateCharge called for chargeId:" + chargeId + ", shipmentCarrier = " + shipmentCarrier + ", tracking number = " + shipmentTrackingNumber);
		}
		
		AffirmResponse updateResponse = new AffirmResponse();
				
		try {
			
			URL url = new URL(this.getConfiguration().getApiUrl() + "/charges/" + chargeId + "/update");
			String content = buildShipmentPostData(orderId, shipAddress, shipmentCarrier, shipmentTrackingNumber);
			
			if(isLoggingDebug()) {
				logDebug("UPDATE CONTENT::" + content);
			}
			
			String result = makeApiCall(url, content);

			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(result);
			String id = (String) obj.get("id");
			
			if(isLoggingDebug()) {
				logDebug("Transaction Id for updating:" + orderId + " is:" + id);
			}
			
			updateResponse.setSuccess(true);
			updateResponse.setOrderId(orderId);
			updateResponse.setId(id);

		} catch (ParseException e) {
			
			if (isLoggingError()) {
				logError("Exception during update for chargeId: " + chargeId, e);
			}
			updateResponse.setErrorMessage("Exception during update for order");
			updateResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_UPDATE);
			
		} catch (MalformedURLException e) {
			
			if (isLoggingError()) {
				logError("Exception during update for chargeId: " + chargeId, e);
			}
			updateResponse.setErrorMessage("Exception during update for order");
			updateResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_UPDATE);
			
		}
		
		return updateResponse;
		
	}
	
	public AffirmResponse startCheckout(Order order) {
		
		if(isLoggingDebug()){
			logDebug("Starting direct checkout");
		}
		
		AffirmResponse checkoutResponse = new AffirmResponse();
		
		try {
			
			URL url = new URL(this.getConfiguration().getApiUrl() + "/checkout/direct");
			String content = buildCheckoutPostData(order);
			String result = makeApiCall(url, content);

			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(result);
			String id = (String) obj.get("id");
			String redirect = (String) obj.get("redirect_url");
			
			checkoutResponse.setId(id);
			checkoutResponse.setOrderId(order.getId());
			checkoutResponse.setRedirectUrl(redirect);
			checkoutResponse.setSuccess(true);
			
		} catch (MalformedURLException e) {
			if (isLoggingError()) {
				logError(e);
			}
			checkoutResponse.setErrorMessage("Bad URL sent to checkout");
			checkoutResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_CHECKOUT);
		} catch (ParseException e) {
			if (isLoggingError()) {
				logError(e);
			}
			checkoutResponse.setErrorMessage("Bad URL sent to checkout");
			checkoutResponse.setErrorCode(AffirmPaymentConstants.ERROR_CODE_CHECKOUT);
		}
		
		return checkoutResponse;
		
	}
	

	private String buildCheckoutPostData(Order order) {

		AffirmOrder affirmOrder = new AffirmOrder();
		
		Merchant merchant = buildMerchantPostData();
		affirmOrder.setMerchant(merchant);
		
		Shipping shipment = buildShipmentPostData(order);
		affirmOrder.setShipping(shipment);
		
		Billing billing = buildBillingPostData(order);
		affirmOrder.setBilling(billing);
		
		List<AffirmItem> affirmItems = buildItemPostData(order);
		affirmOrder.setItems(affirmItems);
		
		addFinancingProgram(order, affirmOrder);
		
		addMetaData(affirmOrder);
		
		Map<String,Discount> discounts = buildDiscountPostData(order);
		if (discounts.size() > 0) {
			affirmOrder.setDiscounts(discounts);
		}
		
		double shippingPrice = order.getPriceInfo().getShipping();
		double tax = order.getPriceInfo().getTax();
		double total = order.getPriceInfo().getTotal();
		
		affirmOrder.setShipping_amount((int)Math.round(shippingPrice * 100));
		affirmOrder.setTax_amount((int)Math.round(tax * 100));
		affirmOrder.setTotal((int)Math.round(total * 100));
		
		Gson gson = new Gson();
		return gson.toJson(affirmOrder);
		
	}

	private void addMetaData(AffirmOrder affirmOrder) {
		AffirmMetaData metaData = new AffirmMetaData();
		metaData.setPlatform_type("Oracle Commerce");
		metaData.setPlatform_version(getConfiguration().getModuleVersion());
		metaData.setPlatform_affirm("v1.0");
		affirmOrder.setMetadata(metaData);
	}

	private void addFinancingProgram(Order order, AffirmOrder affirmOrder) {
		
		RepositoryItem affirmRule = getRuleManager().findAffirmRuleForCheckout(order);
		if (affirmRule != null) {
			String financingProgramId = (String)affirmRule.getPropertyValue(PROPERTY_PROGRAM_ID);
			affirmOrder.setFinancing_program(financingProgramId);
		}
		
	}

	@SuppressWarnings("unchecked")
	private Map<String,Discount> buildDiscountPostData(Order order) {
		
		Map<String,Discount> discounts = new HashMap<String,Discount>();
		List<PricingAdjustment> adjustments = order.getPriceInfo().getAdjustments();
		
		for (PricingAdjustment adjustment : adjustments) {
			if (adjustment.getTotalAdjustment() < 0) {
				Discount discount = new Discount();
				int discountAmount = (int)Math.round(adjustment.getTotalAdjustment() * -100);
				discount.setDiscount_amount(new Integer(discountAmount));
				RepositoryItem promotion = adjustment.getPricingModel();
				String promotionName = (String)promotion.getPropertyValue("displayName");
				discount.setDiscount_display_name(promotionName);
				discounts.put(promotion.getRepositoryId(), discount);
			}
		}
		
		return discounts;
		
	}

	private Billing buildBillingPostData(Order order) {
		
		// Affirm POJOs to be transformed to JSON
		Billing billing = new Billing();
		AffirmAddress affirmAddress = new AffirmAddress();
		Name name = new Name();
		
		AffirmPayment paymentGroup = getAffirmPaymentGroup(order);
		ContactInfo billingAddress = (ContactInfo)paymentGroup.getBillingAddress();
		
		name.setFirst(billingAddress.getFirstName());
		name.setLast(billingAddress.getLastName());
		billing.setName(name);
		
		affirmAddress.setLine1(billingAddress.getAddress1());
		affirmAddress.setLine2(billingAddress.getAddress2());
		affirmAddress.setCity(billingAddress.getCity());
		affirmAddress.setState(billingAddress.getState());
		affirmAddress.setZipcode(billingAddress.getPostalCode());
		billing.setAddress(affirmAddress);
		return billing;
		
	}

	private Merchant buildMerchantPostData() {
		
		Merchant merchant = new Merchant();
		merchant.setUser_cancel_url(getConfiguration().getAffirmCancelUrl());
		merchant.setUser_confirmation_url(getConfiguration().getAffirmConfirmationUrl());
		merchant.setPublic_api_key(getConfiguration().getPublicAPIKey());
		return merchant;
		
	}

	@SuppressWarnings("unchecked")
	private List<AffirmItem> buildItemPostData(Order order) {
		
		List<AffirmItem> affirmItems = new ArrayList<AffirmItem>();
		List<CommerceItem> commerceItems = order.getCommerceItems();
		
		for (CommerceItem commerceItem : commerceItems) {
			AffirmItem affirmItem = new AffirmItem();
			RepositoryItem sku = (RepositoryItem) commerceItem.getAuxiliaryData().getCatalogRef();
			affirmItem.setDisplay_name((String)sku.getPropertyValue("displayName"));
			affirmItem.setSku(sku.getRepositoryId());
			long itemQuantity = commerceItem.getQuantity();
			affirmItem.setQty(new Integer((int)itemQuantity));
			double unitPrice = commerceItem.getPriceInfo().getAmount() / itemQuantity;
			affirmItem.setUnit_price((int)Math.round(unitPrice * 100));
			// TODO: Item and image URLs will be implementation specific
			affirmItem.setItem_url("");
			affirmItem.setItem_image_url("");
			affirmItems.add(affirmItem);
		}
		
		return affirmItems;
		
	}

	@SuppressWarnings("unchecked")
	private Shipping buildShipmentPostData(Order order) {
		
		List<ShippingGroup> shippingGroups = order.getShippingGroups();
		Shipping shipping = new Shipping();
		
		for (ShippingGroup shippingGroup : shippingGroups) {
			
			if (shippingGroup instanceof HardgoodShippingGroup) {
				ContactInfo address = (ContactInfo) ((HardgoodShippingGroup)shippingGroup).getShippingAddress();
				addShippingInfo(shipping, address);
				break;
			}
			
		}

		return shipping;

	}

	private void addShippingInfo(Shipping shipping, ContactInfo address) {
		
		AffirmAddress affirmAddress = new AffirmAddress();
		affirmAddress.setLine1(address.getAddress1());
		affirmAddress.setLine2(address.getAddress2());
		affirmAddress.setCity(address.getCity());
		affirmAddress.setState(address.getState());
		affirmAddress.setZipcode(address.getPostalCode());
		shipping.setAddress(affirmAddress);
		Name name = new Name();
		name.setFirst(address.getFirstName());
		name.setLast(address.getLastName());
		shipping.setName(name);
		shipping.setPhone(address.getPhoneNumber());
		shipping.setEmail(address.getEmail());
		
	}

	private String buildShipmentPostData(String orderId, ContactInfo shipAddress, String shipmentCarrier, String shipmentTrackingNumber) {
		

		UpdateShipment shipment = new UpdateShipment();
		
		if (! StringUtils.isEmpty(shipmentCarrier)) {
			shipment.setShipping_carrier(shipmentCarrier);
		}
		
		shipment.setOrder_id(orderId);
		
		if (! StringUtils.isEmpty(shipmentTrackingNumber)) {
			shipment.setShipping_confirmation(shipmentTrackingNumber);
		}
		
		Shipping shipping = new Shipping();
		addShippingInfo(shipping, shipAddress);
		shipment.setShipping(shipping);
		
		Gson gson = new Gson();
		return gson.toJson(shipment);	
		
	}

	/*
	 * Common code for making the API calls and reading the result.
	 */
	private String makeApiCall(URL url, String content) {

		HttpURLConnection httpsConnection = null;
		StringBuilder result = new StringBuilder();
		
		if (isLoggingDebug()) {
			logDebug("API Call being made to Affirm: " + content);
		}

		try {
			
			httpsConnection = getHttpUrlConnection(url);

			OutputStreamWriter os = new OutputStreamWriter(httpsConnection.getOutputStream());
			os.write(content);
			os.close();

			InputStreamReader is = null;
			boolean error = false;
			
			if (httpsConnection.getResponseCode() >= 400) {
				error=true;
				is = new InputStreamReader(httpsConnection.getErrorStream(), Charset.forName("UTF-8"));
			} else {
				// Read API response; returned as JSON object
				is = new InputStreamReader(httpsConnection.getInputStream(), Charset.forName("UTF-8"));
			}
			
			BufferedReader reader = new BufferedReader(is);

			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}

			if (error) {
				if (isLoggingError()) {
					logError("API failure: " + result.toString());
				}
			} else {
				if(isLoggingDebug()){
					logDebug("API call result: " + result.toString());
				}
			}
			
		} catch (IOException e) {
			
			if (isLoggingError()) {
				logError("Exception making API call", e);
			}
			
		} finally {

			// End API connection
			if (httpsConnection != null) {
				httpsConnection.disconnect();
			}

		}

		return result.toString();
		
	}


	/**
	 * Returns the Affirm payment group for the order.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public AffirmPayment getAffirmPaymentGroup(Order pOrder) {
		
		if (pOrder == null) {
			if (isLoggingDebug()) {
				logDebug("Null order passed to getAffirmPaymentGroup method.");
			}

			return null;
		}

		List<PaymentGroup> paymentGroups = pOrder.getPaymentGroups();

		if (paymentGroups == null) {
			if (isLoggingWarning()) {
				logWarning("Order has null list of payment groups.");
			}

			return null;
		}

		int numPayGroups = paymentGroups.size();

		if (numPayGroups == 0) {
			if (isLoggingWarning()) {
				logWarning("No Affirm payment group on this order!");
			}

			return null;
		}

		// We are only supporting a single credit card payment group. Return the
		// first one we get.
		for (PaymentGroup paymentGroup : paymentGroups) {
			if (paymentGroup instanceof AffirmPayment) {
				return (AffirmPayment) paymentGroup;
			}
		}

		return null;
		
	}

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
				// We can add additional criteria here
			}
		}
		// now remove relationships
		for (String removeId : removeRels) {
			this.getPaymentGroupManager().removeAllRelationshipsFromPaymentGroup(order, removeId);
		}
		return true;
	}
	
	
	/**
	 * Checks if the order has affirm payment group
	 * 
	 * @param order
	 * @return
	 */
	public boolean isAffirmCheckout(Order order) {

		if (order.getPaymentGroups() != null && !order.getPaymentGroups().isEmpty()) {
			
			for (Object payGroupObj : order.getPaymentGroups()) {

				PaymentGroup payGroup = (PaymentGroup) payGroupObj;

				if (isLoggingDebug()) {
					logDebug(">>>>PAYGROUP CLASS TYPE:::"
							+ payGroup.getPaymentGroupClassType());
				}
				if (payGroup.getPaymentGroupClassType().equalsIgnoreCase("affirmPayment")) {
					return true;
				}
			}
		}

		return false;

	}
	
	

	public PaymentGroupManager getPaymentGroupManager() {
		return paymentGroupManager;
	}

	public void setPaymentGroupManager(PaymentGroupManager paymentGroupManager) {
		this.paymentGroupManager = paymentGroupManager;
	}

	public AffirmPaymentConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(AffirmPaymentConfiguration configuration) {
		this.configuration = configuration;
	}
	
	public SimpleOrderManager getOrderManager() {
		return orderManager;
	}
	
	public void setOrderManager(SimpleOrderManager orderManager) {
		this.orderManager = orderManager;
	}
	
	public AffirmRuleManager getRuleManager() {
		return ruleManager;
	}

	public void setRuleManager(AffirmRuleManager ruleManager) {
		this.ruleManager = ruleManager;
	}	
	

	/******** Code below is for testing and invoking methods from dyn/admin ******/
	// test
	private String testOrderId;
	private String testCheckoutToken;
	private String testChargeId;

	public String getTestOrderId() {
		return testOrderId;
	}

	public void setTestOrderId(String testOrderId) {
		this.testOrderId = testOrderId;
	}

	public String getTestCheckoutToken() {
		return testCheckoutToken;
	}

	public void setTestCheckoutToken(String testCheckoutToken) {
		this.testCheckoutToken = testCheckoutToken;
	}

	public String getTestChargeId() {
		return testChargeId;
	}

	public void setTestChargeId(String testChargeId) {
		this.testChargeId = testChargeId;
	}
	
	

	public String testAuthorize() throws ParseException {
		if (isLoggingDebug()) {
			logDebug("Invoked testAuthWithReturn with orderId:"
					+ this.getTestOrderId() + ":" + this.getTestCheckoutToken());
		}

		if (StringUtils.isBlank(this.getTestOrderId())
				|| StringUtils.isBlank(this.getTestCheckoutToken())) {
			return "Please provide both testOrderId and checkout Token";
		}

		AffirmResponse authResponse = this.authorizeCharge(this.getTestOrderId(),
				this.getTestCheckoutToken());
		
		if(isLoggingDebug())
			logDebug("testAuth Response = " + authResponse.toString());

		return null;

	}

	public String testCapture() throws ParseException {
		
		if (isLoggingDebug()) {
			logDebug("Invoked testCapture with orderId: " + this.getTestOrderId() + ":" + this.getTestChargeId());
		}

		if (StringUtils.isBlank(this.getTestOrderId()) || StringUtils.isBlank(this.getTestChargeId())) {
			return "Please provide both testOrderId and testChargeId";
		}

		AffirmResponse captureResponse = this.captureCharge(this.getTestOrderId(), this.getTestChargeId());
		
		if(isLoggingDebug()) {
			logDebug("testCapture Response = " + captureResponse.toString());
		}

		return null;

	}
	
	public String testVoid() throws ParseException {
		
		if (isLoggingDebug()) {
			logDebug("Invoked testVoid with orderId:" + this.getTestOrderId() + ":" + this.getTestChargeId());
		}

		if (StringUtils.isBlank(this.getTestChargeId())) {
			return "Please provide test charge id";
		}

		AffirmResponse voidResponse = this.voidCharge(this.getTestChargeId());
		if(isLoggingDebug()) {
			logDebug("testVoid Response = " + voidResponse.toString());
		}

		return null;

	}
	
}
