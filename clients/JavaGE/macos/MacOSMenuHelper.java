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
