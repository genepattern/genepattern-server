/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/


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
 * checks permissions files to see if a given user is allowed to pereform a given action
 * defaults to allowing anything not specifically disallowed
 *
 * basically answers the question, can userX do action Y
 * and also can return a string representing the link, or a failure string to put in place of the link
 */

public interface IAuthorizationManager  {
	  

	public boolean isAllowed(String urlOrSoapMethod, String userID);

	public boolean checkPermission(String permissionName, String userID);

}
