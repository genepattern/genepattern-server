/*
 * StringUtils.java
 *
 * Created on March 10, 2003, 12:47 PM
 */

package org.genepattern.util;

/**
 * Static methods for String manipulation
 * 
 * @author kohm
 */
public class StringUtils {

	/** Creates a new instance of StringUtils */
	private StringUtils() {
	}

	/**
	 * returns a new String where all occurances of the first string are
	 * replaced with the second string
	 */
	public static final String replaceAll(final String source,
			final String find, final String replace) {
		final StringBuffer sb = new StringBuffer(source);
		final int diff = replace.length() - find.length();
		final int len = find.length();
		for (int i = source.indexOf(find), offset = 0; i >= 0; i = source
				.indexOf(find, i + 1)) {
			final int strt = i + offset, end = strt + len;
			sb.replace(strt, end, replace);
			offset += diff;
		}
		return sb.toString();
	}

	/**
	 * creates an array of Strings by parsing the input String using the
	 * delimiter
	 */
	public static final String[] splitStrings(final String text,
			final char delim) {
		final int num = getNumOccurances(text, delim) + 1;
		if (num == 1) {
			return new String[] { text.trim() };
		}
		//System.out.println("num="+num);
		final String[] strings = new String[num];
		for (int c = 0, last = 0, i = text.indexOf(delim); i >= 0; i = text
				.indexOf(delim, i)) {
			strings[c++] = text.substring(last, i).trim();
			last = ++i;
		}
		strings[num - 1] = text.substring(text.lastIndexOf(delim) + 1).trim();
		return strings;
	}

	/**
	 * creates an array of Strings by parsing the input String using the
	 * delimiter
	 */
	public static final String[] splitStrings(final String text,
			final String delim) {
		final int num = getNumOccurances(text, delim);
		if (num == 0) {
			return new String[] { text.trim() };
		}
		final String[] strings = new String[num];
		final int del_len = delim.length() - 1;
		for (int c = 0, last = 0, i = text.indexOf(delim); i >= 0; i = text
				.indexOf(delim, i)) {
			strings[c++] = text.substring(last, i).trim();
			last = i + del_len;
		}
		return strings;
	}

	/** gets the number of times the delimiter is present in the String */
	public static final int getNumOccurances(final String text, final char delim) {
		int count = 0;
		final int len = text.length() - 1;
		for (int i = text.indexOf(delim); i >= 0 && i < len; i = text.indexOf(
				delim, ++i)) {
			//System.out.println("i="+i);
			count++;
		}
		return count;
	}

	/** gets the number of times the delimiter is present in the String */
	public static final int getNumOccurances(final String text,
			final String delim) {
		int count = 0;
		for (int i = text.indexOf(delim); i >= 0; i = text.indexOf(delim, ++i)) {
			count++;
		}
		return count;
	}

	/**
	 * counts the number of non-null elements that have a trimed length of at
	 * least 1
	 */
	public static final int countNonEmpty(final String[] array) {
		final int limit = array.length;
		int cnt = 0;
		for (int i = 0; i < limit; i++) {
			final String string = array[i];
			if (string != null && string.trim().length() > 0)
				cnt++;
		}
		return cnt;
	}

	/**
	 * returns the String with the first character capitalized so wisconsin =>
	 * Wisconsin
	 */
	public static final String capitalize(final String text) {
		final char upper = Character.toUpperCase(text.charAt(0));
		return upper + text.substring(1);
	}

	/** finds the longest string and returns the number of characters */
	public static final int getMaxLineCount(final String text) {
		int longest = -1;
		final char NL = '\n';
		// s = start e = end
		for (int s = 0, e = text.indexOf(NL); e >= 0; s = e, e = text.indexOf(
				NL, ++e)) {
			final int diff = e - s;
			if (e >= 0 && longest < diff)
				longest = diff;
		}
		if (longest == -1)
			return text.length();
		return longest;
	}
}