package org.genepattern.gpge.ui.maindisplay;
import java.net.URL;
import javax.swing.*;

/**
 *  Manages application icons
 *
 * @author    Joshua Gould
 */
public class IconManager {
   public final static String DELETE_ICON = "delete.gif";
   public final static String REFRESH_ICON = "refresh.gif";
   public final static String SAVE_ICON = "save.gif";
   public final static String SAVE_AS_ICON = "save_as.gif";
   public final static String STOP_ICON = "stop.gif";
   public final static String NEW_PROJECT_ICON = "open.gif";
   public final static String REMOVE_ICON = "remove.gif";
   public final static String TEXT_ICON = "text.gif";
   public final static String ERROR_ICON = "error.gif";
   public final static String SEND_TO_ICON = "send_to.gif";
   public final static String IMPORT_ICON = "import.gif";
   
   private IconManager() { }


   public static Icon loadIcon(String name) {
      java.net.URL imgURL = ClassLoader.getSystemResource("org/genepattern/gpge/resources/icons/" + name);
      if(imgURL != null) {
         return new ImageIcon(imgURL);
      } else {
         System.err.println("Unable to find " + name);
         return null;
      }
   }
}
