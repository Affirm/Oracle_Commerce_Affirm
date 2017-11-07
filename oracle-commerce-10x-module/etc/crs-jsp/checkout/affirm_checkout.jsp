<dsp:page>
<dsp:importbean bean="/atg/commerce/ShoppingCart"/>
<dsp:importbean bean="/atg/commerce/ShoppingCart"/>
<dsp:importbean var="affirmPaymentConfig" bean="/affirm/commerce/payment/AffirmPaymentConfiguration"/>
<dsp:importbean bean="/affirm/rules/AffirmCheckoutRuleDroplet"/>
<dsp:getvalueof var="cart" bean="ShoppingCart.current"/>

<head>

<script type="text/javascript" src="${affirmPaymentConfig.jsUrl}"></script>
<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js"></script>

<c:set var="shippingGroup" value="${cart.shippingGroups[0]}"/>
<c:set var="paymentGroup" value="${cart.paymentGroups[0]}"/>

<c:set var="orderTotal" value="${cart.priceInfo.total * 100}"/>
<c:set var="shippingPrice" value="${cart.priceInfo.shipping * 100}"/>
<c:set var="tax" value="${cart.priceInfo.tax * 100}"/>
<dsp:getvalueof var="currencyCode" bean="ShoppingCart.current.paymentGroups[0].currencyCode" />

<c:if test="${empty currencyCode}">
<dsp:getvalueof var="currencyCode" value="USD" />
</c:if>

<dsp:droplet name="AffirmCheckoutRuleDroplet">
	<dsp:param name="order" value="${cart}"/>
	<dsp:oparam name="output">
		<dsp:getvalueof var="financingProgramId" param="financingProgramId" scope="request"/>
	</dsp:oparam>
</dsp:droplet>

    <script>


affirm.checkout({

  "config": {
    "financial_product_key" : "${financingProgramId}", //replace with your Affirm financial product key
    "public_api_key": "<dsp:valueof bean="AffirmPaymentConfiguration.publicAPIKey"/>"
  },

  "merchant": {
    "user_cancel_url"              : "<dsp:valueof bean="AffirmPaymentConfiguration.affirmCancelUrl"/>",
    "user_confirmation_url"        : "<dsp:valueof bean="AffirmPaymentConfiguration.affirmConfirmationUrl"/>",
    "user_confirmation_url_action" : "POST"
  },

  //shipping contact
  "shipping": {
    "name": {
      "first" : "${shippingGroup.shippingAddress.firstName}",
      "last" : "${shippingGroup.shippingAddress.lastName}"
      // You can also include the full name
      // "full" : "John Doe"
    },
    "address": {
      "line1"  : "${shippingGroup.shippingAddress.address1}",
      "line2"  : "${shippingGroup.shippingAddress.address2}",
      "city"   : "${shippingGroup.shippingAddress.city}",
      "state"  : "${shippingGroup.shippingAddress.state}",
      "zipcode": "${shippingGroup.shippingAddress.postalCode}"
    },
    "email"          : "${shippingGroup.shippingAddress.email}",
    "phone_number"   : "${shippingGroup.shippingAddress.phoneNumber}"
  },

  //billing contact
  "billing": {
    "name": {
      "full" : "${paymentGroup.billingAddress.firstName} ${paymentGroup.billingAddress.lastName}"
    },
    "address": {
      "line1"  : "${paymentGroup.billingAddress.address1}",
      "line2"  : "${paymentGroup.billingAddress.address2}",
      "city"   : "${paymentGroup.billingAddress.city}",
      "state"  : "${paymentGroup.billingAddress.state}",
      "zipcode": "${paymentGroup.billingAddress.postalCode}"
    }
  },

  // cart 
  "items": [
 

<c:forEach items="${cart.commerceItems}" var="commerceItem">
   

{
    "display_name"   : "${commerceItem.auxiliaryData.productRef.displayName}",
    "sku"            : "${commerceItem.catalogRefId}",
    "unit_price"     : ${commerceItem.priceInfo.amount * 100},
    "qty"            : 1,
    "item_image_url" : "https://examplemerchant.com/static/item.png",
    "item_url"       : "https://examplemerchant.com/acme-slr-ng-01.htm",
  }
  
  ,

</c:forEach>

],

"discounts": {
<c:forEach items="${cart.priceInfo.adjustments}" var="adjustment">
   
	<c:if test="${adjustment.totalAdjustment < 0}">
		<c:set var="discountAmount" value="${adjustment.totalAdjustment * -100}"/>
		<dsp:getvalueof var="promo" value="${adjustment.pricingModel}"/>
	
		"${promo.repositoryId}":{
			"discount_amount" :${discountAmount},
		"discount_display_name": "${promo.displayName}"
		}
	
	,
	</c:if>

</c:forEach>
},

  // pricing / charge amount
  "currency"        : "${currencyCode}",
  "tax_amount"      : ${tax},
  "shipping_amount" : ${shippingPrice},
  "total"           : ${orderTotal}
});


$(function() {
      $("#button").click( function()
           {
             //alert('button clicked');
           }
      );
});

$(function() {
      $("#submit-form").click( function()
           {
             //alert('button clicked to submit');
affirm.checkout.post();
//alert("post done");
           }
      );
});


$("document").ready(function() {
//alert("ready to submit");
    setTimeout(function() {
        $("#submit-form").trigger('click');
    },1000);
});

    </script>

</head>

<body>
 
<form id="submit-form" method="post">
Submitting to affirm....
<input class="btn btn-primary" type="button" value="Affirm Checkout" id="submit-form" style="display:none"/>

</form>


</body>

</dsp:page>
