package org.genepattern.gpge.ui.menu;
/*
    Sample code.
    Permission is given to use or modify this code in your own code.
    Lee Ann Rucker
  */
import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 *  MenuAction is an AbstractAction designed to be shared amongst several
 *  JMenuBars; it controls JMenus which contain shared MenuItemActions
 *
 * @author    Joshua Gould
 */
public class MenuAction extends AbstractAction {
   Object[] items;
   
     /**
    *  Constructs a new <code>MenuAction</code> which can be used to create a
    *  <code>JMenu</code> with the supplied string as its text
    *
    * @param  s      the text for the menu label
    */
   public MenuAction(String s) {
      super(s);
      items = new Object[0];
   }

   
     /**
    *  Constructs a new <code>MenuAction</code> which can be used to create a
    *  <code>JMenu</code> with the supplied string as its text
    *
    * @param  s      the text for the menu label
    */
   public MenuAction(String s, Icon icon) {
      super(s, icon);
      items = new Object[0];
   }

   

   /**
    *  Constructs a new <code>MenuAction</code> which can be used to create a
    *  <code>JMenu</code> with the supplied string as its text
    *
    * @param  s      the text for the menu label
    * @param  items  the contents of the menu
    */
   public MenuAction(String s, Object[] items) {
      super(s);
      this.items = items;
   }
   

   /**
    *  Convenience method to create a JMenuBar from an array of MenuActions.
    *  Each JFrame needs its own JMenuBar
    *
    * @param  menus  the MenuActions that will create the JMenuBar
    * @return        Description of the Return Value
    */
   public static JMenuBar makeMenuBar(MenuAction[] menus) {
      JMenuBar mb = new JMenuBar();
      for(int i = 0; i < menus.length; i++) {
         mb.add(menus[i].createMenu());
      }
      return mb;
   }


   /**
    *  Create a <code>JMenu</code> that contains the provided items and knows
    *  how to handle adding and removing them
    *
    * @return    Description of the Return Value
    */
   public JMenu createMenu() {
      JMenu menu = new JActionMenu(this);
      addItems(menu);
      return menu;
   }
   
   /**
    *  Create a <code>JMenu</code> that contains the provided items and knows
    *  how to handle adding and removing them
    *
    * @return    Description of the Return Value
    */
   public JPopupMenu createPopupMenu() {
      JActionPopupMenu menu = new JActionPopupMenu(this);   
      addItems(menu);
      return menu;
   }


   /**
    *  Does nothing, since JMenus are usually containers for JMenuItems
    *
    * @param  e  Description of the Parameter
    */
   public void actionPerformed(ActionEvent e) { }

  
   public void insert(MenuItemAction action, int index) {
      Object oldData[] = items;
      int size = oldData.length;
      items = new Object[size+1];
      System.arraycopy(oldData, 0, items, 0, size);
      System.arraycopy(items, index, items, index + 1,
			 size - index);
      items[index] = action;
      firePropertyChange("MenuAction.insert", null, new InsertPropertyChange(action, index));
   }
  
   public int getItemCount() {
      return items.length;
   }
   
   public Object getMenuComponent(int i) {
      return items[i];
   }
 
   /**
    *  Add an item to this MenuAction and all the JMenu instances that use it
    *
    * @param  action  Description of the Parameter
    */
   public void add(MenuItemAction action) {
      int oldLength = items.length;
      Object[] newItems = new Object[oldLength + 1];
      System.arraycopy(items, 0, newItems, 0, oldLength);
      items = newItems;
      items[oldLength] = action;
      firePropertyChange("MenuAction.addAction", null, action);
   }

   
   /**
    *  Removes all items from this MenuAction and all the JMenu instances that use it
    */
   public void removeAll() {
      for(int i = 0; i < items.length; i++) {
         items[i] = null;  
      }
      items = new Object[0];
      firePropertyChange("MenuAction.removeAll", null, null);
   }
   
   /**
    *  Remove an item from this MenuAction and all the JMenu instances that use
    *  it
    */
   public void remove(int index) {
      int oldLength = items.length;
      Object[] newItems = new Object[oldLength - 1];
      System.arraycopy(items, 0, newItems, 0, index);
      System.arraycopy(items, index + 1,
            newItems, index,
            oldLength - index - 1);
      items = newItems;
      firePropertyChange("MenuAction.remove", null, new Integer(index));
   }

   /**
    *  Remove an item from this MenuAction and all the JMenu instances that use
    *  it
    *
    * @param  action  Description of the Parameter
    */
   public void remove(Object action) {
      // Find the index of this action
      // and remove based on index
      int index = -1;
      for(int i = 0; i < items.length; i++) {
         if(action.equals(items[i])) {
            index = i;
            break;
         }
      }
      if(index >= 0) {
         int oldLength = items.length;
         Object[] newItems = new Object[oldLength - 1];
         System.arraycopy(items, 0, newItems, 0, index);
         System.arraycopy(items, index + 1,
               newItems, index,
               oldLength - index - 1);
         items = newItems;
         firePropertyChange("MenuAction.remove", null, new Integer(index));
      }
   }


   /**
    *  Add the items to this MenuAction's JMenu
    *
    * @param  menu  The feature to be added to the Items attribute
    */
   protected void addItems(JMenu menu) {
      if(items != null) {
         synchronized(menu.getTreeLock()) {
            for(int i = 0; i < items.length; i++) {
               if(items[i] instanceof MenuItemAction) {
                  menu.add(((MenuItemAction) items[i]).createMenuItem());
               } else if(items[i] instanceof JPopupMenu.Separator) {
                  menu.add(new JPopupMenu.Separator());
               } else if(items[i] instanceof JSeparator) {
                  menu.add(new JSeparator());
               } else if(items[i] instanceof MenuAction) {
                  menu.add(((MenuAction) items[i]).createMenu());
               }
               // if the item is a JSeparator, make a new one, if we reuse
               // the one from the array, it'll be removed from the last menu we made
            }
         }
      }
   }
   
   /**
    *  Add the items to this MenuAction's JMenu
    *
    * @param  menu  The feature to be added to the Items attribute
    */
   protected void addItems(JPopupMenu menu) {
      if(items != null) {
         synchronized(menu.getTreeLock()) {
            for(int i = 0; i < items.length; i++) {
               if(items[i] instanceof MenuItemAction) {
                  menu.add(((MenuItemAction) items[i]).createMenuItem());
               } else if(items[i] instanceof JPopupMenu.Separator) {
                  menu.add(new JPopupMenu.Separator());
               } else if(items[i] instanceof JSeparator) {
                  menu.add(new JSeparator());
               } else if(items[i] instanceof MenuAction) {
                  menu.add(((MenuAction) items[i]).createMenu());
               }
               // if the item is a JSeparator, make a new one, if we reuse
               // the one from the array, it'll be removed from the last menu we made
            }
         }
      }
   }


    /**
    *  Subclass of JMenu which handles adding and removing MenuActions
    *
    * @author    Joshua Gould
    */
   class JActionPopupMenu extends JPopupMenu {
      boolean fAddAll = false;


      JActionPopupMenu(Action action) {
        // super(action);
         action.addPropertyChangeListener(new ActionItemsChangedListener());
      }


      JPopupMenu getMenu() {
         return this;
      }


      private class ActionItemsChangedListener implements PropertyChangeListener {
         public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if(e.getPropertyName().equals("MenuAction.addAction")) {
               getMenu().add(((MenuItemAction) e.getNewValue()).createMenuItem());
            } else if(e.getPropertyName().equals("MenuAction.remove")) {
               getMenu().remove(((Integer) e.getNewValue()).intValue());
            } else if(e.getPropertyName().equals("MenuAction.insert")) {
               InsertPropertyChange c = (InsertPropertyChange) e.getNewValue();
               JMenuItem m = c.action.createMenuItem();
               getMenu().insert(m, c.index);  
            }
         }
      }
   }
   
   static class InsertPropertyChange {
      int index;
      MenuItemAction action;
      
      InsertPropertyChange(MenuItemAction a, int i) {
         action = a;
         index = i;
      }
   }
   
   /**
    *  Subclass of JMenu which handles adding and removing MenuActions
    *
    * @author    Joshua Gould
    */
   class JActionMenu extends JMenu {
      boolean fAddAll = false;


      JActionMenu(Action action) {
         super(action);
         action.addPropertyChangeListener(new ActionItemsChangedListener());
      }


      JMenu getMenu() {
         return this;
      }


      private class ActionItemsChangedListener implements PropertyChangeListener {
         public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if(e.getPropertyName().equals("MenuAction.addAction")) {
               getMenu().add(((MenuItemAction) e.getNewValue()).createMenuItem());
            } else if(e.getPropertyName().equals("MenuAction.remove")) {
               getMenu().remove(((Integer) e.getNewValue()).intValue());
            } else if(e.getPropertyName().equals("MenuAction.insert")) {
               InsertPropertyChange c = (InsertPropertyChange) e.getNewValue();
               JMenuItem m = c.action.createMenuItem();
               getMenu().insert(m, c.index);  
            } else if(e.getPropertyName().equals("MenuAction.removeAll")) {
               getMenu().removeAll();  
            }
         }
      }
   }
}
