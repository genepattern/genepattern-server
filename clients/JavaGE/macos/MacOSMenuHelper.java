/*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/


package macos;

import com.apple.eawt.*;
import com.apple.eio.*;

/**
 *  Utilites for mac menu bar
 *
 * @author    Joshua Gould
 */
public class MacOSMenuHelper {

   private MacOSMenuHelper() { }


   public static void registerHandlers() {
      Application app = new Application();
      app.addApplicationListener(new AppHandler());
      app.setEnabledPreferencesMenu(false);
      app.setEnabledAboutMenu(true);
   }


   static class AppHandler extends ApplicationAdapter {
      public void handleAbout(ApplicationEvent event) {
         event.setHandled(true);
         org.genepattern.gpge.GenePattern.showAbout();
      }


      public void handleQuit(ApplicationEvent event) {
         event.setHandled(true);
         System.exit(0);
      }
   }
}
