package org.genepattern.integration;

/**
 * Handles configuration for finding and logging into GenePattern.
 * 
 * By default, it returns localhost and test/test.  This can be
 * overridden using System properties.  To override the system 
 * properties, use the -D commandline option.  For example:
 * 
 * java -DgenePatternUrl="http://genepattern.broad.mit.edu" -Dusername="bob" -Dpassword="secret"
 * @author jnedzel
 *
 */
public class GenePatternConfig {
	
	/** default url for GenePattern */
	public static final String DEFAULT_GP_URL = "http://localhost:8020";
	
	/** default username for GenePattern */
	public static final String DEFAULT_USERNAME = "test";
	
	/** default password for GenePattern */
	public static final String DEFAULT_PASSWORD = "test";
	
	/** System properties key for GenePattern url to override default */
	public static final String GENE_PATTERN_URL_KEY = "genePatternUrl";
	
	/** System properties key for GenePattern url to override default */
	public static final String USERNAME_KEY = "username";
	
	/** System properties key for GenePattern url to override default */
	public static final String PASSWORD_KEY = "password";
	
	public GenePatternConfig() {
	}
	
	public String getGenePatternUrl() {
		return System.getProperty(GENE_PATTERN_URL_KEY, DEFAULT_GP_URL);
	}

	public String getUsername() {
		return System.getProperty(USERNAME_KEY, DEFAULT_USERNAME);
	}
	
	public String getPassword() {
		return System.getProperty(PASSWORD_KEY, DEFAULT_PASSWORD);
	}
	

}
