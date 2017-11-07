package com.affirm.commerce.payment;

import atg.commerce.CommerceException;


/**
 * Defines payment processing through affirm
 * 
 * @author dev
 * 
 */
public interface AffirmPaymentProcessor {

	// -----------------------------------
	/**
	 * Authorize the amount on Affirm
	 * 
	 * @param AffirmPaymentInfo
	 *            the AffirmPaymentInfo reference which contains all the
	 *            authorization data
	 * @return a AffirmPaymentStatus object detailing the results of the
	 *         authorization
	 */
	public AffirmPaymentStatus authorize(AffirmPaymentInfo affirmPaymentInfo) throws CommerceException;

	// -----------------------------------
	/**
	 * Debit the amount on the Affirm after authorization
	 * 
	 * @param AffirmPaymentInfo
	 *            the AffirmPaymentInfo reference which contains all the debit
	 *            data
	 * @param status
	 *            the AffirmPaymentStatus object which contains information
	 *            about the transaction. This should be the object which was
	 *            returned from authorize().
	 * @return a AffirmPaymentStatus object detailing the results of the debit
	 */
	public AffirmPaymentStatus debit(AffirmPaymentInfo affirmPaymentInfo, AffirmPaymentStatus status) throws CommerceException;

	// -----------------------------------
	/**
	 * Credit the amount on the Affirm after debiting.
	 * 
	 * @param AffirmPaymentInfo
	 *            the AffirmPaymentInfo reference which contains all the credit
	 *            data
	 * @param status
	 *            the AffirmPaymentStatus object which contains information
	 *            about the transaction. This should be the object which was
	 *            returned from debit().
	 * @return a AffirmPaymentStatus object detailing the results of the credit
	 */
	public AffirmPaymentStatus credit(AffirmPaymentInfo affirmPaymentInfo, AffirmPaymentStatus status) throws CommerceException;

	// -----------------------------------
	/**
	 * Credit the amount on Affirm
	 * 
	 * @param AffirmPaymentInfo
	 *            the AffirmPaymentInfo reference which contains all the credit
	 *            data
	 * @return a AffirmPaymentStatus object detailing the results of the credit
	 */
	//public AffirmPaymentStatus credit(AffirmPaymentInfo affirmPaymentInfo) throws CommerceException;
	
	public AffirmPaymentStatus credit(AffirmPaymentInfo AffirmPaymentInfo, double amount) throws CommerceException;
	
	
	public AffirmPaymentStatus decreaseAuthorizationForPaymentGroup(AffirmPaymentInfo AffirmPaymentInfo) throws CommerceException;

}
