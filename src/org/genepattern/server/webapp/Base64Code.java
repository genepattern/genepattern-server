/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/


// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.genepattern.server.webapp;

import java.io.UnsupportedEncodingException;

/* ------------------------------------------------------------ */

/**
 * Fast B64 Encoder/Decoder as described in RFC 1421.
 * <p>
 * Does not insert or interpret whitespace as described in RFC 1521. If you
 * require this you must pre/post process your data.
 * <p>
 * Note that in a web context the usual case is to not want linebreaks or other
 * white space in the encoded output.
 * 
 * @version $Revision$
 * @author Brett Sealey (bretts)
 * @author Greg Wilkins (gregw)
 */
public class Base64Code {
	// ------------------------------------------------------------------
	static final char pad = '=';

	static final char[] nibble2code = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
			'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
			'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
			'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
			'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', '+', '/' };

	static byte[] code2nibble = null;

	static {
		code2nibble = new byte[256];
		for (int i = 0; i < 256; i++)
			code2nibble[i] = -1;
		for (byte b = 0; b < 64; b++)
			code2nibble[(byte) nibble2code[b]] = b;
		code2nibble[(byte) pad] = 0;
	}

	// ------------------------------------------------------------------
	/**
	 * Base 64 encode as described in RFC 1421.
	 * <p>
	 * Does not insert whitespace as described in RFC 1521.
	 * 
	 * @param s
	 *            String to encode.
	 * @return String containing the encoded form of the input.
	 */
	static public String encode(String s) throws UnsupportedEncodingException {
		return encode(s, null);
	}

	// ------------------------------------------------------------------
	/**
	 * Base 64 encode as described in RFC 1421.
	 * <p>
	 * Does not insert whitespace as described in RFC 1521.
	 * 
	 * @param s
	 *            String to encode.
	 * @param charEncoding
	 *            String representing the name of the character encoding of the
	 *            provided input String.
	 * @return String containing the encoded form of the input.
	 */
	static public String encode(String s, String charEncoding)
			throws UnsupportedEncodingException {
		byte[] bytes;
		if (charEncoding == null)
			bytes = s.getBytes("ISO-8859-1");
		else
			bytes = s.getBytes(charEncoding);

		return new String(encode(bytes));
	}

	// ------------------------------------------------------------------
	/**
	 * Fast Base 64 encode as described in RFC 1421.
	 * <p>
	 * Does not insert whitespace as described in RFC 1521.
	 * <p>
	 * Avoids creating extra copies of the input/output.
	 * 
	 * @param b
	 *            byte array to encode.
	 * @return char array containing the encoded form of the input.
	 */
	static public char[] encode(byte[] b) {
		if (b == null)
			return null;

		int bLen = b.length;
		char r[] = new char[((bLen + 2) / 3) * 4];
		int ri = 0;
		int bi = 0;
		byte b0, b1, b2;
		int stop = (bLen / 3) * 3;
		while (bi < stop) {
			b0 = b[bi++];
			b1 = b[bi++];
			b2 = b[bi++];
			r[ri++] = nibble2code[(b0 >>> 2) & 0x3f];
			r[ri++] = nibble2code[(b0 << 4) & 0x3f | (b1 >>> 4) & 0x0f];
			r[ri++] = nibble2code[(b1 << 2) & 0x3f | (b2 >>> 6) & 0x03];
			r[ri++] = nibble2code[b2 & 077];
		}

		if (bLen != bi) {
			switch (bLen % 3) {
			case 2:
				b0 = b[bi++];
				b1 = b[bi++];
				r[ri++] = nibble2code[(b0 >>> 2) & 0x3f];
				r[ri++] = nibble2code[(b0 << 4) & 0x3f | (b1 >>> 4) & 0x0f];
				r[ri++] = nibble2code[(b1 << 2) & 0x3f];
				r[ri++] = pad;
				break;

			case 1:
				b0 = b[bi++];
				r[ri++] = nibble2code[(b0 >>> 2) & 0x3f];
				r[ri++] = nibble2code[(b0 << 4) & 0x3f];
				r[ri++] = pad;
				r[ri++] = pad;
				break;

			default:
				break;
			}
		}

		return r;
	}
}
