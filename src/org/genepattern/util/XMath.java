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

package org.genepattern.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.genepattern.data.Matrix;

/**
 * Functionally extends java.lang.Math with additional math related methods.
 *
 * @author Michael Angelo (CMath in GeneCluster)
 * @author Aravind Subramanian
 * @version %I%, %G%
 */


public class XMath {

    private static Random fRng = new Random(149L);

    /**
     * Privatized class constructor.
     */
    private XMath() {}

    public static final boolean isEven(final int x) {
        return ( (x & 1) == 0 );//((x>>1)<<1 == x);
    }
//    public static final void main(final String[] args) {
//        boolean flip = true;
//        final int limit = 100;
//        for(int i = -limit; i < limit; i++) {
//            if(! (flip == isEven(i)) )
//                System.out.println("Error: "+i+" was determined to be "+ ((isEven(i)) ? "even" : "odd") );
//             flip = !flip;
//        }
//    }

    public static final boolean isDivisibleBy(final  int x, final  int y) {
        return (x % y == 0);
    }

    public static final boolean isNumeric(final  Object[] obj) {
        for (int i=0; i < obj.length; i++) {
            try {
                Float.parseFloat(obj[i].toString());
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * are n1 and n2 within fuzz of each other
     */
    public static final boolean equals(final  int fuzz, final  int n1, final  int n2) {

        if ((Math.abs(n1-n2)) <= fuzz) return true;
        return false;
    }
    /** sumOfSquares is x1^2 + x2^2 + ... */
    public static float biasedStd(final float sumOfSquares, final float mean, final int num) {
	final int norm = (num > 1 ? num - 1 : 1); // no zero in denominator
	return (float) Math.sqrt(((sumOfSquares - num * mean * mean) / norm));
    }
    /**
     * Some heuristics for adjusting variance based on data from affy chips.
     * NOTE: problem occurs when we threshold to a value, then that artificially
     *   reduces the variance in the data
     *
     * First, we make the variance at least a fixed percent of the mean
     * If the mean is too small, then we use an absolute variance
     *
     * However, we don't want to bias our algs for affy data, e.g. we may
     * get data in 0..1 and in that case it is not appropriate to use
     * an absolute standard deviation of 10 - will kill the signal.
     */
    public static final float fixStdDev(final float s, final float m){
        //FIXME this needs to have the min std dev settable
        final float fix_var_thresh = 0.20f;
	float minS = s;
	if (true/*CGlobals.kFixVar*/) {
	    final float absMean = Math.abs(m);
	    //minS = CGlobals.kFixVarThreshold * absMean;
            minS = fix_var_thresh * absMean;

	    // In the case of a zero mean, assume the mean is 1
	    if (minS == 0)
		minS = fix_var_thresh;

	    // make sure minS is at least as big as s
	    if (minS < s)
		minS = s;
	}
	return minS;
    }

  /**
   * Converts radians to degrees.
   *
   * @param rad the input value (in radians)
   * @return <code>rad</code> converted to degrees
   * @see #deg2rad(double)
   * @since org.dyndns.aoi.Utilities 1.0
   */
  public static final double rad2deg(final  double rad) {
    return (rad * 360) / (2 * Math.PI);
  }

  /**
   * Converts degrees to radians.
   *
   * @param deg the input value (in degrees)
   * @return <code>deg</code> converted to radians
   * @see #rad2deg(double)
   * @since org.dyndns.aoi.Utilities 1.0
   */
  public static final double deg2rad(final  double deg) {
    return (deg * (2 * Math.PI)) / 360;
  }


    /** Creates an array whose elemnts are randomly arranged between
     * 0 and num - 1.
     * Example: randomize(5) could yeild: 0, 3, 1, 2, 4
     *
     * The same random number generator is employed (in this instance of the jvm)
     * so safe to call multiple times.
     * But as it uses the same seed each time, the rnd stays the same from
     * jvm invoc to jvm invoc. See link below for more. See http://mindprod.com/gotchas.html#RANDOM
     *
     * {@link http://mindprod.com}
     * @param num
     * @return int[] array of random ints
     */

    public static final int[] randomizeWithoutReplacement(final  int num) {

        List seen = new ArrayList(num);
        int[] inds = new int[num];
        int cnt = 0;
        for (int i = 0; i < num;) {
            int r = fRng.nextInt(num);
            if (seen.contains(new Integer(r))) continue;
            seen.add(new Integer(r));
            inds[cnt++] = r;
            if (cnt == num) break;
        }

        return inds;
    }
    /** */
    public static final double normalizedEntropy(final  double[] array) {
        // normFactor is the entropy of a uniform distribution of the appropriate length
        final  int len = array.length;
        final  double normFactor = Math.log(1.0 / len);
        double ent = 0;
        for (int i = 0; i < len; ++i) {
            final  double val = array[i];
            if (val > 0)
                ent += val * Math.log(val);
        }
        return ent / normFactor;
    }
    /** gets the mean of the array */
    public static final float getMean(final float [] vec) {
        return getSum(vec)/vec.length;
    }
    /** returns the means for each of the rows by adding column to column */
    public static final float [] getRowsMeans(final Matrix matrix) {
        final int num_rows = matrix.getRowCount();
        final float [] means = new float [num_rows];
        final float [] the_row = new float [matrix.getColumnCount()];
        for(int r = 0; r < num_rows; r++) {
            matrix.getRow(r, the_row);
            means[r] = getMean(the_row);
        }
        return means;
    }
    public static final float[] getColumnsMeans(final Matrix matrix){
        //float[][] theData = getData();
        final int numRows = matrix.getRowCount();
        final int numCols = matrix.getColumnCount();
	final float[] colMean = new float[numCols];
        final float [] theRow = new float [numCols];
	for (int r = 0; r < numRows; r++)  {
	    //float[] theRow = theData[i];
            matrix.getRow(r, theRow);
	    for (int c = 0; c < numCols; c++)
		colMean[c] += theRow[c];
        }

	for (int c = 0; c < numCols; c++)
	    colMean[c] /= numRows;

	return colMean;
    }
    /** getst the sum of the elements in the array */
    public static final float getSum(final float [] vec) {
        final int len = vec.length;
        int sum = 0;
        for(int i = 0; i < len; i++) {
            sum += vec[i];
        }
        return sum;
    }
    /** gets the median of the array starting from 0 to len - 1 */
    public static final float getMedian(float[] vec, final int len) {
	final float[] v1 = new float[len];
	System.arraycopy(vec, 0, v1, 0, len);
        return getMedianModifies(v1);
    }
    /** gets the median of the array */
    public static final float getMedian(float[] v1) {
        return getMedian(v1, v1.length);
    }
    /** doesn't make a copy of the array */    
    private static final float getMedianModifies(float[] v1) {
        final int len = v1.length;
	Arrays.sort(v1);
	final int ind = (len - 1) / 2;
	if (isEven(len))
	    return (v1[ind] + v1[ind + 1]) / 2;
	else
	    return v1[ind];
    }
    //FIXME this is needed for some calcs
//    /**
//     * return the variance of the rows of this matrix about the
//     * global mean of the rows.
//     */
//    public static final float getTotalVariance(final Matrix matrix) {
//        //float[][] theData = getData();
//        final int numRows = matrix.getRowCount();
//        final int numCols = matrix.getColumnCount();
//        final float[] rowMean = getColumnsMeans(matrix);
//        //System.out.println("getTotalVariance() numRows="+numRows+" numCols="+numCols
//        //    +"\nrowMean="+ArrayUtils.toString(rowMean));
//	final float[] row = matrix.getRow(0);
//        System.out.println("row array len="+row.length);
//        for (int r = 0; r < numRows; r++) {
//            matrix.getRow(r, row);
//            System.out.println("row"+r+" init 10="+ArrayUtils.toString(row, Math.min(10, row.length)));
//        }
//	float rowVariance = 0;
//        final edu.mit.genome.gp.analytics.distance.FloatArraysDFInput input = 
//            new edu.mit.genome.gp.analytics.distance.FloatArraysDFInput(rowMean, row);
//        System.out.println("Intermediate rowVariance:");
//	for (int r = 0; r < numRows; r++) {
//	    //float[] theRow = theData[i];
//            matrix.getRow(r, row);
//	    //rowVariance += CMath.EuclidDistSquared(rowMean, theRow);
//            rowVariance += edu.mit.genome.gp.analytics.distance.EuclideanDistance.calculateEuclideanDistanceSquaredAsFloat(input);
//            System.out.print(" "+rowVariance);
//        }
//        System.out.println();
//
//        return rowVariance / (numRows - 1);
//    }
} // End XMath
