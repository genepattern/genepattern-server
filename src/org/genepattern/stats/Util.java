package org.genepattern.stats;

import org.genepattern.data.matrix.DoubleMatrix2D;
/**
 * @author    Joshua Gould
 */
public class Util {
   public static int ASCENDING = 0;
   public static int DESCENDING = 1;
   public static int ABSOLUTE = 2;

   private final static double kMinVariancePercent = .20;



   public static double[] sort(final double[] values, int order) {
      if(order == 0) {
         cern.colt.GenericSorting.quickSort(0, values.length, new AscendingComparator(values), new DataSwapper(values));
      } else if(order == 1) {
         cern.colt.GenericSorting.quickSort(0, values.length, new DescendingComparator(values), new DataSwapper(values));
      } else if(order == 2) {
         cern.colt.GenericSorting.quickSort(0, values.length, new AbsoluteDescendingComparator(values), new DataSwapper(values));
      } else {
         throw new IllegalArgumentException("Invalid order");
      }

      return values;
   }


   public static void pearsonCorrelation(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         double[] values) {
      if(classOneIndices.length != class2Indices.length) {
         throw new IllegalArgumentException("Size of classes must be equal for pearson correlation");
      }
      for(int i = 0, length = values.length; i < length; i++) {
         values[i] = pearsonCorrelation(dataset, classOneIndices, class2Indices, i);
      }
   }


   public static void cosineDistance(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         double[] values) {
      if(classOneIndices.length != class2Indices.length) {
         throw new IllegalArgumentException("Size of classes must be equal for cosine distance");
      }
      for(int i = 0, length = values.length; i < length; i++) {
         values[i] = cosineDistance(dataset, classOneIndices, class2Indices, i);
      }
   }


   public static double euclideanDistance(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         int row) {
      double dist = 0;
      int N = classOneIndices.length;
      for(int i = 0; i < N; ++i) {
         double x = dataset.get(row, classOneIndices[i]);
         double y = dataset.get(row, class2Indices[i]);
         dist += (x - y) * (x - y);
      }
      return Math.sqrt(dist);
   }


   /**
    *  Computes the rank using the given index array. The index array can be
    *  obtained from the index method.
    *
    * @param  indices  An index array.
    * @return          The ranks.
    * @see             index(double[], int)
    */
   public static int[] rank(int[] indices) {
      int[] rank = new int[indices.length];
      for(int j = 0; j < indices.length; j++) {
         rank[indices[j]] = j + 1;
      }
      return rank;
   }



   public static double mean(DoubleMatrix2D dataset, int[] indices, int row) {

      double sum = 0;

      for(int j = 0; j < indices.length; j++) {
         sum += dataset.get(row, indices[j]);
      }

      return sum / indices.length;
   }


   public static double median(DoubleMatrix2D dataset, int[] indices, int row) {
      double[] tempArray = new double[indices.length];
      for(int j = 0; j < indices.length; j++) {
         tempArray[j] = dataset.get(row, indices[j]);
      }

      java.util.Arrays.sort(tempArray);
      int half = indices.length / 2;
      if(indices.length % 2 == 0) {
         double k1 = tempArray[half];
         double k2 = tempArray[half - 1];
         return (k1 + k2) / 2.0;
      }
      return tempArray[half];
   }


   public static void ttest(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         double[] scores, boolean adjustStdev, double minStd) {

      int rows = dataset.getRowCount();

      for(int i = 0; i < rows; i++) {

         double class1Mean = Util.mean(dataset, classOneIndices, i);
         double class2Mean = Util.mean(dataset, class2Indices, i);
         double class1Std = Util.standardDeviation(dataset, classOneIndices,
               i, class1Mean);

         double class2Std = Util.standardDeviation(dataset, class2Indices, i,
               class2Mean);

         class1Std = adjustStdev(adjustStdev, class1Std, class1Mean);
         class2Std = adjustStdev(adjustStdev, class2Std, class2Mean);

         class1Std = Math.max(class1Std, minStd);
         class2Std = Math.max(class2Std, minStd);

         double Sxi = (class1Mean - class2Mean) / Math.sqrt((class1Std * class1Std / classOneIndices.length) + (class2Std * class2Std / class2Indices.length));
         scores[i] = Sxi;
      }
   }


   public static void ttest(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         double[] scores, boolean adjustStdev) {

      int rows = dataset.getRowCount();

      for(int i = 0; i < rows; i++) {

         double class1Mean = Util.mean(dataset, classOneIndices, i);
         double class2Mean = Util.mean(dataset, class2Indices, i);
         double class1Std = Util.standardDeviation(dataset, classOneIndices,
               i, class1Mean);
         double class2Std = Util.standardDeviation(dataset, class2Indices, i,
               class2Mean);

         class1Std = adjustStdev(adjustStdev, class1Std, class1Mean);
         class2Std = adjustStdev(adjustStdev, class2Std, class2Mean);

         double Sxi = (class1Mean - class2Mean) / Math.sqrt((class1Std * class1Std / classOneIndices.length) + (class2Std * class2Std / class2Indices.length));
         scores[i] = Sxi;
      }
   }


   public static void ttestMedian(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         double[] scores, boolean adjustStdev) {

      int rows = dataset.getRowCount();

      for(int i = 0; i < rows; i++) {

         double class1Mean = Util.median(dataset, classOneIndices, i);
         double class2Mean = Util.median(dataset, class2Indices, i);
         double class1Std = Util.standardDeviation(dataset, classOneIndices,
               i, class1Mean);
         double class2Std = Util.standardDeviation(dataset, class2Indices, i,
               class2Mean);

         class1Std = adjustStdev(adjustStdev, class1Std, class1Mean);
         class2Std = adjustStdev(adjustStdev, class2Std, class2Mean);

         double Sxi = (class1Mean - class2Mean) / Math.sqrt((class1Std * class1Std / classOneIndices.length) + (class2Std * class2Std / class2Indices.length));
         scores[i] = Sxi;
      }
   }


   /**
    *  Adjusts the standard deviation. The standard deviation will be set to
    *  0.1 if the standard deviation equals 0. In addition, the standard
    *  deviation will be set to 20 percent of the given mean if <code>minStd</code>
    *  is <code>true</code>.
    *
    * @param  stdev   The standard deviation, as returned by standardDeviation
    * @param  mean    The mean, as returned by mean
    * @param  minStd  Whether to ensure that the standard deviation is at least
    *      20 percent of the mean
    * @return         The adjusted standard deviation
    */
   public static double adjustStdev(boolean minStd, double stdev, double mean) {
      double returnValue = stdev;
      if(minStd) {
         double absMean = Math.abs(mean);
         double minS = kMinVariancePercent * absMean;
         if(minS > stdev) {
            returnValue = minS;
         }
      }
      if(returnValue == 0) {
         returnValue = 0.1;
      }
      return returnValue;
   }


   public static void snr(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         double[] scores, boolean adjustStdev) {

      int rows = dataset.getRowCount();

      for(int i = 0; i < rows; i++) {

         double class1Mean = Util.mean(dataset, classOneIndices, i);
         double class2Mean = Util.mean(dataset, class2Indices, i);
         double class1Std = Util.standardDeviation(dataset, classOneIndices,
               i, class1Mean);
         double class2Std = Util.standardDeviation(dataset, class2Indices, i,
               class2Mean);

         class1Std = adjustStdev(adjustStdev, class1Std, class1Mean);
         class2Std = adjustStdev(adjustStdev, class2Std, class2Mean);

         double Sxi = (class1Mean - class2Mean) / (class1Std +
               class2Std);
         scores[i] = Sxi;
      }
   }


   public static void snrMedian(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         double[] scores, boolean adjustStdev) {

      int rows = dataset.getRowCount();

      for(int i = 0; i < rows; i++) {
         double class1Mean = Util.median(dataset, classOneIndices, i);
         double class2Mean = Util.median(dataset, class2Indices, i);
         double class1Std = Util.standardDeviation(dataset, classOneIndices,
               i, class1Mean);
         double class2Std = Util.standardDeviation(dataset, class2Indices, i,
               class2Mean);

         class1Std = adjustStdev(adjustStdev, class1Std, class1Mean);
         class2Std = adjustStdev(adjustStdev, class2Std, class2Mean);

         double Sxi = (class1Mean - class2Mean) / (class1Std +
               class2Std);
         scores[i] = Sxi;
      }
   }


   public static Function computeLinearRegression(double[] xpoints, double[] ypoints) {
      double xBar_yBar = 0;
      double xBar = 0;
      double yBar = 0;
      double x2Bar = 0;
      int n = xpoints.length;
      for(int i = 0; i < n; i++) {
         double x = xpoints[i];
         double y = ypoints[i];
         xBar_yBar += x * y;
         xBar += x;
         yBar += y;
         x2Bar += x * x;
      }

      xBar_yBar = xBar_yBar / n;
      xBar = xBar / n;
      yBar = yBar / n;
      x2Bar = x2Bar / n;
      double deltaX2 = x2Bar - xBar * xBar;
      final double m = (xBar_yBar - xBar * yBar) / deltaX2;
      final double b = yBar - m * xBar;
      java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
      nf.setMaximumFractionDigits(2);
      final String s = "y = " + nf.format(m) + "*x " + " + " + nf.format(b);
      return
         new Function() {
            public double evaluate(double x) {
               return m * x + b;
            }


            public String toString() {
               return s;
            }
         };
   }


   public static double standardDeviation(DoubleMatrix2D dataset, int[] indices,
         int row, double mean) {

      double sum = 0;

      for(int j = 0; j < indices.length; j++) {

         double x = dataset.get(row, indices[j]);
         double diff = x - mean;
         diff = diff * diff;
         sum += diff;
      }

      double variance = sum / (indices.length - 1);

      return Math.sqrt(variance);
   }


   /**
    *  Indexes the given array values array. Creates a new index array,indx,
    *  such that values[indx[j]] is in the specified order for j = 0, 2, ...
    *  The input array is not changed. {
    *
    * @param  order   one of ASCENDING, DESCENDING, ABSOLUTE
    * @param  values  The array to construct an index table for.
    * @return         The indices
    */
   public static int[] index(final double[] values, int order) {
      final int[] indices = new int[values.length];
      for(int i = 0; i < indices.length; i++) {
         indices[i] = i;
      }
      if(order == 0) {
         cern.colt.GenericSorting.quickSort(0, values.length, new AscendingIndexComparator(values, indices), new IndexSwapper(indices));
      } else if(order == 1) {
         cern.colt.GenericSorting.quickSort(0, values.length, new DescendingIndexComparator(values, indices), new IndexSwapper(indices));
      } else if(order == 2) {
         cern.colt.GenericSorting.quickSort(0, values.length, new AbsoluteDescendingIndexComparator(values, indices), new IndexSwapper(indices));
      } else {
         throw new IllegalArgumentException("Invalid order");
      }

      return indices;
   }


   private static double cosineDistance(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         int row) {
      double mag_x = 0;
      double mag_y = 0;
      double sum = 0;
      int N = classOneIndices.length;
      for(int i = 0; i < N; i++) {
         double x = dataset.get(row, classOneIndices[i]);
         double y = dataset.get(row, class2Indices[i]);
         mag_x += x * x;
         mag_y += y * y;
         sum += x * y;
      }

      return (sum / Math.sqrt(mag_x * mag_y));
   }


   /**
    *  http://davidmlane.com/hyperstat/A51911.html numr -> sum(xy) -
    *  (sum(x)*sum(y)) / N denr -> sqrt((sum(x**2) - ((sum(x))**2)/N) *
    *  (sum(y**2) - ((sum(y))**2)/N))
    *
    * @param  dataset          Description of the Parameter
    * @param  classOneIndices  Description of the Parameter
    * @param  class2Indices    Description of the Parameter
    * @param  row              Description of the Parameter
    * @return                  Description of the Return Value
    */
   private static double pearsonCorrelation(DoubleMatrix2D dataset,
         int[] classOneIndices,
         int[] class2Indices,
         int row) {

      double sumx = 0;
      double sumxx = 0;

      double sumy = 0;
      double sumyy = 0;

      double sumxy = 0;
      int N = classOneIndices.length;
      for(int i = 0; i < N; i++) {

         double x = dataset.get(row, classOneIndices[i]);
         double y = dataset.get(row, class2Indices[i]);
         sumx += x;
         sumxx += x * x;

         sumy += y;
         sumyy += y * y;

         sumxy += x * y;
      }

      double numr = sumxy - (sumx * sumy / N);
      double denr = Math.sqrt((sumxx - (sumx * sumx / N)) *
            (sumyy - (sumy * sumy / N)));
      return numr / denr;
   }


   /**
    * @author    Joshua Gould
    */
   static class AscendingIndexComparator implements cern.colt.function.IntComparator {
      double[] values;
      int[] indices;


      public AscendingIndexComparator(double[] values, int[] indices) {
         this.values = values;
         this.indices = indices;
      }


      public int compare(int o1, int o2) {
         if(values[indices[o1]] < values[indices[o2]]) {
            return -1;
         } else if(values[indices[o1]] > values[indices[o2]]) {
            return 1;
         }
         return 0;
      }
   }


   /**
    * @author    Joshua Gould
    */
   static class DescendingIndexComparator implements cern.colt.function.IntComparator {
      double[] values;
      int[] indices;


      public DescendingIndexComparator(double[] values, int[] indices) {
         this.values = values;
         this.indices = indices;
      }


      public int compare(int o1, int o2) {
         if(values[indices[o1]] < values[indices[o2]]) {
            return 1;
         } else if(values[indices[o1]] > values[indices[o2]]) {
            return -1;
         }
         return 0;
      }
   }


   /**
    * @author    Joshua Gould
    */
   static class AbsoluteDescendingIndexComparator implements cern.colt.function.IntComparator {
      double[] values;
      int[] indices;


      public AbsoluteDescendingIndexComparator(double[] values, int[] indices) {
         this.values = values;
         this.indices = indices;
      }


      public int compare(int o1, int o2) {
         if(Math.abs(values[indices[o1]]) < Math.abs(values[indices[o2]])) {
            return 1;
         } else if(Math.abs(values[indices[o1]]) > Math.abs(values[indices[o2]])) {
            return -1;
         }
         return 0;
      }
   }


   /**
    * @author    Joshua Gould
    */
   static class IndexSwapper implements cern.colt.Swapper {
      int[] indices;


      public IndexSwapper(int[] indices) {
         this.indices = indices;
      }


      public void swap(int o1, int o2) {
         int ind1 = indices[o1];
         indices[o1] = indices[o2];
         indices[o2] = ind1;
      }
   }


   /**
    * @author    Joshua Gould
    */
   static class DataSwapper implements cern.colt.Swapper {
      double[] values;


      public DataSwapper(double[] values) {
         this.values = values;
      }


      public void swap(int o1, int o2) {
         double tmp = values[o1];
         values[o1] = values[o2];
         values[o2] = tmp;
      }
   }


   /**
    * @author    Joshua Gould
    */
   static class AscendingComparator implements cern.colt.function.IntComparator {
      double[] values;



      public AscendingComparator(double[] values) {
         this.values = values;

      }


      public int compare(int o1, int o2) {
         if(values[o1] < values[o2]) {
            return -1;
         } else if(values[o1] > values[o2]) {
            return 1;
         }
         return 0;
      }
   }


   /**
    * @author    Joshua Gould
    */
   static class DescendingComparator implements cern.colt.function.IntComparator {
      double[] values;


      public DescendingComparator(double[] values) {
         this.values = values;

      }


      public int compare(int o1, int o2) {
         if(values[o1] < values[o2]) {
            return 1;
         } else if(values[o1] > values[o2]) {
            return -1;
         }
         return 0;
      }
   }


   /**
    * @author    Joshua Gould
    */
   static class AbsoluteDescendingComparator implements cern.colt.function.IntComparator {
      double[] values;


      public AbsoluteDescendingComparator(double[] values) {
         this.values = values;

      }


      public int compare(int o1, int o2) {
         if(Math.abs(values[o1]) < Math.abs(values[o2])) {
            return 1;
         } else if(Math.abs(values[o1]) > Math.abs(values[o2])) {
            return -1;
         }
         return 0;
      }
   }

}
