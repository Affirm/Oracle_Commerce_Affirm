package com.affirm.commerce.payment.response;

public class AffirmResponse {

	private String id;
	private String orderId;
	private String transactionId;
	private boolean success;
	private String redirectUrl;
	private int errorCode;
	private String errorMessage;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}
	
	@Override
	public String toString() {
		return "AffirmResponse [id=" + id + ", orderId=" + orderId
				+ ", transactionId=" + transactionId + ", success=" + success
				+ ", errorCode=" + errorCode + ", errorMessage=" + errorMessage
				+ "]";
	}

}
