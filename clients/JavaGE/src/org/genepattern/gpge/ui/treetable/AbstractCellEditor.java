package org.genepattern.gpge.ui.treetable;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.*;
import java.io.Serializable;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */
public class AbstractCellEditor implements CellEditor {

   protected EventListenerList listenerList = new EventListenerList();


   public boolean shouldSelectCell(EventObject anEvent) {
      return false;
   }


   public boolean stopCellEditing() {
      return true;
   }


   public void cancelCellEditing() { }


   public void addCellEditorListener(CellEditorListener l) {
      listenerList.add(CellEditorListener.class, l);
   }


   public void removeCellEditorListener(CellEditorListener l) {
      listenerList.remove(CellEditorListener.class, l);
   }


   /*
       Notify all listeners that have registered interest for
       notification on this event type.
       @see EventListenerList
     */
   protected void fireEditingStopped() {
      // Guaranteed to return a non-null array
      Object[] listeners = listenerList.getListenerList();
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == CellEditorListener.class) {
            ((CellEditorListener) listeners[i + 1]).editingStopped(new ChangeEvent(this));
         }
      }
   }


   /*
       Notify all listeners that have registered interest for
       notification on this event type.
       @see EventListenerList
     */
   protected void fireEditingCanceled() {
      // Guaranteed to return a non-null array
      Object[] listeners = listenerList.getListenerList();
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == CellEditorListener.class) {
            ((CellEditorListener) listeners[i + 1]).editingCanceled(new ChangeEvent(this));
         }
      }
   }


   public Object getCellEditorValue() {
      return null;
   }


   public boolean isCellEditable(EventObject e) {
      return true;
   }
}
