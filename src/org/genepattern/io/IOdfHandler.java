package org.genepattern.io;

/**
 * An interface for receiving notification of the content of an odf document
 * 
 * @author Joshua Gould
 */
public interface IOdfHandler {

	public void endHeader() throws ParseException;

	public void header(String key, String[] values) throws ParseException;

	public void header(String key, String value) throws ParseException;

	public void data(int row, int column, String s) throws ParseException;
}

