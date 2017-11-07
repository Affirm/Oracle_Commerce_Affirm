<dsp:page>
	<dsp:importbean bean="/atg/dynamo/droplet/RQLQueryForEach" />
	<dsp:importbean bean="/atg/dynamo/droplet/ForEach" />
	<dsp:importbean var="processingFormHandler" bean="/affirm/commerce/order/purchase/AffirmProcessingFormHandler" />

<style type="text/css">
body {
    font-size: 11px;
}
</style>

<p><a href="affirm_orders.jsp">All Affirm Orders</a></p>

<p><b>Update Order Shipping</b></p>



  <dsp:droplet name="/atg/dynamo/droplet/ErrorMessageForEach">
       <dsp:param name="exceptions" bean="AffirmProcessingFormHandler.formExceptions"/>
       <dsp:oparam name="output">
         
          <li><dsp:valueof param="message"/></li>
         
      </dsp:oparam>
    </dsp:droplet>



<dsp:droplet name="/affirm/commerce/order/AffirmOrderLookup">
  <dsp:param name="orderId" value="${param.orderId}"/>
 <dsp:oparam name="error">
  <p>
  ERROR:
  	<dsp:valueof param="errorMsg">no error message</dsp:valueof>
  
  <p>
 </dsp:oparam>

 <dsp:oparam name="output">
  <dsp:getvalueof var="orderInfo" param="result" />
  <dsp:getvalueof var="orderId" param="result.id"/>

<dsp:form method="post" formid="${orderId}">

	<dsp:input type="hidden" bean="AffirmProcessingFormHandler.orderId" value="${orderId}"/>


	<dsp:getvalueof var="hgShippingGroup" param="result.shippingGroups[0]"/>

	<dsp:tomap var="hgShippingGroup"  param="result.shippingGroups[0]" recursive="true"/>

	<b>Order Id: ${orderId}</b><br/>
	<b>ChargeId: ${param.chargeId}</b><br/>

	<br/>FirstName: <dsp:input bean="AffirmProcessingFormHandler.address.firstName" value="${hgShippingGroup.shippingAddress.firstName}"/>
	<br/>LastName: <dsp:input bean="AffirmProcessingFormHandler.address.lastName" value="${hgShippingGroup.shippingAddress.lastName}"/>
	<br/>Address1: <dsp:input bean="AffirmProcessingFormHandler.address.address1" value="${hgShippingGroup.shippingAddress.address1}"/>
	<br/>Address2: <dsp:input bean="AffirmProcessingFormHandler.address.address2" value="${hgShippingGroup.shippingAddress.address2}"/>
	<br/>City:     <dsp:input bean="AffirmProcessingFormHandler.address.city" value="${hgShippingGroup.shippingAddress.city}"/>
	<br/>State:    <dsp:input bean="AffirmProcessingFormHandler.address.state" value="${hgShippingGroup.shippingAddress.state}"/>
	<br/>ZipCode:  <dsp:input bean="AffirmProcessingFormHandler.address.postalCode" value="${hgShippingGroup.shippingAddress.postalCode}"/>


	<br/>Carrier: <dsp:input bean="AffirmProcessingFormHandler.shipmentCarrier" value="${hgShippingGroup.carrierCode}"/>
	<br/>Tracking: <dsp:input bean="AffirmProcessingFormHandler.shipmentTracking" size="15" value="${hgShippingGroup.trackingNumber}"/> 


	<dsp:input type="hidden" bean="AffirmProcessingFormHandler.successUrl" value="update_shipping.jsp?orderId=${orderId}"/>
	<dsp:input type="hidden" bean="AffirmProcessingFormHandler.errorUrl" value="update_shipping.jsp?orderId=${orderId}"/>

	<br/><dsp:input type="submit" value="Update Shipping" bean="AffirmProcessingFormHandler.updateShipping"/>

</dsp:form>  

 </dsp:oparam>
</dsp:droplet>



</dsp:page>
