package org.genepattern.gpge.util;

import java.io.*;
import java.net.*;
/**
 *  Utilities for Mac OS X specific functions
 *
 * @author    Joshua Gould
 */
public class MacOS {
   private MacOS() { }


   /**
    *  Selects the file specified by <tt>file</tt> in a new file viewer is
    *  opened. Returns true if the file is successfully selected, false
    *  otherwise.
    *
    * @param  file  the file to open
    * @return       <tt>true</tt> if the file is opened successfully, <tt>false
    *      </tt> otherwise
    */
   public static boolean showFileInFinder(File file) {
      try {
         if(file.exists()) {
            Class NSWorkspaceClass = null;
            if(new File("/System/Library/Java/com/apple/cocoa/application/NSWorkspace.class").exists()) {
               ClassLoader classLoader = new URLClassLoader(new URL[]{new File("/System/Library/Java").toURL()});
               NSWorkspaceClass = Class.forName("com.apple.cocoa.application.NSWorkspace", true, classLoader);
            } else {
               NSWorkspaceClass = Class.forName("com.apple.cocoa.application.NSWorkspace");
            }

            java.lang.reflect.Method sharedWorkspaceMethod = NSWorkspaceClass.getMethod("sharedWorkspace",
                  null);
            Object NSWorkspace = sharedWorkspaceMethod.invoke(null, null);

            java.lang.reflect.Method selectFileMethod = NSWorkspace.getClass().getMethod("selectFile",
                  new Class[]{String.class, String.class});

            String path = file.getCanonicalPath();
            
            Object opened = selectFileMethod.invoke(NSWorkspace, new Object[]{path, path});
            if(opened instanceof Boolean) {
               return ((Boolean) opened).booleanValue();
            }
         }
         return false;
      } catch(Throwable t) {
         return false;
      }
   }
}
