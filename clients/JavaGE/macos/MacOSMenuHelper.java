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


   public static void registerAboutHandler() {
      Application app2 = new Application();
      app2.addApplicationListener(new AppHandler());
      app2.setEnabledPreferencesMenu(false);
      app2.setEnabledAboutMenu(true);
   }



   static class AppHandler extends ApplicationAdapter {
      public void handleAbout(ApplicationEvent event) {
         event.setHandled(true);
         org.genepattern.gpge.GenePattern.showAbout();
      }
   }
}
