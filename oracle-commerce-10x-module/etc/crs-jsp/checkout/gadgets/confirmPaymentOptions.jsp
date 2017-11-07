<%--
  This gadget displays payment information for an order specified.

  Required parameters:
    order
      Order, whose payment information should be displayed.

  Optional parameters:
    isCurrent
      Flags, if order specified is a current shopping cart.
    expressCheckout
      Flags, if the user has selected and express checkout option.
--%>

<dsp:page>
  <dsp:importbean bean="/atg/commerce/order/purchase/CommitOrderFormHandler"/>

  <dsp:getvalueof var="paymentGroupRelationships" vartype="java.lang.Object" 
                  param="order.paymentGroupRelationships"/>
  <dsp:getvalueof var="paymentGroupRelationshipCount" vartype="java.lang.String" 
                  param="order.paymentGroupRelationshipCount"/>
  <dsp:getvalueof var="isCurrent" param="isCurrent"/>
  <dsp:getvalueof var="expressCheckout" param="expressCheckout"/>
  
  <c:if test="${isCurrent}">
    <dsp:getvalueof var="creditCardRequired" vartype="java.lang.Boolean" 
                    bean="CommitOrderFormHandler.creditCardRequired"/>
  </c:if>  

  <%-- Default value for expressCheckout is false. --%>
  <c:if test="${empty expressCheckout}">
    <c:set var="expressCheckout" value="false"/>
  </c:if>

  <c:choose>
    <%-- Is it Confirm page from express checkout with non-empty payment? --%>
    <c:when test='${isCurrent && paymentGroupRelationshipCount == "0" && expressCheckout && creditCardRequired}'>
      
      <dsp:getvalueof var="creditCard" bean="CommitOrderFormHandler.creditCard"/>
      
      <%-- Then display cart's credit card. --%>
      <c:if test="${not empty creditCard}">
        <dsp:include page="/checkout/gadgets/paymentGroupRenderer.jsp">
          <dsp:param name="isExpressCheckout" value="true"/>
          <dsp:param name="isCurrent" param="isCurrent"/>
          <dsp:param name="paymentGroup" bean="CommitOrderFormHandler.creditCard"/>
        </dsp:include>
      </c:if>
    
    </c:when>
    <%-- Is it step-by-step checkout? or Order Details page? --%>
    <c:otherwise>
      <%-- Then display all available payment groups (they may be Store Credits or Credit Card). --%>
      <c:forEach var="paymentGroupRelationship" items="${paymentGroupRelationships}">
        
        <dsp:param name="rel" value="${paymentGroupRelationship}"/>
        <dsp:setvalue param="paymentGroup" paramvalue="rel.paymentGroup"/>
        <dsp:getvalueof var="paymentGroupClassType" param="paymentGroup.paymentGroupClassType"/>
        
        <%-- We will display credit cards only, however. --%>
        <c:if test="${paymentGroupClassType == 'creditCard'}">
          <dsp:include page="/checkout/gadgets/paymentGroupRenderer.jsp">
            <dsp:param name="isCurrent" param="isCurrent"/>
            <dsp:param name="paymentGroup" param="paymentGroup"/>
            <dsp:param name="isExpressCheckout" value="${expressCheckout}"/>
          </dsp:include>
        </c:if>

        <c:if test="${paymentGroupClassType == 'affirmPayment'}">
		<dl class="atg_store_groupShippingAddress">
	    <dt>
	      Billing Address: 
	    </dt>
	    <dd>
      
      
          <div class="vcard">
                 
              <div class="fn">
                
                <span><dsp:valueof param="paymentGroup.billingAddress.firstName"/></span>
                <span></span>
                <span><dsp:valueof param="paymentGroup.billingAddress.lastName"/></span>
              </div>  
            
    
            <div class="adr">

                  <div class="street-address">
                    <dsp:valueof param="paymentGroup.billingAddress.address1"/>
                  </div>
                  <div class="street-address">
                      <dsp:valueof param="paymentGroup.billingAddress.address2"/>
                  </div>
                
              
    
              
              	<span class="locality"><dsp:valueof param="paymentGroup.billingAddress.city"/>,</span>
                <span class="region"><dsp:valueof param="paymentGroup.billingAddress.state"/></span>
		<span class="postal-code"><dsp:valueof param="paymentGroup.billingAddress.postalCode"/></span>
              <div class="country-name"><span class="country-name">United States</span>
                  
                
              </div>
            </div>
    
            
            <div class="tel"><dsp:valueof param="paymentGroup.billingAddress.phoneNumber"/></div>
          </div>   

	<br/><a href="affirm_cancel.jsp">Change Payment Type From Affirm</a>    

    </dd>
  </dl>
        </c:if>	
      
      </c:forEach>
    </c:otherwise>
  </c:choose>

</dsp:page>

<%-- @version $Id: //hosting-blueprint/B2CBlueprint/version/11.2/Storefront/j2ee/store.war/checkout/gadgets/confirmPaymentOptions.jsp#1 $$Change: 946917 $--%>
