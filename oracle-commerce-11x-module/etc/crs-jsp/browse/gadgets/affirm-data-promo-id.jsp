<%--
  affirm.jsp will try to find an affirm rule to apply, and render data elements for JS use
  
  Required Parameters:
    None
  Optional Parameters:
    product
      the product the user is viewing
    sku
      a specific SKU
 --%>
<dsp:page>

  <%-- Import Required Beans --%>
  <dsp:importbean bean="/atg/commerce/ShoppingCart"/>
  <dsp:importbean bean="/affirm/rules/AffirmRuleDroplet"/>

  <dsp:droplet name="AffirmRuleDroplet">
    <dsp:param name="order" bean="ShoppingCart.current"/>
    <dsp:param name="product" param="product"/>
    <dsp:param name="sku" param="sku"/>
    <dsp:param name="pageType" param="pageType"/>
    <dsp:oparam name="output">
      <dsp:getvalueof var="dataPromoId" param="dataPromoId"/>
      <dsp:getvalueof var="ruleName" param="ruleName"/>
      <dsp:getvalueof var="dataAmount" param="dataAmount"/>
      <c:choose>
        <c:when test="${not empty dataAmount}">
          <p class="affirm-as-low-as" data-promo-id="${dataPromoId}" data-amount="${dataAmount}" data-affirm-color="blue"></p>
        </c:when>
        <c:otherwise>
          <p class="affirm-as-low-as" data-promo-id="${dataPromoId}" data-affirm-color="blue"></p>
        </c:otherwise>
      </c:choose>
      <!-- Affirm data promo id: ${dataPromoId} -->
      <!-- Affirm rule display name: ${ruleName} -->
    </dsp:oparam>
    <dsp:oparam name="empty">
      <!-- No affirm rule match -->      
    </dsp:oparam>
  </dsp:droplet>

</dsp:page>
