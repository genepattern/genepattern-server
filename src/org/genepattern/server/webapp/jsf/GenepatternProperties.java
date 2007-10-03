/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.util.Map;

/**
 * @author jrobinso
 * 
 * Managed bean to expose genepattern properties to JSF pages.  Currently Genepattern merges
 * all of its properties to System.properties at startup time, so this implementation just 
 * returns system properties
 *
 */
public class GenepatternProperties {

  public Map getProperties() {
	  return System.getProperties();
  }
}
