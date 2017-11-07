<dsp:page>
	<dsp:importbean bean="/atg/commerce/ShoppingCart"/>
	<dsp:importbean var="billingFormHandler" bean="/affirm/commerce/order/purchase/AffirmBillingFormHandler" />


	<dsp:setvalue bean="AffirmBillingFormHandler.cancelSuccessUrl" value="billing.jsp"/>
	<dsp:setvalue bean="AffirmBillingFormHandler.cancelErrorUrl" value="billing.jsp"/>
	<dsp:setvalue bean="AffirmBillingFormHandler.cancelAffirmPayment" value="true"/>
				

</dsp:page>

