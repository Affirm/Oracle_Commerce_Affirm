package com.affirm.droplet;

import java.io.IOException;

import javax.servlet.ServletException;

import atg.nucleus.naming.ParameterName;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.DynamoServlet;

public class AjaxCheck extends DynamoServlet {
	
	private static final ParameterName OPARAM_TRUE =  ParameterName.getParameterName("true");
	private static final ParameterName OPARAM_FALSE =  ParameterName.getParameterName("false");

	public void service(DynamoHttpServletRequest request, DynamoHttpServletResponse response) throws ServletException, IOException {
		
		String requestedWith = request.getHeader("X-Requested-With");
		if (requestedWith != null && requestedWith.equals("XMLHttpRequest")) {
			request.serviceParameter(OPARAM_TRUE, request, response);
			return;
		} else {
			request.serviceParameter(OPARAM_FALSE, request, response);
			return;
		}
		
	}

}
