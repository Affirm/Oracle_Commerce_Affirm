<dsp:page>
	<dsp:importbean bean="/atg/dynamo/droplet/RQLQueryForEach" />
	<dsp:importbean bean="/atg/dynamo/droplet/ForEach" />
	<dsp:importbean var="billingFormHandler" bean="/affirm/commerce/order/purchase/AffirmProcessingFormHandler" />

<p><b>Orders With Affirm Payment</b></p>

<style type="text/css">
table {
    font-size: 11px;
}
</style>

  <dsp:droplet name="/atg/dynamo/droplet/ErrorMessageForEach">
       <dsp:param name="exceptions" bean="AffirmProcessingFormHandler.formExceptions"/>
       <dsp:oparam name="output">
         
          <li><dsp:valueof param="message"/></li>
         
      </dsp:oparam>
    </dsp:droplet>



	<dsp:droplet name="RQLQueryForEach">
	  <dsp:param name="queryRQL" value="NOT order.state = \"INCOMPLETE\" ORDER BY order.submittedDate SORT DESC"/>
	  <dsp:param name="repository" value="/atg/commerce/order/OrderRepository"/>
	  <dsp:param name="itemDescriptor" value="affirmPaymentGroup"/>
	  <dsp:param name="elementName" value="affirmPaymentGroup"/>
	  
	 
<dsp:oparam name="outputStart">
  <table border="1" style="border:1px;">
  <tr>
     <th>Order Id</th>

     <th>Order State</th>
     <th>Pay Group Id</th>
     <th>Pay Group Status</th>
     <th>Pay Group Details</th>
     <th>Action</th>
     
 
  </tr>
</dsp:oparam>

<dsp:oparam name="outputEnd">
  </table>
</dsp:oparam>

	<dsp:oparam name="output">

<dsp:getvalueof var="chargeId" param="affirmPaymentGroup.chargeId"/>

	   <dsp:getvalueof var="orderId" param="affirmPaymentGroup.order.id"/>
	   <tr>
		<td><dsp:valueof param="affirmPaymentGroup.order.id"/></td>
		<td><dsp:valueof param="affirmPaymentGroup.order.state"/></td>
		<td><dsp:valueof param="affirmPaymentGroup.repositoryId"/></td>
		<td><dsp:valueof param="affirmPaymentGroup.state"/></td>
		<td>
			<dsp:getvalueof var="pgState" param="affirmPaymentGroup.state" />
			<c:if test="${pgState == 'SETTLED' || pgState == 'SETTLE_FAILED' || pgState == 'AUTHORIZED' || pgState == 'VOID' || pgState == 'REFUNDED' || pgState == 'PARTIALLY_REFUNDED'}">
				ChargeId: <dsp:valueof param="affirmPaymentGroup.chargeId"/><br/>
				Auth Txn Id: <dsp:valueof param="affirmPaymentGroup.authTxnId"/><br/>
				Auth On: <dsp:valueof param="affirmPaymentGroup.authorizationStatus[0].transactionTimestamp" date="M/dd/yyyy HH:mm:ss"/><br/>
			</c:if>
			<c:if test="${pgState == 'VOID'}">
				<br/>Void On: <dsp:valueof param="affirmPaymentGroup.authorizationStatus[1].transactionTimestamp" date="M/dd/yyyy HH:mm:ss"/>
				<br/>Void Txn Id: <dsp:valueof param="affirmPaymentGroup.authorizationStatus[1].transactionId"/>
			</c:if>
			<c:if test="${pgState == 'SETTLED' || pgState == 'REFUNDED' || pgState == 'PARTIALLY_REFUNDED'}">
				<br/>Settled On: <dsp:valueof param="affirmPaymentGroup.debitStatus[0].transactionTimestamp" date="M/dd/yyyy HH:mm:ss"/>
				<br/>Capture Txn Id: <dsp:valueof param="affirmPaymentGroup.debitStatus[0].transactionId"/><br/>
			</c:if>
			<c:if test="${pgState == 'REFUNDED' || pgState == 'PARTIALLY_REFUNDED'}">
				

<dsp:droplet name="ForEach">
    <dsp:param name="array" param="affirmPaymentGroup.creditStatus"/>
    <dsp:param name="elementName" value="creditStatusElement"/>
    <dsp:param name="sortProperties" value="-transactionTimestamp"/>
    <dsp:oparam name="output">

<dsp:getvalueof var="transactionSuccess" param="creditStatusElement.transactionSuccess"/>

<br/><br/>Refund Time: <dsp:valueof param="creditStatusElement.transactionTimestamp" date="M/dd/yyyy HH:mm:ss"/>
<br/>Refund Txn Id: <dsp:valueof param="creditStatusElement.transactionId"/>
<br/>Success: <dsp:valueof param="creditStatusElement.transactionSuccess"/>
<c:if test="${transactionSuccess == 'true'}">
	<br/>Refund Amount: <dsp:valueof param="creditStatusElement.amount" converter="currency"/>
</c:if>



    </dsp:oparam>
</dsp:droplet>

			</c:if>
			<c:if test="${pgState == 'SETTLE_FAILED'}">
				<br/>Settle Failed On: <dsp:valueof param="affirmPaymentGroup.debitStatus[0].transactionTimestamp" date="M/dd/yyyy HH:mm:ss"/>
			</c:if>

		</td>
		<td>
			
<dsp:form method="post" formid="${orderId}">

<dsp:input type="hidden" bean="AffirmProcessingFormHandler.orderId" value="${orderId}"/>

			<c:if test="${pgState == 'AUTHORIZED'}">
<br/>Order Amount: <dsp:valueof param="affirmPaymentGroup.amountAuthorized" converter="currency"/><br/>				
<dsp:input type="submit" value="Settle Payment" bean="AffirmProcessingFormHandler.capture"/>
				&nbsp;
				<dsp:input type="submit" value="Void Auth" bean="AffirmProcessingFormHandler.void"/>
			</c:if>
			<c:if test="${pgState == 'SETTLED' || pgState == 'PARTIALLY_REFUNDED'}">
				<br/>Order Amount: <dsp:valueof param="affirmPaymentGroup.amountAuthorized" converter="currency"/>
				<br/>Enter Refund Amount: <dsp:input bean="AffirmProcessingFormHandler.refundAmount" value="0" size="10"/>
				<br/><dsp:input type="submit" value="Refund" bean="AffirmProcessingFormHandler.refund"/>
			</c:if>
			<br/>
			<a target="_blank" href="update_shipping.jsp?orderId=${orderId}&chargeId=${chargeId}"/>Update Shipping</a>	
			
</dsp:form>

		</td>
    
	 </dsp:oparam>

	</dsp:droplet>
</dsp:page>
