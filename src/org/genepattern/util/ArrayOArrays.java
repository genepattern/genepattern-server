/*
 * ArrayOArrays.java
 *
 * Created on January 17, 2003, 9:15 PM
 */

package org.genepattern.util;

/**
 * This class encapsulates an array of arrays all of equal length but perhaps
 * each of a different Class. It enforces that all arrays be populated
 * individully and row-wise in sequence.
 * 
 * @author kohm
 */
public class ArrayOArrays {

	/** Creates a new instance of ArrayOArrays */
	public ArrayOArrays(final int depth, final Class[] classes) {
		this.depth = depth;
		final int cnt = classes.length;
		this.array_count = cnt;
		arrays = new Object[cnt];
		// construct arrays of length 'depth'
		for (int i = 0; i < cnt; i++) {
			final Class clss = classes[i];
			final Object array = java.lang.reflect.Array.newInstance(clss,
					depth);
			arrays[i] = array; // store the new array
		}
		this.types = (Class[]) classes.clone();
	}

	// normal getters
	/** returns the array count */
	public int getArrayCount() {
		return array_count;
	}

	/** returns the depth or length of the arrays */
	public int getDepth() {
		return depth;
	}

	/**
	 * gets the specified array checks the status first and if not ready throws
	 * an Exception
	 */
	public Object getArray(final int i) {
		checkStatus();
		return arrays[i];
	}

	/** determines if the arrays have all been filled and this is complete */
	public boolean isComplete() {
		return (current_column == DONE && current_row == DONE);
	}

	// the setters must be called in the proper order
	// and not called after the arrays are full
	/** general setter - tries to convert the String to the type for its' column */
	public void set(final String token) {
		final Class type = this.types[current_column];
		if (type.equals(String.class)) {
			setString(token);
		} else if (type.equals(Integer.TYPE)) {
			setInt(Integer.parseInt(token));
		} else if (type.equals(Float.TYPE)) {
			setFloat(Float.parseFloat(token));
		} else if (type.equals(Boolean.TYPE)) {
			setBoolean(token);
		} else if (type.equals(Character.TYPE)) {
			if (token.length() == 1)
				setChar(token.charAt(0));
			else
				throw new IllegalArgumentException(
						"Cannot convert a multi character String"
								+ " to a single character!");
		} else if (type.equals(Byte.TYPE)) {
			setByte(Byte.parseByte(token));
		} else if (type.equals(Short.TYPE)) {
			setShort(Short.parseShort(token));
		} else if (type.equals(Long.TYPE)) {
			setLong(Long.parseLong(token));
		} else if (type.equals(Double.TYPE)) {
			setDouble(Double.parseDouble(token));
		}
	}

	/** sets the byte throws a ClassCastException if shouldn't be setting an byte */
	public void setByte(final byte value) {
		((byte[]) arrays[current_column])[current_row] = value;
		advance();
	}

	/**
	 * sets the short throws a ClassCastException if shouldn't be setting an
	 * short
	 */
	public void setShort(final short value) {
		((short[]) arrays[current_column])[current_row] = value;
		advance();
	}

	/** sets the int throws a ClassCastException if shouldn't be setting an int */
	public void setInt(final int integer) {
		((int[]) arrays[current_column])[current_row] = integer;
		// same as:
		//final int[] ints = (int[])arrays[current_column];
		//ints[current_row] = i;
		advance();
	}

	/** sets the long throws a ClassCastException if shouldn't be setting an long */
	public void setLong(final long value) {
		((long[]) arrays[current_column])[current_row] = value;
		advance();
	}

	/**
	 * sets the float throws a ClassCastException if shouldn't be setting a
	 * float
	 */
	public void setFloat(final float fl) {
		((float[]) arrays[current_column])[current_row] = fl;
		advance();
	}

	/**
	 * sets the double throws a ClassCastException if shouldn't be setting a
	 * double
	 */
	public void setDouble(final double value) {
		((double[]) arrays[current_column])[current_row] = value;
		advance();
	}

	/**
	 * sets the boolean from the String representation of a boolean throws a
	 * ClassCastException if shouldn't be setting a boolean
	 */
	public void setBoolean(final String text) {
		boolean value = false;
		final String string = text.toLowerCase();
		if (string.equals("true") || string.equals("t") || string.equals("yes"))
			value = true;
		else if (string.equals("false") || string.equals("f")
				|| string.equals("no"))
			value = false;
		else
			throw new IllegalArgumentException(
					"Not a valid string representation (" + text
							+ ") of a boolean value!");
		setBoolean(value);
	}

	/**
	 * sets the boolean throws a ClassCastException if shouldn't be setting a
	 * boolean
	 */
	public void setBoolean(final boolean value) {
		((boolean[]) arrays[current_column])[current_row] = value;
		advance();
	}

	/**
	 * sets the character throws a ClassCastException if shouldn't be setting a
	 * char
	 */
	public void setChar(final char value) {
		((char[]) arrays[current_column])[current_row] = value;
		advance();
	}

	/**
	 * sets the String throws a ClassCastException if shouldn't be setting a
	 * String
	 */
	public void setString(final String text) {
		((String[]) arrays[current_column])[current_row] = text.trim();
		advance();
	}

	//    /** sets the int throws a ClassCastException if shouldn't be setting an
	// int*/
	//    public void setObject(final Object obj) { // FIXME need reflection here
	//        Object arr = arrays[current_column];
	//        Class array_class = arr.getClass();
	//        ((array_class)arr)[current_row] = (types[current_column])obj;
	//    }
	// helper methods
	/** advances the current_row or current_column counts */
	private void advance() {
		current_column++;
		if (current_column == array_count) {//if EOR goto next row and reset
											// column
			current_column = 0;
			current_row++;
			if (current_row == depth) {//if no more rows then done
				current_row = current_column = DONE;
				// System.out.println("ArrayOArrays is full");
			}
		}
	}

	/** throws an Exception if the arrays are not yey full */
	private void checkStatus() {
		if (!isComplete()) {
			throw new IllegalStateException("Arrays are not yet full: rows "
					+ current_row + " of " + depth + "; columns "
					+ current_column + " of " + array_count);
		}
	}

	// fields
	/**
	 * the value that the current_column and current_row are set to when the
	 * arrays are filled
	 */
	private static final int DONE = Integer.MAX_VALUE;

	/** the depth of each array */
	private final int depth;

	/** the current number of complete rows added or Integer.MAX_VALUE if done */
	private int current_row = 0;

	/** the number of arrays */
	private final int array_count;

	/** the current column to add or Integer.MAX_VALUE if done */
	private int current_column = 0;

	/** the array of the arrays */
	private final Object[] arrays;

	/** the classes for the individual elements of the specified array */
	private final Class[] types;
}