package org.genepattern.util;
import org.genepattern.data.matrix.DoubleMatrix2D;
/**
 *  Collection of test statistics
 *
 * @author    Joshua Gould
 */
public class TestStatistics {
   private TestStatistics() { }


   /**
    *  Pearson correlation
    *
    * @author    Joshua Gould
    */
   public static class PearsonCorrelation implements ITestStatistic {
      public void compute(DoubleMatrix2D dataset,
            int[] classZeroIndices,
            int[] classOneIndices, double[] scores) {
         Util.pearsonCorrelation(dataset, classZeroIndices, classOneIndices, scores);
      }
      
      public String toString() {
         return "Pearson Correlation";  
      }
   }


   /**
    *  T-Test using median instead of mean
    *
    * @author    Joshua Gould
    */
   public static class TTestMedian implements ITestStatistic {
      boolean fixStdev;


      public TTestMedian(boolean fixStdev) {
         this.fixStdev = fixStdev;
      }


      public void compute(DoubleMatrix2D dataset,
            int[] classZeroIndices,
            int[] classOneIndices, double[] scores) {
         Util.ttestMedian(dataset,
               classZeroIndices,
               classOneIndices, scores, fixStdev);

      }


      public String toString() {
         return "T-Test (median)";
      }
   }


   /**
    *  Signal to Noise
    *
    * @author    Joshua Gould
    */
   public static class SNR implements ITestStatistic {

      boolean fixStdev;


      public SNR(boolean fixStdev) {
         this.fixStdev = fixStdev;
      }


      public void compute(DoubleMatrix2D dataset,
            int[] classZeroIndices,
            int[] classOneIndices, double[] scores) {
         Util.snr(dataset,
               classZeroIndices,
               classOneIndices, scores, fixStdev);

      }


      public String toString() {
         return "SNR";
      }
   }


   /**
    *  Signal to Noise using median instead of mean
    *
    * @author    Joshua Gould
    */
   public static class SNRMedian implements ITestStatistic {

      boolean fixStdev;


      public SNRMedian(boolean fixStdev) {
         this.fixStdev = fixStdev;
      }


      public void compute(DoubleMatrix2D dataset,
            int[] classZeroIndices,
            int[] classOneIndices, double[] scores) {
         Util.snrMedian(dataset,
               classZeroIndices,
               classOneIndices, scores, fixStdev);

      }


      public String toString() {
         return "SNR (median)";
      }
   }


   /**
    *  T-Test which imposes a minimum standard
    *
    * @author    Joshua Gould
    */
   public static class TTestMinStd implements ITestStatistic {
      double minStd;
      boolean fixStdev;


      public TTestMinStd(double min, boolean fixStdev) {
         this.minStd = min;
         this.fixStdev = fixStdev;
      }


      public void compute(DoubleMatrix2D dataset,
            int[] classZeroIndices,
            int[] classOneIndices, double[] scores) {
         Util.ttest(dataset,
               classZeroIndices,
               classOneIndices, scores, fixStdev, minStd);

      }


      public String toString() {
         return "T-Test (min std=" + minStd + ")";
      }
   }

    /**
    *  T-Test
    *
    * @author    Joshua Gould
    */
   public static class TTest implements ITestStatistic {

      boolean fixStdev;


      public TTest(boolean fixStdev) {

         this.fixStdev = fixStdev;
      }


      public void compute(DoubleMatrix2D dataset,
            int[] classZeroIndices,
            int[] classOneIndices, double[] scores) {
         Util.ttest(dataset,
               classZeroIndices,
               classOneIndices, scores, fixStdev);

      }


      public String toString() {
         return "T-Test";
      }
   }
}
