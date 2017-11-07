package com.affirm.payment;

/**
 * 
 * @author dev
 *
 */
public class AffirmPaymentConstants {

	public static final String AUTH_TXN_ID = "authTxnId";
	public static final String CHARGE_ID = "chargeId";
	public static final String CHECKOUT_TOKEN = "checkoutToken";
	public static final String FINANCING_PROGRAM_ID = "financingProgramId";
	
	public static final int SUCCESS_CODE = 0;
	public static final int ERROR_CODE_AUTH = 1001;
	public static final int ERROR_CODE_VOID = 1002;
	public static final int ERROR_CODE_DEBIT = 1003;
	public static final int ERROR_CODE_CREDIT = 1004;
	public static final int ERROR_CODE_UPDATE = 1005;
	public static final int ERROR_CODE_CHECKOUT = 1006;
	
}
