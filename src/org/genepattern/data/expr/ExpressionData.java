package org.genepattern.data.expr;
import org.genepattern.data.matrix.DoubleMatrix2D;

/**
 *  Implementation of IExpressionData interface
 *
 * @author    Joshua Gould
 */
public class ExpressionData implements IExpressionData {
   protected DoubleMatrix2D dataset;
   protected String[] rowDescriptions;
   protected String[] columnDescriptions;


   public ExpressionData(DoubleMatrix2D dataset, String[] _rowDescriptions, String[] _columnDescriptions) {
      this.dataset = dataset;
      if(_rowDescriptions != null) {
         if(_rowDescriptions.length != dataset.getRowCount()) {
            throw new IllegalArgumentException("Length of row descriptions not equal to number of rows in matrix.");
         }
         this.rowDescriptions = _rowDescriptions;
      }

      if(_columnDescriptions != null) {
         if(_columnDescriptions.length != dataset.getColumnCount()) {
            throw new IllegalArgumentException("Length of column descriptions not equal to number of columns in matrix.");
         }
         this.columnDescriptions = _columnDescriptions;
      }
   }


   public ExpressionData slice(int[] rowIndices, int[] columnIndices) {
      if(rowIndices == null) {
         rowIndices = new int[dataset.getRowCount()];
         for(int i = dataset.getRowCount(); --i >= 0; ) {
            rowIndices[i] = i;
         }
      }
      if(columnIndices == null) {
         columnIndices = new int[dataset.getColumnCount()];
         for(int i = dataset.getColumnCount(); --i >= 0; ) {
            columnIndices[i] = i;
         }
      }

      DoubleMatrix2D newDoubleMatrix2D = dataset.slice(rowIndices, columnIndices);
      String[] newRowAnnotations = null;

      if(rowDescriptions != null) {
         newRowAnnotations = new String[rowIndices.length];
         for(int i = 0, length = rowIndices.length; i < length; i++) {
            newRowAnnotations[i] = rowDescriptions[rowIndices[i]];
         }
      }

      String[] newColumnAnnotations = null;

      if(columnDescriptions != null) {
         newColumnAnnotations = new String[columnIndices.length];
         for(int j = 0, length = columnIndices.length; j < length; j++) {
            newColumnAnnotations[j] = columnDescriptions[columnIndices[j]];
         }
      }

      return new ExpressionData(newDoubleMatrix2D, newRowAnnotations, newColumnAnnotations);
   }


   public void setRowDescription(int row, String description) {
      rowDescriptions[row] = description;
   }


   public void setColumnDescription(int column, String description) {
      columnDescriptions[column] = description;
   }


   public void setColumnName(int column, String name) {
      dataset.setColumnName(column, name);
   }


   public void setRowName(int row, String name) {
      dataset.setRowName(row, name);
   }


   public String getColumnDescription(int column) {
      if(columnDescriptions != null) {
         return columnDescriptions[column];
      }
      return null;
   }


   public String getRowDescription(int row) {
      if(rowDescriptions != null) {
         return rowDescriptions[row];
      }
      return null;
   }


   public int getRowCount() {
      return dataset.getRowCount();
   }


   public int getColumnCount() {
      return dataset.getColumnCount();
   }


   public double getValue(int row, int column) {
      return dataset.get(row, column);
   }


   public String getValueAsString(int row, int column) {
      return String.valueOf(dataset.get(row, column));
   }


   public String getColumnName(int column) {
      return dataset.getColumnName(column);
   }


   public String getRowName(int row) {
      return dataset.getRowName(row);
   }


   /**
    *  Allocates a new array contains the row names
    *
    * @return    The row names.
    */
   public String[] getRowNames() {
      return dataset.getRowNames();
   }


   /**
    *  Allocates a new array contains the row descriptions
    *
    * @return    The row descriptions.
    */
   public String[] getRowDescriptions() {
      if(rowDescriptions==null) {
         return new String[getRowCount()];  
      }
      return (String[]) rowDescriptions.clone();
   }


   /**
    *  Allocates a new array contains the column names
    *
    * @return    The column names.
    */
   public String[] getColumnNames() {
      return dataset.getColumnNames();
   }


   /**
    *  Allocates a new array contains the column descriptions
    *
    * @return    The column descriptions.
    */
   public String[] getColumnDescriptions() {
      if(columnDescriptions==null) {
         return new String[getColumnCount()];  
      }
      return (String[]) columnDescriptions.clone();
   }


   /**
    *  Gets the 2-dimensional matrix that holds the expression values
    *
    * @return    The expression matrix
    */
   public org.genepattern.data.matrix.DoubleMatrix2D getExpressionMatrix() {
      return dataset;
   }


   /**
    *  Gets the underlying double[][] array
    *
    * @return    the array
    */
   public double[][] getArray() {
      return dataset.getArray();
   }

}

