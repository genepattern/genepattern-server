/*
 * WHITEHEAD INSTITUTE
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2001 by the
 * Whitehead Institute for Biomedical Research.  All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever.  The Whitehead Institute can not be responsible for its
 * use, misuse, or functionality.
 */

package org.genepattern.data;

import java.io.Serializable;
import java.util.Arrays;

import org.genepattern.data.DataModel;
import org.genepattern.util.XMath;

/**
 * Lots of code and ideas copied form GVector.
 * 
 * vector -> encapsulates an array of floats
 * 
 * FloatVector is very very mutable!
 * 
 * why not use GVectpor dfirecly -> uses double while we can do with float. has
 * some inapp methods.
 *  * A float precision, general, and dynamically resizeable one dimensional
 * vector class. Index numbering begins with zero.
 * 
 * Notes: - before adding methods might want to check if already impl in GVector -
 * note that the constructors do a sysarray copy for safety
 * 
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class FloatVector implements org.genepattern.data.DataObjector,
		Serializable, FloatsSource {

	/**
	 * Constructs a new generalized mathematic Vector with zero elements, length
	 * reprents the number of elements in the vector.
	 * 
	 * @param length
	 *            number of elements in this vector.
	 */
	public FloatVector(final int length) {
		elementCount = length;
		elementData = new float[length]; // will be initialized to 0.0
	}

	/**
	 * Constructs a new generalized mathematic Vector with zero elements, length
	 * reprents the number of elements in the vector. This comment is a bug in
	 * Sun's API !!
	 * 
	 * @param vector
	 *            the values for the new vector.
	 */
	public FloatVector(float[] vector) {
		this(vector.length);
		System.arraycopy(vector, 0, elementData, 0, elementCount);
	}

	/**
	 * Constructs a new Vector and copies the initial values from the parameter
	 * vector.
	 * 
	 * @param vector
	 *            the source for the new Vector's initial values
	 */
	public FloatVector(FloatVector vector) {
		this(vector.elementCount);
		System.arraycopy(vector.elementData, 0, elementData, 0, elementCount);
	}

	/**
	 * Constructs a new Vector by copying length elements from the array
	 * parameter. The parameter length must be less than or equal to
	 * vector.length.
	 * 
	 * @param vector
	 *            The array from which the values will be copied.
	 * @param length
	 *            The number of values copied from the array.
	 */
	public FloatVector(float[] vector, int length) {
		// ArrayIndexOutOfBounds occur if length > vector.legnth
		this(length);
		System.arraycopy(vector, 0, elementData, 0, elementCount);
	}

	public FloatVector(FloatVector vectorA, FloatVector vectorB) {
		this(new FloatVector[] { vectorA, vectorB });
	}

	public FloatVector(FloatVector[] vectors) {
		int length = 0;
		for (int i = 0; i < vectors.length; i++) {
			length += vectors[i].getSize();
		}

		this.elementCount = length;
		this.elementData = new float[length]; // will be initialized to 0.0
		int pos = 0;
		for (int i = 0; i < vectors.length; i++) {
			System.arraycopy(vectors[i].elementData, 0, elementData, pos,
					vectors[i].elementCount);
			pos += vectors[i].elementCount;
		}
	}

	/**
	 * @return sum of all elements in the Vector
	 */
	public float sum() {
		float sum = 0;
		final int cnt = elementCount;
		for (int i = 0; i < cnt; i++) {
			sum += elementData[i];
		}

		return sum;
	}

	// trouble when element is negative
	public float sqrtsum() {
		float sqrtsum = 0;
		for (int i = 0; i < elementCount; i++) {
			sqrtsum += Math.sqrt(elementData[i]);
		}

		return sqrtsum;
	}

	public float squaresum() {
		float squaresum = 0;
		for (int i = 0; i < elementCount; i++) {
			squaresum += elementData[i] * elementData[i];
		}

		return squaresum;
	}

	/**
	 * @return arithmetic mean
	 */
	public float mean() {
		computeset.mean = sum() / elementCount;
		return computeset.mean;
	}

	/**
	 * 
	 * XXX check impl -its a little diff from mangelos
	 */
	public float median() {

		float[] v1 = new float[elementCount];
		System.arraycopy(elementData, 0, v1, 0, elementCount);
		Arrays.sort(v1);
		float median;
		if (XMath.isEven(elementCount))
			median = elementData[elementCount / 2];
		else
			median = elementData[(elementCount - 1) / 2];

		computeset.median = median;
		return median;

		/*
		 * float[] v1 = new float[aLen]; System.arraycopy(aVec, 0, v1, 0, aLen);
		 * Arrays.sort(v1); int ind = (aLen - 1) / 2; if (CMath.isEven(aLen))
		 * return (v1[ind] + v1[aLen / 2]) / 2; else return v1[ind];
		 *  
		 */
	}

	/**
	 * XXX needs review from pt
	 */
	public float sig(double level) {
		float[] v1 = new float[elementCount];
		System.arraycopy(elementData, 0, v1, 0, elementCount);
		Arrays.sort(v1);
		int sigindx = (int) (level * elementCount);
		return elementData[sigindx];
	}

	/**
	 * Calculate the unbiased variance. 
	 * 
	 * var = sum(x_i-mean)**2 / N
	 *  {*
	 * @see <a href=\'http://davidmlane.com/hyperstat/A16252.html\'> this</a> unbiased variance ->
	 *       n-1 biased variance using n Most commonly used is unbiased.
	 * 
	 * @return the variance of the vector unbiased (div by n-1) or biased(divide
	 *         by n)
	 */
	public float var(boolean biased) {

		float oldvar = (float) 0.0;
		final int cnt = elementCount;
		final int len = (!biased) ? cnt - 1 : cnt;
		//if (! biased) len--;

		// Variance of 1 point is 0 (we are returning the biased variance in
		// this case)
		if (len <= 0)
			return oldvar;

		final float mean = mean();

		for (int i = 0; i < cnt; i++) {
			float tmp = elementData[i] - mean;
			oldvar += tmp * tmp;
		}

		return oldvar / len;
	}

	/**
	 * @return The std dev of vector
	 */
	public double stddev(boolean biased) {
		computeset.stddev = Math.sqrt(var(biased));
		return computeset.stddev;
	}

	/**
	 * defined as stddev / mean
	 * 
	 * @see <a href="http://www.dchip.org/">DChip</a>
	 */
	public double vard(boolean biased) {
		computeset.variation = this.stddev(biased) / this.mean();
		return computeset.variation;
	}

	/**
	 * @return the highest value element of this Vector
	 */
	public float max() {
		float max = Float.MIN_VALUE;
		for (int i = 0; i < elementCount; i++) {
			if (elementData[i] > max)
				max = elementData[i];
		}
		computeset.max = max;
		return max;
	}

	/**
	 * @return the highest value element of this Vector
	 */
	public float min() {
		float min = Float.MAX_VALUE;
		for (int i = 0; i < elementCount; i++) {
			if (elementData[i] < min)
				min = elementData[i];
		}
		computeset.min = min;
		return min;
	}

	/**
	 * Returns the square root of the sum of the squares of this vector (its
	 * length in n-dimensional space).
	 * 
	 * @return length of this vector
	 */
	public final double norm() {
		return Math.sqrt(normSquared());
	}

	/**
	 * Returns the sum of the squares of this vector (its length sqaured in
	 * n-dimensional space).
	 * <p>
	 * 
	 * @return length squared of this vector
	 */
	public final double normSquared() {
		double s = 0.0;
		for (int i = 0; i < elementCount; i++) {
			s += elementData[i] * elementData[i];
		}
		return s;
	}

	/**
	 * Sets the value of this vector to the normalization of vector v1.
	 * 
	 * @param v1
	 *            the un-normalized vector
	 */
	public final void normalize(FloatVector v1) {
		set(v1);
		normalize();
	}

	/**
	 * Normalizes this vector in place.
	 */
	public final void normalize() {
		double len = norm();
		// zero-div may happen.
		for (int i = 0; i < elementCount; i++)
			elementData[i] /= len;
	}

	/**
	 * These set the unbiased variance of the vector to var. (They do not set
	 * the sigma to var) E.g. to change the variance from oldVar to newVar,
	 * multiply each component of the vector by sqrt(newVar / oldVar).
	 */
	public void setRowNormalization(float mean, float var) {
		final float oldVar = var(false); // unbiased or biased ???
		if (oldVar == 0)
			throw new IllegalStateException(
					"Cannot divide by a variance of zero!");
		final float oldMean = mean();
		final float mean_minus_oldmean = (mean - oldMean);
		final float theScale = (float) Math.sqrt(var / oldVar);

		final int cnt = this.elementCount;
		for (int i = 0; i < cnt; ++i)
			elementData[i] = theScale * (elementData[i] + mean_minus_oldmean);

	}

	/**
	 * Sets the value of this vector to the scalar multiplication of the scale
	 * factor with the vector v1.
	 * 
	 * @param s
	 *            the scalar value
	 * @param v1
	 *            the source vector
	 */
	public final void scale(float s, FloatVector v1) {
		set(v1);
		scale(s);
	}

	/**
	 * Scales this vector by the scale factor s.
	 * 
	 * @param s
	 *            the scalar value
	 */
	public final void scale(float s) {
		final int cnt = elementCount;
		for (int i = 0; i < cnt; i++)
			elementData[i] *= s;
	}

	public final void divideBy(float value) {
		divide(elementData, value);
	}

	/** divides all elements in the array by the value */
	public static final void divide(final float[] array, final float value) {
		final int limit = array.length;
		for (int i = 0; i < limit; i++) {
			array[i] /= value;
		}
	}

	public static final void divide(final double[] array, final double value) {
		final int limit = array.length;
		for (int i = 0; i < limit; i++) {
			array[i] /= value;
		}
	}

	/**
	 * Sets the value of this vector to the scalar multiplication by s of vector
	 * v1 plus vector v2 (this = s*v1 + v2).
	 * 
	 * @param s
	 *            the scalar value
	 * @param v1
	 *            the vector to be multiplied
	 * @param v2
	 *            the vector to be added
	 */
	public final void scaleSum(float s, FloatVector v1, FloatVector v2) {
		float[] v1data = v1.elementData;
		float[] v2data = v2.elementData;

		if (elementCount != v1.elementCount)
			throw new ArrayIndexOutOfBoundsException("this.size:"
					+ elementCount + " != v1's size:" + v1.elementCount);
		if (elementCount != v2.elementCount)
			throw new ArrayIndexOutOfBoundsException("this.size:"
					+ elementCount + " != v2's size:" + v2.elementCount);

		for (int i = 0; i < elementCount; i++) {
			elementData[i] = s * v1data[i] + v2data[i];
		}
	}

	// simple math
	/**
	 * Sets the value of this vector to sum of itself and the specified vector
	 * 
	 * @param vector
	 *            the second vector
	 */
	public final void sum(FloatVector vector) {
		sum(vector.elementData);
		//        float [] v1data = vector.elementData;
		//
		//        if (elementCount != vector.elementCount)
		//            throw new ArrayIndexOutOfBoundsException("this.size:"+elementCount+"
		// != v2's size:"+vector.elementCount);
		//
		//        for (int i = 0; i < elementCount; i++) {
		//            elementData[i] += v1data[i];
		//        }
	}

	public final void sum(final float[] v1data) {
		if (elementCount != v1data.length)
			throw new ArrayIndexOutOfBoundsException("this.size:"
					+ elementCount + " != v2's size:" + v1data.length);

		for (int i = 0; i < elementCount; i++) {
			elementData[i] += v1data[i];
		}
	}

	/**
	 * Sets the value of this vector to the vector sum of vectors vector1 and
	 * vector2.
	 * 
	 * @param vector1
	 *            the first vector
	 * @param vector2
	 *            the second vector
	 */
	public final void sum(FloatVector vector1, FloatVector vector2) {
		set(vector1);
		sum(vector2);
	}

	/**
	 * Sets the value of this vector to the vector difference of itself and
	 * vector (this = this - vector).
	 * 
	 * @param vector -
	 *            the other vector
	 */
	public final void sub(FloatVector vector) {
		float[] v1data = vector.elementData;
		if (elementCount != vector.elementCount)
			throw new ArrayIndexOutOfBoundsException("this.size:"
					+ elementCount + " != vector's size:" + vector.elementCount);

		for (int i = 0; i < elementCount; i++) {
			elementData[i] -= v1data[i];
		}
	}

	/**
	 * Sets the value of this vector to the vector difference of vectors vector1
	 * and vector2 (this = vector1 - vector2).
	 * 
	 * @param vector1
	 *            the first vector
	 * @param vector2
	 *            the second vector
	 */
	public final void sub(FloatVector vector1, FloatVector vector2) {
		set(vector1);
		sub(vector2);
	}

	// util methods
	/**
	 * Negates the value of this vector: this = -this.
	 */
	public final void negate() {
		for (int i = 0; i < elementCount; i++)
			elementData[i] = -elementData[i];
	}

	/**
	 * Sets all the values in this vector to zero.
	 */
	public final void zero() {
		// seZero may be more consistent name.
		Arrays.fill(elementData, 0.0f);
		//        for (int i = 0; i < elementCount; i++)
		//            elementData[i] = 0;
	}

	/**
	 * Changes the size of this vector dynamically. If the size is increased no
	 * data values will be lost. If the size is decreased, only those data
	 * values whose vector positions were eliminated will be lost.
	 * 
	 * @param newSize
	 *            number of desired elements in this vector
	 */
	public final void setSize(int newSize) {
		if (newSize < 0)
			throw new NegativeArraySizeException("newSize:" + newSize + " < 0");

		if (elementCount < newSize) {
			float[] oldData = elementData;
			elementData = new float[newSize];
			System.arraycopy(oldData, 0, elementData, 0, elementCount);
		}

		elementCount = newSize;
	}

	/**
	 * Sets the value of this vector to the values found in the array parameter.
	 * The array should be at least equal in length to the number of elements in
	 * the vector.
	 * 
	 * @param vector
	 *            the source array
	 */
	public final void set(double vector[]) {
		// note: only this.elementCount data is copied.(no auto-grow)
		System.arraycopy(vector, 0, elementData, 0, elementCount);
	}

	/**
	 * Sets the value of this vector to the values found in vector vector.
	 * 
	 * @param vector
	 *            the source vector
	 */
	public final void set(FloatVector vector) {
		// note: only this.elementCount data is copied.(no auto-grow)
		System.arraycopy(vector.elementData, 0, elementData, 0, elementCount);
	}

	/**
	 * Returns the number of elements in this vector.
	 * 
	 * @return number of elements in this vector
	 */
	public final int getSize() {
		return elementCount;
	}

	/**
	 * Retrieves the value at the specified index value of this vector.
	 * 
	 * @param index
	 *            the index of the element to retrieve (zero indexed)
	 * @return the value at the indexed element
	 */
	public final float getElement(int index) {
		try {
			return elementData[index];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("index:" + index
					+ "must be in [0, " + (elementCount - 1) + "]");
		}
	}

	/**
	 * Modifies the value at the specified index of this vector.
	 * 
	 * @param index
	 *            the index if the element to modify (zero indexed)
	 * @param value
	 *            the new vector element value
	 */
	public final void setElement(int index, float value) {
		try {
			elementData[index] = value;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("index:" + index
					+ " must be in [0, " + (elementCount - 1) + "]");
		}
	}

	/**
	 * Returns a string that contains the values of this Vector.
	 * 
	 * @return the String representation
	 */
	public String toString() {
		return "(" + toString(',') + ")";
		//        final StringBuffer buf = new StringBuffer();
		//        buf.append("(");
		//        final int cnt = elementCount - 1;
		//        for (int i = 0 ; i < cnt; i++) {
		//            buf.append(elementData[i]);
		//            buf.append(",");
		//        }
		//        buf.append(elementData[cnt]);
		//        buf.append(")");
		//        return buf.toString();
	}

	public String toString(final char delim) {
		StringBuffer buf = new StringBuffer();
		final int cnt = elementCount - 1;
		for (int i = 0; i < cnt; i++) {
			buf.append(elementData[i]);
			buf.append(delim);
		}
		buf.append(elementData[cnt]);
		return buf.toString();
	}

	public java.util.Vector toUtilVector() {
		java.util.Vector v = new java.util.Vector(getSize());
		for (int i = 0; i < elementCount; i++) {
			v.add(new Float(elementData[i]));
		}
		return v;
	}

	/**
	 * returns a safe copy
	 */
	public float[] toArray() {
		float[] fl = new float[elementData.length];
		System.arraycopy(elementData, 0, fl, 0, elementData.length);
		return fl;
	}

	/**
	 * returns a DataModel that defines the type of model this implementation
	 * represents
	 */
	public org.genepattern.data.DataModel getDataModel() {
		return DATA_MODEL;
	}

	/**
	 * Returns true if all of the data members of Vector vector1 are equal to
	 * the corresponding data members in this Vector.
	 * 
	 * @param vector1
	 *            The vector with which the comparison is made.
	 * @return true or false
	 */
	public boolean equals(FloatVector vector1) {
		if (vector1 == this)
			return true;
		if (vector1 == null || elementCount != vector1.elementCount)
			return false;
		final float[] v1data = vector1.elementData;
		for (int i = 0; i < elementCount; i++) {
			if (elementData[i] != v1data[i])
				return false;
		}
		return true;
	}

	/**
	 * Returns true if the Object o1 is of type Vector and all of the data
	 * members of t1 are equal to the corresponding data members in this Vector.
	 * 
	 * @param o1
	 *            the object with which the comparison is made.
	 */
	public boolean equals(Object o1) {
		return (o1 instanceof FloatVector) && equals((FloatVector) o1);
	}

	/**
	 * Returns true if the L-infinite distance between this vector and vector v1
	 * is less than or equal to the epsilon parameter, otherwise returns false.
	 * The L-infinite distance is equal to MAX[abs(x1-x2), abs(y1-y2), . . . ].
	 * <p>
	 * 
	 * @param v1
	 *            The vector to be compared to this vector
	 * @param epsilon
	 *            the threshold value
	 */
	public boolean epsilonEquals(FloatVector v1, double epsilon) {
		if (elementCount != v1.elementCount)
			return false;
		float[] v1data = v1.elementData;
		for (int i = 0; i < elementCount; i++) {
			if (Math.abs(elementData[i] - v1data[i]) > epsilon)
				return false;
		}
		return true;
	}

	// scalers ??
	/**
	 * Returns the dot product of this vector and vector v1.
	 * 
	 * @param v1
	 *            the other vector
	 * @return the dot product of this and v1
	 */
	public final double dot(FloatVector v1) {
		float[] v1data = v1.elementData;
		if (elementCount != v1.elementCount)
			throw new IllegalArgumentException("this.size:" + elementCount
					+ " != v1.size:" + v1.elementCount);
		double sum = 0.0;
		for (int i = 0; i < elementCount; ++i)
			sum += elementData[i] * v1data[i];
		return sum;
	}

	/**
	 * Returns the (n-space) angle in radians between this vector and the vector
	 * parameter; the return value is constrained to the range [0,PI].
	 * 
	 * @param v1
	 *            The other vector
	 * @return The angle in radians in the range [0,PI]
	 */
	public final double angle(FloatVector v1) {
		return Math.acos(dot(v1) / norm() / v1.norm());
	}

	/**
	 * Linearly interpolates between vectors v1 and v2 and places the result
	 * into this tuple: this = (1-alpha)*v1 + alpha*v2.
	 * 
	 * @deprecated the double version of this method should be used.
	 * @param v1
	 *            the first vector
	 * @param v2
	 *            the second vector
	 * @param alpha
	 *            the alpha interpolation parameter
	 */
	public final void interpolate(FloatVector v1, FloatVector v2, float alpha) {
		interpolate(v1, v2, alpha);
	}

	/**
	 * Linearly interpolates between this vector and vector v1 and places the
	 * result into this tuple: this = (1-alpha)*this + alpha*v1.
	 * 
	 * @param v1
	 *            the first vector
	 * @param alpha
	 *            the alpha interpolation parameter
	 */
	public final void interpolate(FloatVector v1, float alpha) {
		float[] v1data = v1.elementData;
		if (elementCount != v1.elementCount)
			throw new IllegalArgumentException("this.size:" + elementCount
					+ " != v1.size:" + v1.elementCount);

		float beta = (float) (1.0 - alpha);
		for (int i = 0; i < elementCount; ++i) {
			elementData[i] = beta * elementData[i] + alpha * v1data[i];
		}
	}

	/**
	 * biinning
	 */
	// notice that any element not in one of the ranges is simply not binned
	// so totasl number of elekments in all bins not necc == size of vector
	public void bin(Range[] ranges) {
		for (int i = 0; i < elementCount; i++) {
			for (int j = 0; j < ranges.length; j++) {
				if (ranges[j].isMember(elementData[i]))
					ranges[j].increment();
			}
		}
	}

	public void bin(java.util.List ranges) {
		for (int i = 0; i < elementCount; i++) {
			for (int j = 0; j < ranges.size(); j++) {
				Range range = (Range) ranges.get(j);
				if (range.isMember(elementData[i]))
					range.increment();
			}
		}
	}

	/**
	 * PNormalizes this vector in place. 
	 * 
	 * Min set to -1 and max set to +1
	 * 
	 * size of vect doeesnt matter:
	 * 
	 * 
	 * 
	 * XXX what is the correct terminology for this and how does it relate to
	 *       normalize?
	 */
	public void pnormalize() {
		float pmin = -1;
		float pmax = +1;
		float min = this.min();
		float max = this.max();

		for (int i = 0; i < elementCount; i++) {
			elementData[i] = pmin + ((elementData[i] - min) / (max - min)) * 2;
		}

	}

	public static String getSummaryStatistics(FloatVector[] vectors) {
		StringBuffer buf = new StringBuffer("Number of Vectors : ").append(
				vectors.length).append('\n');

		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;

		for (int i = 0; i < vectors.length; i++) {
			if (min > vectors[i].min())
				min = vectors[i].computeset.min;
			if (max < vectors[i].max())
				max = vectors[i].computeset.max;

		}

		buf.append("Absolute min: ").append(min).append('\n');
		buf.append("Absolute max: ").append(max).append('\n');

		return buf.toString();
	}

	/**
	 * returns false if this is an object that cannot have it's internal state
	 * changed
	 */
	public final boolean isMutable() {
		return true;
	}

	/**
	 * This is a reminer that classes that override equals must also create a
	 * working hash algorithm. 
	 * 
	 * for example:
	 * 
	 * given: boolean b compute (b ? 0 : 1) byte, char, short, or int i compute
	 * (int)i long l compute (int)(l ^ (l >>> 32)) float f compute
	 * Float.floatToIntBits(f) double d compute Double.doubleToLongBits(d) then
	 * compute as long
	 * 
	 * Object just get it's hash or if null then 0
	 * 
	 * Arrays compute for each element
	 * 
	 * i.e.: int result = 17; // prime number result = 37 * result +
	 * (int)character; result = 37 * result + Float.floatToIntBits(f); etc..
	 * return result;
	 */
	public int hashCode() {
		if (hashcode == 0 || isMutable()) {
			int result = 17; // some prime #
			final int cnt = this.getSize();
			for (int i = 0; i < cnt; i++) {
				result = 37 * result + Float.floatToIntBits(elementData[i]);
			}
			if (isMutable()) // for mutable classes recalculate every time
				return result;
			else
				hashcode = result;
		}
		return hashcode;
	}

	/** returns the name of this data object */
	public String getName() {
		return "FloatVector " + elementCount;
	}

	// fields

	private static final long serialVersionUID = 3728218889911142280L;

	// fields
	/** the DataModel that should be returned from getDataModel() */
	public static final DataModel DATA_MODEL = new DataModel("FloatVector", 3);

	// note: only elementCount data in elementData are valid.
	// elementData.length is the allocated size.
	// invariant: elementData.length >= elementCount.

	// NOTE: AS making these protected for package friendly use (by Matrix etc)

	protected int elementCount;

	protected float elementData[];

	public transient ComputeSet computeset = new ComputeSet();

	/** the hash code value or 0 if not computed */
	private int hashcode = 0;

	// I N N E R C L A S S E S

	/**
	 * These are set each time a mean, media etc call is made. If the vector
	 * changes, they do NOT auto change! Only change after the compute call is
	 * made again. see metrics and the nnalg for whys this is a useful concept.
	 *  
	 */
	public class ComputeSet implements Serializable {

		private static final long serialVersionUID = 998218889911142280L;

		public float mean = Float.NaN;

		public float median = Float.NaN;

		public double stddev = Float.NaN;

		public float max = Float.NaN;

		public float min = Float.NaN;

		// NOT the same as var --
		public double variation = Float.NaN;

		public void recompute() {
			mean();
			median();
			stddev(false); // default
			max();
			min();
			vard(false); // default
		}

	} // End ComputeSet

} // End Vector

