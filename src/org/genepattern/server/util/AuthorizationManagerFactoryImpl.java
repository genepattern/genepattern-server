/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.genepattern.server.util;




/**
 * @author Liefeld
 * 
 */

public class AuthorizationManagerFactoryImpl implements IAuthorizationManagerFactory {
	  
	public AuthorizationManagerFactoryImpl(){}

	public IAuthorizationManager getAuthorizationManager(){
		return new AuthorizationManager();
	}

}