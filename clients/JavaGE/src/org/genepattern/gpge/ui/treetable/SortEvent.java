package org.genepattern.gpge.ui.treetable;

public class SortEvent extends java.util.EventObject {
   private int column;
   private boolean ascending;
   
   public SortEvent(Object source, int column, boolean ascending) {
      super(source);
      this.column = column;
      this.ascending = ascending;
   }
   
   public int getColumn() {
      return column;  
   }
   
   public boolean isAscending() {
      return ascending;  
   }
}