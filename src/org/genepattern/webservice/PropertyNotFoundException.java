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
 *                Omnigene Development Code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for the code is held jointly by Whitehead Institute
 * and the the individual authors.  These should be listed in
 * @author doc comments.
 *
 * For more information on the Omnigene project and its aims
 * visit the www.sourceforge.net site.
 *
 */

package org.genepattern.webservice;

/**
 * To throw property could not be located
 * 
 * @author Rajesh Kuttan
 */

public class PropertyNotFoundException extends OmnigeneException {

	/**
	 * Default Constructor
	 */
	public PropertyNotFoundException() {
		super(OmnigeneErrorCode.E_PROPERTY_NOT_FOUND);
	}

	/**
	 * Constructs PropertyNotFoundException with the specified detail message.
	 * 
	 * @param s
	 *            the detail message.
	 *  
	 */
	public PropertyNotFoundException(String s) {
		super(OmnigeneErrorCode.E_PROPERTY_NOT_FOUND, s);
	}

}