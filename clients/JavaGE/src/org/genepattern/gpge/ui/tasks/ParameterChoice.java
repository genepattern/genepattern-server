package org.genepattern.gpge.ui.tasks;

/**
 * 
 * @author Joshua Gould
 *
 */
public class ParameterChoice {

	/** the text that is displayed to the user */
	private String uiText;

	/** the command line value */
	private String commandLineValue;

	public ParameterChoice(final String cmdLineValue) {
		this.uiText = cmdLineValue;
		this.commandLineValue = cmdLineValue;
	}
	
	public ParameterChoice(final String uiText, final String value) {
		this.uiText = uiText;
		this.commandLineValue = value;

	}
	
	/**
	 * Creates a new ParameterChoice by parsing the given string
	 * @param s
	 * @return new ParameterChoice
	 */
	public static ParameterChoice createChoice(String s) {
		String[] tokens = s.split("=");
		if(tokens.length==2) {
			return new ParameterChoice(tokens[1], tokens[0]);
		}
		return new ParameterChoice(tokens[0]);
	}

	
	
	public final String toString() {
		return uiText;
	}

	public boolean equalsCmdLineOrUIValue(String s) {
		return uiText.equals(s) || commandLineValue.equals(s);
	}

	/**
	 * returns the command line value (which is not displayed to the user)
	 * 
	 * @return The value
	 */
	public final String getValue() {
		return commandLineValue;
	}
}
