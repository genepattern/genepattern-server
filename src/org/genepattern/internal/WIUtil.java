package org.genepattern.internal;
import org.genepattern.data.expr.*;
/**
 * @author    Joshua Gould
 */
public class WIUtil {

   public static org.genepattern.data.Dataset createWIDataset(ExpressionData data) {
      int rows = data.getRowCount();
      int columns = data.getColumnCount();
      String[] _rowDescriptions = new String[rows];
      String[] _columnDescriptions = new String[columns];
      String[] _rowNames = new String[rows];
      String[] _columnNames = new String[columns];
      float[] _data = new float[rows * columns];

      for(int i = 0; i < rows; i++) {
         _rowNames[i] = data.getRowName(i);
         _rowDescriptions[i] = data.getRowDescription(i);
         for(int j = 0; j < columns; j++) {
            _data[i * j + columns] = (float) data.getValue(i, j);
            _columnNames[i] = data.getColumnName(j);
            _columnDescriptions[j] = data.getColumnDescription(j);
         }
      }
      org.genepattern.data.annotation.AnnotationFactory rfactory = org.genepattern.data.annotation.PrimeAnnotationFactory.createAnnotationFactory(_rowNames, _rowDescriptions);
      org.genepattern.data.annotation.AnnotationFactory cfactory = org.genepattern.data.annotation.PrimeAnnotationFactory.createAnnotationFactory(_columnNames, _columnDescriptions);
      return new org.genepattern.data.DefaultDataset("data", new org.genepattern.data.DefaultMatrix(rows, columns, _data), new org.genepattern.data.DefaultNamesPanel(_rowNames, rfactory), new org.genepattern.data.DefaultNamesPanel(_columnNames, cfactory));
   }


   /*
       Returns an expression data instance backed by the specified dataset
       @param  dataset  The dataset
       @return          an expression data view of the specified dataset
     */
   public static IExpressionData asExpressionData(final org.genepattern.data.Dataset dataset) {
      return
         new IExpressionData() {
            public double getValue(int row, int column) {
               return dataset.getElement(row, column);
            }
            
            public String getValueAsString(int row, int column) {
               return String.valueOf(dataset.getElement(row, column));
            }


            public String getRowName(int row) {
               return dataset.getRowName(row);
            }


            public int getRowCount() {
               return dataset.getRowCount();
            }


            public int getColumnCount() {
               return dataset.getColumnCount();
            }


            public String getColumnName(int column) {
               return dataset.getColumnName(column);
            }


            public String getRowDescription(int row) {
               return dataset.getRowPanel().getAnnotation(row);
            }


            public String getColumnDescription(int column) {
               return dataset.getColumnPanel().getAnnotation(column);
            }
         };
   }


   public static org.genepattern.data.Dataset createWIDataset(int rows, int columns, String[] rowNames, String[] rowDescriptions, String[] columnNames, String[] columnDescriptions, float[] data) {
      org.genepattern.data.annotation.AnnotationFactory rfactory = org.genepattern.data.annotation.PrimeAnnotationFactory.createAnnotationFactory(rowNames, rowDescriptions);
      org.genepattern.data.annotation.AnnotationFactory cfactory = org.genepattern.data.annotation.PrimeAnnotationFactory.createAnnotationFactory(columnNames, columnDescriptions);
      return new org.genepattern.data.DefaultDataset("data", new org.genepattern.data.DefaultMatrix(rows, columns, data), new org.genepattern.data.DefaultNamesPanel(rowNames, rfactory), new org.genepattern.data.DefaultNamesPanel(columnNames, cfactory));
   }
}
