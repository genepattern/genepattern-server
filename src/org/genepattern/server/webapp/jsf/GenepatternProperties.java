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
