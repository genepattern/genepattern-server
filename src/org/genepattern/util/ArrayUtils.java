package org.genepattern.util;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TFloatArrayList;
import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.genepattern.data.Dir;
import org.genepattern.math.XMath;

/**
 * Native array related utilities.
 * Functionally extends java.util.Arrays.
 *
 * @author Aravind Subramanian
 * @version %I%, %G%
 */

public class ArrayUtils {

    private static final XLogger log = XLogger.getLogger(ArrayUtils.class);
    /** private to prevent instantiation */
    private ArrayUtils() { }

    /** adds all the objects in the array to the list and returns the same list */
    public static final Collection addToCollection(final Object[] objs, final Collection list) {

        final int cnt = objs.length;
        for(int i = 0; i < cnt; i++) {
            list.add(objs[i]);
        }
        return list;
    }
    /** helper method that converts a List to a String array */
    public static final String[] listToStringArray(final List list) {
        final int num = list.size();
        return (String[])list.toArray(new String[num]);
    }
    /** helper method that converts a String array into a list (ArrayList) */
    public static final List stringArrayToList(final String[] strings) {
        final int num = strings.length;
        final List list = new ArrayList(num);
        for (int i = 0; i < num; i++) {
            list.add(strings[i]);
        }
        return list;
    }
    /** finds the index of a string in the array */
    public static final int getIndex(final String name, final String[] strings) {
        final int num = strings.length;
        for(int i = 0; i < num; i++) {
            if(strings[i].equals(name))
                return i;
        }
        return -1;
    }
    /** finds the index of first matching value in the array */
    public final int findIndexIn(final int[] indices, final int j) {
        if(j < 0 || j == indices[j])
            return j;
        final int limit = indices.length;
        for (int i = 0; i < limit; i++) {
            if(j == indices[i])
                return i;
        }
        return -1;
    }
    /**
     * Returns the nth value in the array as if it were sorted.
     *
     * @param n the index into the array if it where sorted
     * @param x the array
     */
    public static final float getNthSortedValue(final int n, final float[] x) {
        return getNthSortedValue(n, x, 0, x.length - 1);
    }
    /**
     * Returns the nth value in the array as if it were sorted.
     * NOTE: really need a partial sort for this, but..
     *   Also, order statistics are linear in N
     * n = 0 corresponds to the min..
     *
     * @param n the index into the array if it where sorted
     * @param x the array
     * @param start the starting index (inclusive)
     * @param end the ending index (inclusive)
     */
    public static final float getNthSortedValue(final int n, final float[] x, final int start, final int end ) {
        float result = Float.NaN;
        if(start > end)
            throw new IllegalArgumentException("Start ("+start+") cannot be after end ("+end+")!");
        try {
            final int cnt = end - start + 1;
            if( n >= cnt || n < 0 )
                throw new ArrayIndexOutOfBoundsException("n="+n+" must be 0 <= n < "+cnt+" !");
            final float[] tmp = new float[cnt];
            float max = Float.NEGATIVE_INFINITY;
            int j = 0;
            System.arraycopy(x, start, tmp, 0, cnt);
            Arrays.sort(tmp);
            result = tmp[n];
        } finally {
            if(result == Float.NaN) {
                System.err.println("ArrayUtils.getNthSortedValue(): n = "
                + n+" start="+start+" end="+end);
            }
        }
        
        return result;
    }
    /**
     * determines if the array is empty
     * empty Stirng arrays are either null, zero length, or each element in
     * the array is either null, or the empty String
     */
    public static final boolean arrayIsEmpty(final String[] array) {
        if(array == null)
            return true;
        
        for(int i = 0, limit = array.length; i < limit; i++ ) {
            final String val = array[i];
            if(val != null && !"".equals(val))//also checking if it is empty string
                return false;
        }
        return true;
    }
    /**
     * determines if the array is empty
     * empty Object arrays are either null, zero length, or each element in
     * the array is null
     * Note this is not quite the same as arrayIsEmpty(String[])
     */
    public static final boolean arrayIsEmpty(final Object[] array) {
        if(array == null)
            return true;
        for(int i = 0, limit = array.length; i < limit; i++ ) {
            if(array[i] != null)
                return false;
        }
        return true;
    }
    /**
     * Determines if the List is empty
     * Empty Lists are either null, zero length, or each element in
     * the array is null
     * @see ArrayUtils#arrayIsEmpty(Object[])
     */
    public static final boolean listIsEmpty(final List list) {
        if(list == null)
            return true;
        for(int i = list.size() - 1; i >= 0; i-- ) { // rev. loop
            if(list.get(i) != null)
                return false;
        }
        return true;
    }
    /** gets the min and max values in the array without sorting, modifying, or
     * copying the array*/
    public static final MinMax getMinMax(final int[] array) {
        final int cnt = array.length;
        int min = array[0], max = min;
        for(int i = 1; i < cnt; i++) {
            final int val = array[i];
            if(min > val)
                min = val;
            else if(val > max)
                max = val;
        }
        return new MinMax(min, max);
    }
    /** gets the min and max values in the array without sorting, modifying, or
     * copying the array*/
    public static final MinMaxFloat getMinMax(final float [] array) {
        final int cnt = array.length;
        float  min = array[0], max = min;
        for(int i = 1; i < cnt; i++) {
            final float val = array[i];
            if(min > val)
                min = val;
            else if(val > max)
                max = val;
        }
        return new MinMaxFloat(min, max);
    }
    /** returns true if all array elements are less than some value */
    public static final boolean arrayValuesLessThan(final int[] array, final int value) {
        final int limit = array.length;
        for (int i = 0; i < limit; i++) {
            if(array[i] >= value)
                return false;
        }
        return true;
    }
    /** returns the lesser of all the vlaues in the array */
    public static final float getMin(final float [] array) {
        final int cnt = array.length;
        float  min = array[0];
        for(int i = 1; i < cnt; i++) {
            final float val = array[i];
            if(min > val)
                min = val;
        }
        return min;
    }
    /** returns the lesser of all the vlaues in the array */
    public static final float getMax(final float [] array) {
        final int cnt = array.length;
        float  max = array[0];
        for(int i = 1; i < cnt; i++) {
            final float val = array[i];
            if(max < val)
                max = val;
        }
        return max;
    }
    /**
     * complement
     * feeds into ret array all indices from 0 to max -1
     * that are NOT in the specified exclude array.
     *
     * retr array will have length max - exclude.length
     *
     * all elements of exlude array must be >= 0 and <= max.
     */
    // @todo improve algorithm
    // think binary search cant be used as the arrays need to be sorted (see javadoc)
     public final static int[] complement(int[] exclude, int max) {

        List list = new ArrayList(max);
        for (int i=0; i < exclude.length; i++) {
            if (exclude[i] < 0) throw new IllegalArgumentException("Exclude array has negative element value: " + exclude[i] + " at: " + i);
            if (exclude[i] >= max) throw new IllegalArgumentException("Exclude array has element of value: " + exclude[i] + " that is  >= max: " + max + " at pos: " + i);
            list.add(new Integer(exclude[i]));
        }

        int[] invrows = new int[max - exclude.length];
        int cnt = 0;
        for (int i=0; i < max; i++) {
            if (list.contains(new Integer(i))) ;
            else invrows[cnt++] = i;
        }

        return invrows;
    }

    /**
     * creates a new integer array with elements as all consecutive integers between
     * min and max inclusive.
     * Length = max - min + 1
     */
    public final static int[] rangeAsElements(final int min, final int max) {
        if (max < min)
            throw new IllegalArgumentException("Max: " + max + " less than min: " + min);
        
        return rangeAsElements(new int[max - min + 1], min);
    }
    /**
     * Fills the integer array with elements as all consecutive integers between
     * start and start + array length -1.
     */
    public final static int[] rangeAsElements(final int[] ret, final int start) {
        final int limit = ret.length;

        for (int i=0, curr = start; i < limit; i++) {
            ret[i] = curr++;
        }

        return ret;
    }

    // INclusive
    public final static java.util.Vector rangeAsUtilVector(final int min, final int max) {
        if (max < min) throw new IllegalArgumentException("Max: " + max + " less than min: " + min);
        java.util.Vector vector = new java.util.Vector(max - min +1);

        int curr = min;
        for (int i=0; i < (max-min+1); i++) {
            vector.add(new Integer(curr++));
        }

        return vector;
    }

    public final static int[] complementRangeAsElements(final int compmin, final int compmax, final int max) {
        if (compmax < compmin) throw new IllegalArgumentException("Max: " + compmax + " less than min: " + compmin);
        int[] ret = new int[max - (compmax - compmin +1)];

        int cnt = 0;
        for (int i=0; i < max; i++) {
            if ( (i >= compmin) && (i <= compmax) ) ;
            else ret[cnt++] = i;
        }

        return ret;
    }
    /** returns true if obj is found in one of the array elements of the array,
     * false otherwise 
     * If the array is sorted use java.util.Arrays.binarySearch();
     */
    public static final boolean contains(final Object[] array, final Object obj) {
        final int limit = array.length;
        for(int i = 0; i < limit; i++) {
            if( obj.equals(array[i]) )
                return true;
        }
        return false;
    }

    /**
     * Specified array param is not modified in any way.
     * All duplicate (upto floats order of precision) elements are removed.
     *
     * @done improve algorithm
     *       <br>-> Moved to using gnu.trove
     */
    //
    // 377837 # of uniq elements: 376403
    // 377837 # of uniq elements: 30737
    //377837 # of uniq elements: 30737
    public final static float[] unique(final float[] values) {
        TFloatArrayList list = new TFloatArrayList();
        float[] copy = new float[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        Arrays.sort(copy);

        float curr = copy[0];
        list.add(curr);

        for (int i=1; i < values.length; i++) {
            if (copy[i] == curr) ;
            else {
                list.add(copy[i]);
                curr = copy[i];
            }
        }

        log.debug("# of non-uniq elements: " + values.length + " # of uniq elements: " + list.size());
        return list.toNativeArray();

    }
    /**
     * Specified array is not modified in any way.
     * Returns a sorted array of unique values from the specified array
     * All duplicate elements are removed.
     *
     * @done improve algorithm
     *       <br>-> Moved to using gnu.trove
     */
    public final static int[] unique(final int[] values) {
        final TIntArrayList list = new TIntArrayList();
        final int[] copy = (int[])values.clone();
        Arrays.sort(copy);

        int curr = copy[0];
        list.add(curr);
        final int len = values.length;
        for (int i=1; i < len; i++) {
            if (copy[i] != curr) {
                list.add(copy[i]);
                curr = copy[i];
            }
        }

        //log.debug("# of non-uniq elements: " + values.length + " # of uniq elements: " + list.size());
        System.out.println("# of non-uniq elements: " + values.length + " # of uniq elements: " + list.size());
        return list.toNativeArray();

    }
    /** creates an array of random ints with earlier elements tending to be larger */
    public static final int[] randomSubset(int aNum, final int aMaxVals, final java.util.Random rng) {
        if (aNum > aMaxVals)
          aNum = aMaxVals;
        final int [] tmpInds = new int[aMaxVals];
        for (int i = 0; i < aMaxVals; ++i)
          tmpInds[i] = i;
        final int [] inds = new int[aNum];
        final int limit = aMaxVals - aNum;
        for (int i = aMaxVals, j = 0; i > limit; i--, j++) {
            final int r = (int)(rng.nextFloat() * i);
            inds[j] = tmpInds[r];
            tmpInds[r] = tmpInds[i - 1];
        }
        return inds;
    }
    /**
     * part of the array in the dir
     * (note the indices which should always be 0 to n-1, but something that pout the indices in this order)
     *
     * Specified array param is not modified in any way.
     *
     * This methoid is range safe:
     * If num > data.length, it is truncated to data.length
     * If num is not even for the two tailed thing, it is made even.
     */
    public final static int[] sub(int num, Dir dir, int[] data) {
        if (num > data.length) num = data.length;

        if (dir == Dir.FORWARD) {
            int[] ret = new int[num];
            System.arraycopy(data, 0, ret, 0, num);
            return ret;
        }
        /* This will get from bot2top but form n-x, to n
        else if (dir == Dir.BOT2TOP) {
            int[] ret = new int[num];
            System.arraycopy(data, data.length-num, ret, 0, num);
            return ret;
        }
        */
        // This will get from reverse but from n to n-x
        else if (dir == Dir.REVERSE) {
            int[] ret = new int[num];
            int cnt = data.length-1;
            for (int i=0; i < num; i++) {
                ret[i] = data[cnt--];
            }
            return ret;
        }
        else if (dir == Dir.TWOTAILED) {
            if (! XMath.isEven(num)) num--; // make even, safely
            TIntArrayList list = new TIntArrayList(num);
            int[] top = new int[num/2];
            System.arraycopy(data, 0, top, 0, num/2);
            list.add(top);
            for (int i=data.length-num/2; i < data.length; i++) {
                list.add(data[i]);
            }

            return list.toNativeArray();
        }
        else throw new IllegalArgumentException("Unexpected dir: " + dir);
    }

    /**
     * specified array is untouched
     */
    public final static double[] sub(int num, Dir dir, double[] data) {
        if (num > data.length) num = data.length;

        if (dir == Dir.FORWARD) {
            double[] ret = new double[num];
            System.arraycopy(data, 0, ret, 0, num);
            return ret;
        }
        /* This will get from bot2top but form n-x, to n
        else if (dir == Dir.BOT2TOP) {
            int[] ret = new int[num];
            System.arraycopy(data, data.length-num, ret, 0, num);
            return ret;
        }
        */
        // This will get from bot2top but from n to n-x
        else if (dir == Dir.REVERSE) {
            double[] ret = new double[num];
            int cnt = data.length-1;
            for (int i=0; i < num; i++) {
                ret[i] = data[cnt--];
            }
            return ret;
        }
        else if (dir == Dir.TWOTAILED) {
            if (! XMath.isEven(num)) num--; // make even, safely
            TDoubleArrayList list = new TDoubleArrayList(num);

            double[] top = new double[num/2];
            System.arraycopy(data, 0, top, 0, num/2);
            list.add(top);
            for (int i=data.length-num/2; i < data.length; i++) {
                list.add(data[i]);
            }

            return list.toNativeArray();
        }
        else throw new IllegalArgumentException("Unexpected dir: " + dir);
    }

    /**
     * specified array is untouched
     */
    public final static float[] sub(int num, Dir dir, float[] data) {
        if (num > data.length) num = data.length;

        if (dir == Dir.FORWARD) {
            float[] ret = new float[num];
            System.arraycopy(data, 0, ret, 0, num);
            return ret;
        }
        /* This will get from bot2top but form n-x, to n
        else if (dir == Dir.BOT2TOP) {
            int[] ret = new int[num];
            System.arraycopy(data, data.length-num, ret, 0, num);
            return ret;
        }
        */
        // This will get from bot2top but from n to n-x
        else if (dir == Dir.REVERSE) {
            float[] ret = new float[num];
            int cnt = data.length-1;
            for (int i=0; i < num; i++) {
                ret[i] = data[cnt--];
            }
            return ret;
        }
        else if (dir == Dir.TWOTAILED) {
            if (! XMath.isEven(num)) num--; // make even, safely
            TFloatArrayList list = new TFloatArrayList(num);

            float[] top = new float[num/2];
            System.arraycopy(data, 0, top, 0, num/2);
            list.add(top);
            for (int i=data.length-num/2; i < data.length; i++) {
                list.add(data[i]);
            }

            return list.toNativeArray();
        }
        else throw new IllegalArgumentException("Unexpected dir: " + dir);
    }

    /**
     * Two tailed subarray with the first nfromtop elements of the array
     * first followed by the last nfrombottom elements.
     *
     * Any overlap that might exist is NOT gotten rid off
     * Hence length of returned array is always top + bottom
     */
    public final static int[] sub(final int nfromtop, final int nfrombottom, final int[] data) {
        int[] indices = new int[nfromtop + nfrombottom]; // any overlap that might exist is NOT gotten rid off

        // first include the top genes
        int cnt = 0;
        for (int i=0; i < nfromtop; i++) {
            indices[cnt++] = data[i];
        }

        // Then the from the bottom
        for (int i= data.length - nfrombottom; i < data.length; i++) {
            indices[cnt++] = data[i];
        }

        return indices;
    }
    //
    // F L O A T    S O R T I N G 
    //
    /**
     * Sorts the specified array of floats into ascending numerical order.
     * The sorting algorithm is a tuned quicksort, adapted from Jon
     * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
     * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
     * 1993).  This algorithm offers n*log(n) performance on many data sets
     * that cause other quicksorts to degrade to quadratic performance.
     *
     * @param a the array to be sorted.
     */

    public final static void indexSort(int[] aInds, float[] a) {
	    indexSort(aInds, a, 0, a.length);
    }

    /**
     * Sorts the specified range of the specified array of floats into
     * ascending numerical order.  The sorting algorithm is a tuned quicksort,
     * adapted from Jon L. Bentley and M. Douglas McIlroy's "Engineering a
     * Sort Function", Software-Practice and Experience, Vol. 23(11)
     * P. 1249-1265 (November 1993).  This algorithm offers n*log(n)
     * performance on many data sets that cause other quicksorts to degrade to
     * quadratic performance.
     *
     * @param a the array to be sorted.
     * @param fromIndex the index of the first element (inclusive) to be
     *        sorted.
     * @param toIndex the index of the last element (exclusive) to be sorted.
     * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
     * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
     *	       <tt>toIndex &gt; a.length</tt>
     */
    public final static void indexSort(final int[] aInds, final float x[], final int off, final int len) {
	// Insertion sort on smallest arrays
	if (len < 7) {
	    for (int i=off; i<len+off; i++)
		for (int j=i; j>off && x[aInds[j-1]]>x[aInds[j]]; j--)
		    swap(aInds, j, j-1);
	    return;
	}

	// Choose a partition element, v
	int m = off + len/2;       // Small arrays, middle element
	if (len > 7) {
	    int l = off;
	    int n = off + len - 1;
	    if (len > 40) {        // Big arrays, pseudomedian of 9
		    int s = len/8;
		    l = med3(x, aInds, l, l+s, l+2*s);
		    m = med3(x, aInds, m-s, m, m+s);
		    n = med3(x, aInds, n-2*s, n-s, n);
	    }
	    m = med3(x, aInds, l, m, n); // Mid-size, med of 3
	}
	float v = x[aInds[m]];     // med3 returns one of the indices, so no need for aInds here

	// Establish Invariant: v* (<v)* (>v)* v*
	int a = off, b = a, c = off + len - 1, d = c;
	while(true) {
	    while (b <= c && x[aInds[b]] <= v) {
		if (x[aInds[b]] == v)
		    swap(aInds, a++, b);
		b++;
	    }
	    while (c >= b && x[aInds[c]] >= v) {
		if (x[aInds[c]] == v)
		    swap(aInds, c, d--);
		c--;
	    }
	    if (b > c)
		break;
	    swap(aInds, b++, c--);
	}

	// Swap partition elements back to middle
	int s, n = off + len;
	s = Math.min(a-off, b-a  );  vecswap(aInds, off, b-s, s);
	s = Math.min(d-c,   n-d-1);  vecswap(aInds, b,   n-s, s);

	// Recursively sort non-partition-elements
	if ((s = b-a) > 1)
	    indexSort(aInds, x, off, s);
	if ((s = d-c) > 1)
	    indexSort(aInds, x, n-s, s);
    }
    /**
     * Returns the index of the median of the three indexed floats.
     */
    private final static int med3(final float x[], final int[] aInds, final int a, final int b, final int c) {
	return (x[aInds[a]] < x[aInds[b]] ?
		(x[aInds[b]] < x[aInds[c]] ? b : x[aInds[a]] < x[aInds[c]] ? c : a) :
		(x[aInds[b]] > x[aInds[c]] ? b : x[aInds[a]] > x[aInds[c]] ? c : a));
    }
    /**
     * Swaps x[a] with x[b].
     */
    private final static void swap(final int aInds[], final int a, final int b) {
	final int t = aInds[a];
	aInds[a] = aInds[b];
	aInds[b] = t;
    }
    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(final int aInds[], int a, int b, final int n) {
	for (int i=0; i<n; i++, a++, b++)
	    swap(aInds, a, b);
    }

    //
    //  D O U B L E    S O R T I N G
    //

    public final static void indexSort(final int[] aInds, final double[] a) {
	    indexSort(aInds, a, 0, a.length);
    }

    public final static void indexSort(final int[] aInds, final double x[], final int off, final int len) {
	// Insertion sort on smallest arrays
	if (len < 7) {
	    for (int i=off; i<len+off; i++)
		for (int j=i; j>off && x[aInds[j-1]]>x[aInds[j]]; j--)
		    swap(aInds, j, j-1);
	    return;
	}

	// Choose a partition element, v
	int m = off + len/2;       // Small arrays, middle element
	if (len > 7) {
	    int l = off;
	    int n = off + len - 1;
	    if (len > 40) {        // Big arrays, pseudomedian of 9
		    int s = len/8;
		    l = med3(x, aInds, l, l+s, l+2*s);
		    m = med3(x, aInds, m-s, m, m+s);
		    n = med3(x, aInds, n-2*s, n-s, n);
	    }
	    m = med3(x, aInds, l, m, n); // Mid-size, med of 3
	}
	double v = x[aInds[m]];     // med3 returns one of the indices, so no need for aInds here

	// Establish Invariant: v* (<v)* (>v)* v*
	int a = off, b = a, c = off + len - 1, d = c;
	while(true) {
	    while (b <= c && x[aInds[b]] <= v) {
		if (x[aInds[b]] == v)
		    swap(aInds, a++, b);
		b++;
	    }
	    while (c >= b && x[aInds[c]] >= v) {
		if (x[aInds[c]] == v)
		    swap(aInds, c, d--);
		c--;
	    }
	    if (b > c)
		break;
	    swap(aInds, b++, c--);
	}

	// Swap partition elements back to middle
	int s, n = off + len;
	s = Math.min(a-off, b-a  );  vecswap(aInds, off, b-s, s);
	s = Math.min(d-c,   n-d-1);  vecswap(aInds, b,   n-s, s);

	// Recursively sort non-partition-elements
	if ((s = b-a) > 1)
	    indexSort(aInds, x, off, s);
	if ((s = d-c) > 1)
	    indexSort(aInds, x, n-s, s);
    }
    /**
     * Returns the index of the median of the three indexed doubles.
     */
    private final static int med3(final double x[], final int[] aInds, final int a, final int b, final int c) {
        return (x[aInds[a]] < x[aInds[b]] ?
        (x[aInds[b]] < x[aInds[c]] ? b : x[aInds[a]] < x[aInds[c]] ? c : a) :
            (x[aInds[b]] > x[aInds[c]] ? b : x[aInds[a]] > x[aInds[c]] ? c : a));
    }
    
    // M I S C. 
    /** reverses the order of the array */
    public static final void reverse(final int[] array) {
        final int half = array.length/2;
        for(int i = 0, j = array.length - 1; i < half; i++) {
            final int tmp = array[i];
            array[i] = array[j];
            array[j--] = tmp;
        }
    }
    /** reverses the order of the array */
    public static final void reverse(final Object[] array) {
        final int half = array.length/2;
        for(int i = 0, j = array.length - 1; i < half; i++) {
            final Object tmp = array[i];
            array[i] = array[j];
            array[j--] = tmp;
        }
    }
    // debug helper methods
    
    public final static void dump(final float[] aVec) {
        for (int i = 0; i < aVec.length; ++i)
            System.out.print(" " + aVec[i]);
        System.out.println();
    }
//    /** creates an array of Strings by parsing the input String using the delimiter */
//    public static final String[] splitStrings(final String text, final char delim) {
//        final int num = getNumOccurances(text, delim);
//        if(num == 0) {
//            return new String[] {text.trim()};
//        }
//        final String[] strings = new String[num];
//        for(int c = 0, last = 0, i = text.indexOf(delim); i >= 0; i = text.indexOf(delim, i)) {
//            strings[c++] = text.substring(last, i).trim();
//            last = i;
//        }
//        return strings;
//    }
//    /** gets the number of times the delimiter is present in the String */
//    public static final int getNumOccurances(final String text, final char delim) {
//        int count = 0;
//        for(int i = text.indexOf(delim); i >= 0; i = text.indexOf(delim, i)) {
//            count++;
//        }
//        return count;
//    }
    
    public final static String toString(final double[] aVec) {
        final StringBuffer sb = new StringBuffer(aVec.length * 20);
        final int limit = aVec.length;
        sb.append("(len="); sb.append(limit); sb.append(')');
        final char space = ' ';
        for (int i = 0; i < limit; ++i) {
            sb.append(space);
            sb.append(aVec[i]);
        }
        return sb.toString();
    }
    
    public final static String toString(final float[] aVec) {
        return toString(aVec, aVec.length);
    }
    
    public final static String toString(final float[] aVec, final int aNum) {
        final StringBuffer sb = new StringBuffer(aNum * 20);
        sb.append("(len="); sb.append(aNum); sb.append(')');
        final char space = ' ';
        for (int i = 0; i < aNum; ++i) {
            sb.append(space);
            sb.append(aVec[i]);
        }   
        return sb.toString();
    }
    
    public final static String toString(final int[] aVec) {
        final StringBuffer sb = new StringBuffer(aVec.length * 20);
        final int limit = aVec.length;
        sb.append("(len="); sb.append(limit); sb.append(')');
        final char space = ' ';
        for (int i = 0; i < limit; ++i) {
            sb.append(space);
            sb.append(aVec[i]);
        }
        return sb.toString();
    }
    /** helper method for debuging - prints the contents of the array */
    public final static String toString(final Object[] aVec) {
        return toString(aVec, aVec.length);
    }
     /** helper method for debuging - prints the contents of the array */
    public final static String toString(final Object[] aVec, final int aNum) {
        return toString(aVec, aVec.length, " ");
    }
    /** helper method for debuging - prints the contents of the array */
    public final static String toString(final Object[] aVec, final int aNum, final String seperator) {
        final StringBuffer sb = new StringBuffer(aNum * 20);
        //sb.append("(len="); sb.append(aNum); sb.append(')');
        //final char space = ' ';
        for (int i = 0; i < aNum; ++i) {
            //sb.append(space);
            sb.append(aVec[i]);
            sb.append(seperator);
        }   
        return sb.toString();
    }
    // I N N E R   C L A S S E S
    /** Keeps track of two numbers */
    public static class MinMax {
        /** the minimum value */
        public final int min;
        /** the max value */
        public final int max;
        
        /** constructs a new MinMax */
        public MinMax(final int val1, final int val2) {
            if(val1 < val2) {
                this.min = val1;
                this.max = val2;
            } else { // val1 >= val2
                this.min = val2;
                this.max = val1;
            }
        }
    }
    /** Keeps track of two numbers */
    public static class MinMaxFloat {
        /** the minimum value */
        public final float  min;
        /** the max value */
        public final float max;
        
        /** constructs a new MinMax */
        public MinMaxFloat(final float  val1, final float  val2) {
            if(val1 < val2) {
                this.min = val1;
                this.max = val2;
            } else { // val1 >= val2
                this.min = val2;
                this.max = val1;
            }
        }
    }
} // End ArrayUtils


/**
     * arg arr is untouched
     */
     /*
    public static float[] subarray(float[] a, int numb, Dir dir) {
        float[] res = new float[numb];
        if (dir == BOT2TOP) {
            System.arraycopy(a, 0, res, 0, numb);
        }
        else if (dir == TOP2BOT) {
            System.arraycopy(a, a.length-numb, res, 0, numb);
        }
        else if (dir == TWOTAILED) {
            if (!Dir.isEven(numb)) throw new IllegalArgumentException("Numb must be even for double sided");
            System.arraycopy(a, 0, res, 0, numb/2);
            System.arraycopy(a, a.length-numb, res, numb/2, numb/2);
        }
        return res;
    }
    */


