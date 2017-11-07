<dsp:page>
	<dsp:importbean bean="/atg/commerce/ShoppingCart"/>
	<dsp:importbean var="billingFormHandler" bean="/affirm/commerce/order/purchase/AffirmBillingFormHandler" />

	<dsp:tomap var="order" bean="ShoppingCart.current"/>
			<c:choose>
								
				<c:when test="${!empty param.checkout_token}">
					
      <c:forEach var="pageParameter" items="${param}">
        <li> <c:out value="${pageParameter.key}" /> = <c:out value="${pageParameter.value}" />
      </c:forEach>


					<dsp:setvalue bean="AffirmBillingFormHandler.moveToConfirmSuccessURL" value="confirmResponse.jsp"/>
					<dsp:setvalue bean="AffirmBillingFormHandler.moveToConfirmErrorURL" value="billing.jsp"/>
					<dsp:setvalue bean="AffirmBillingFormHandler.checkoutToken" paramvalue="checkout_token"/>
					<dsp:setvalue bean="AffirmBillingFormHandler.affirmConfirmPayment" paramvalue="checkout_token"/>


				</c:when>
				<c:otherwise>
					Show Error:
      <c:forEach var="pageParameter" items="${param}">
        <li> <c:out value="${pageParameter.key}" /> = <c:out value="${pageParameter.value}" />
      </c:forEach>

				</c:otherwise>
			</c:choose>

</dsp:page>

