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
