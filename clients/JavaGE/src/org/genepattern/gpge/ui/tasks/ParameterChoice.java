package org.genepattern.gpge.ui.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
	 * Parses the String and returns a ParameterChoice
	 * 
	 * @param string
	 *            Description of the Parameter
	 * @return Description of the Return Value
	 */
	public static ParameterChoice createChoiceItem(final String string) {
		final int index = string.indexOf('=');
		ParameterChoice choice = null;
		if (index < 0) {
			choice = new ParameterChoice(string, string);
		} else {
			choice = new ParameterChoice(string.substring(index + 1), string
					.substring(0, index));
		}
		return choice;
	}

	/**
	 * Creates a new ParameterChoice by parsing the given string
	 * 
	 * @param s
	 * @return new ParameterChoice
	 */
	public static ParameterChoice[] createChoice(String s) {
		final StringTokenizer tokenizer = new StringTokenizer(s, ";");
		List list = new ArrayList();
		for (int i = 0; tokenizer.hasMoreTokens(); i++) {
			final String token = tokenizer.nextToken();
			ParameterChoice item = createChoiceItem(token);
			list.add(item);

		}
		if (list.size() == 0) {
			return null;
		}
		return (ParameterChoice[]) list.toArray(new ParameterChoice[0]);

	}

	public final String toString() {
		return uiText;
	}

	public boolean equalsCmdLineOrUIValue(String s) {
		return uiText.equals(s) || commandLineValue.equals(s);
	}

	public String getUIValue() {
		return uiText;
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
