package org.genepattern.util;
import org.genepattern.data.matrix.DoubleMatrix2D;

/**
 *  Description of the Interface
 *
 * @author    Joshua Gould
 */
public interface ITestStatistic {
   public void compute(DoubleMatrix2D dataset,
         int[] classZeroIndices,
         int[] classOneIndices, double[] scores);
}
