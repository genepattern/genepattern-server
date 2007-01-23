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
	  


	public String getCheckedLink(String link, String userID, String failureNote);
	
	public String getCheckedLink(String permission, String link, String userID, String failureNote);


	public boolean isAllowed(String urlOrSoapMethod, String userID);

	public boolean checkPermission(String permissionName, String userID);

}