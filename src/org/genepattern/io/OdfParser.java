package org.genepattern.io;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Class for reading an ODF document using callbacks.
 * 
 * @author Joshua Gould
 */
public class OdfParser {
	IOdfHandler handler;
	LineNumberReader reader;
	int dataLines;
   private final static String COMMENT = "#";
   
	final static List VERSIONS = Arrays.asList(new String[] { "ODF 1.0",
			"SDF 1.0" });

	public OdfParser() {
	}

	/**
	 * Parse an ODF document. The application can use this method to instruct
	 * the ODF reader to begin parsing an ODF document from any valid input
	 * source. Applications may not invoke this method while a parse is in
	 * progress (they should create a new ODFParser instead ). Once a parse is
	 * complete, an application may reuse the same ODFParser object, possibly
	 * with a different input source. During the parse, the ODFParser will
	 * provide information about the ODF document through the registered ODF
	 * event handler. This method is synchronous: it will not return until
	 * parsing has ended. If a client application wants to terminate parsing
	 * early, it should throw an exception.
	 * 
	 * @param is
	 *            The input stream
	 * @throws ParseException -
	 *             Any parse exception, possibly wrapping another exception.
	 * @throws IOException -
	 *             An IO exception from the parser, possibly from a byte stream
	 *             or character stream supplied by the application.
	 */
	public void parse(InputStream is) throws ParseException, IOException {
		this.reader = new LineNumberReader(new InputStreamReader(is));
		parseHeader();
		parseData();
	}

   private String readLine() throws IOException {
      String line = reader.readLine();
      if(line!=null && line.trim().startsWith(COMMENT)) {
         return readLine();  
      } else if(line!=null && line.trim().equals("")) { // skip blank lines
         return readLine();  
      }
      return line;
   }
   
	/**
	 * Returns <code>true</code> if this parser can claims to be able to
	 * decode the given stream upon brief examination, <code>false</code>
	 * otherwise
	 * 
	 * @param is
	 *            The input stream
	 * @return Whether this parser can decode the given stream
	 * @exception IOException
	 *                If an error occurs while reading from the stream
	 */
	public boolean canDecode(InputStream is) throws IOException {//delete this
																 // method??
		this.reader = new LineNumberReader(new InputStreamReader(is));
		return isSupportedVersion(readLine());
	}

	void parseData() throws ParseException, IOException {
		if (dataLines == 0) {
			return;
		}
		// read the 1st line to get the number of columns. Check to make sure
		// all rows in data block have the same number of rows
		String line = readLine().trim();// trim b/c old odf writer adds
											   // an extra tab at the end of the
											   // line
		String[] tokens = line.split("\t");

		int expectedColumns = tokens.length;

		for (int j = 0; j < expectedColumns; j++) {
			if (handler != null) {
				handler.data(0, j, tokens[j]);
			}
		}
		int i = 1;// already read 1st row above

		for (; i < dataLines; i++) {
			line = readLine().trim();// trim b/c old odf writer adds an
											// extra tab at the end of the line
			if (line == null) {
				int dataLinesRead = i + 1;
				throw new ParseException("Unexpected end of file on line "
						+ reader.getLineNumber() + ".");
			}
			tokens = line.split("\t");
			if (tokens.length != expectedColumns) {
				throw new ParseException("Expecting " + expectedColumns
						+ " tokens on line " + reader.getLineNumber()
						+ ", but found " + tokens.length + " tokens.");
			}

			for (int j = 0; j < expectedColumns; j++) {
				if (handler != null) {
					handler.data(i, j, tokens[j].trim());
				}
			}
		}

		// see if there are extra lines
		for (String s = readLine(); s != null; s = readLine()) {
			if (!s.trim().equals("")) {
				throw new ParseException("Extra data rows on line "
						+ reader.getLineNumber() + ".");
			}
		}
	}

	void parseHeader() throws ParseException, IOException {
		String versionLine = readLine();

		if (!isSupportedVersion(versionLine)) {
			throw new ParseException("First line must be one of " + VERSIONS);
		}

		int headerLines = 0;
		try {
			headerLines = getIntValue(readLine());
		} catch (NumberFormatException nfe) {
			throw new ParseException("Header lines is not a number.");
		}
		boolean modelFound = false;
		boolean dataLinesFound = false;

		for (int i = 0; i < headerLines; i++) {
			String line = readLine();
			int delimitterIndex = 0;
			int equalsIndex = line.indexOf("=");
			int semiIndex = line.indexOf(":");
			if (equalsIndex == -1 && semiIndex == -1) {
				throw new ParseException("Invalid header " + line);
			}

			if (semiIndex == -1) {
				semiIndex = Integer.MAX_VALUE;
			}
			if (equalsIndex == -1) {
				equalsIndex = Integer.MAX_VALUE;
			}

			if (equalsIndex > 0 && equalsIndex < semiIndex) {
				delimitterIndex = equalsIndex;
				String key = line.substring(0, delimitterIndex).trim();
				String value = line.substring(delimitterIndex + 1, line
						.length());
				key = key.trim();
				value = value.trim();
            if(handler!=null) {
               handler.header(key, value);
            }
				if (key.equalsIgnoreCase("model")) {
					modelFound = true;
				}
				if (key.equalsIgnoreCase("DataLines")) {
					dataLinesFound = true;
					try {
						if ((dataLines = Integer.parseInt(value)) < 0) {
							throw new ParseException(key + " must be >= 0.");
						}
					} catch (NumberFormatException nfe) {
						throw new ParseException(key + " is not a number.");
					}
				}
			} else if (semiIndex > 0 && semiIndex < equalsIndex) {
				delimitterIndex = semiIndex;
				String key = line.substring(0, delimitterIndex);
				key = key.trim();
				String values = line.substring(delimitterIndex + 1, line
						.length());
				values = values.trim();
				String[] tokens = values.split("\t");
				if (handler != null) {
					handler.header(key, tokens);
				}
			} else {
				throw new ParseException("Invalid header " + line);
			}

		}
		if (!dataLinesFound) {
			throw new ParseException("Missing DataLines header");
		}
		if (!modelFound) {
			throw new ParseException("Missing Model header");
		}
		if (handler != null) {
			handler.endHeader();
		}
	}

	/**
	 * Allow an application to register an event handler. If the application
	 * does not register a handler, all content events reported by the ODF
	 * parser will be silently ignored. Applications may register a new or
	 * different handler in the middle of a parse, and the ODF parser must begin
	 * using the new handler immediately.
	 * 
	 * @param handler
	 *            The new handler value
	 */
	public void setHandler(IOdfHandler handler) {
		this.handler = handler;
	}

	boolean isSupportedVersion(String version) {
		if (version == null) {
			return false;
		}
		version = version.trim();
		for (int i = 0; i < VERSIONS.size(); i++) {
			String s = (String) VERSIONS.get(i);
			if (s.equalsIgnoreCase(version)) {
				return true;
			}
		}
		return false;
	}

	private int getIntValue(String s) throws NumberFormatException {
		String[] tokens = s.split("=");

		return Integer.parseInt(tokens[1].trim());
	}

}

