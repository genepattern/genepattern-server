/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.util;

import java.lang.reflect.*;




/**
 * @author Liefeld
 * 
 */

public class AuthorizationManagerFactoryImpl implements IAuthorizationManagerFactory {
	  
	public AuthorizationManagerFactoryImpl(){}

	public static IAuthorizationManager getDefaultAuthorizationManager(){
		return new AuthorizationManager();
	}


	public IAuthorizationManager getAuthorizationManager(){

		String className = System.getProperty("org.genepattern.AuthorizationManagerFactory");

		if (className == null) return getDefaultAuthorizationManager();

		try {
			Class cl = Class.forName(className);
			Constructor construct = cl.getConstructor(new Class[0]);
		
			IAuthorizationManagerFactory factory = (IAuthorizationManagerFactory)construct.newInstance(new Object[0]);
	
			return factory.getAuthorizationManager();
		} catch (Exception e) {
			e.printStackTrace();

			return getDefaultAuthorizationManager();

		}
	}

}