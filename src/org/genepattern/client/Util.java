package org.genepattern.client;
/**
 *  A utility class used to read user input from the console. Used in the
 *  generated pipeline code to get runtime parameters.
 *
 *@author     Joshua Gould
 */
public class Util {
	private static java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));


	private Util() { }


	/**
	 *  Prompts the user for input from the console.
	 *
	 *@param  prompt                   The user prompt.
	 *@return                          The user entered text.
	 *@exception  java.io.IOException  If an error occurs while reading the input
	 *      from the console.
	 */
	public static String prompt(String prompt) throws java.io.IOException {
		System.out.print(prompt);
		return br.readLine();
	}
}
