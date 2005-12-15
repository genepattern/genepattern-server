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


/*
 * RegisterGenePattern.java
 *
 * Created on January 14, 2004, 6:54 AM
 */
package edu.mit.broad.genepattern.installer;

import com.zerog.ia.api.pub.*;
import java.net.*;
import java.io.*;
/**
 * custom class for registering GenePattern from a remote client installation
 * It expects to find IA variables with the following names;
 * GP_REG_NAME = name
 * GP_REG_EMAIL = email address
 * GP_REG_JOIN_GPUSERS = t/f jopoin the email to the gp-users mailing list
 * GP_REG_INSTITUTE = institution
 * GP_REG_ADDRESS = address
 *
 * GP_REG_REGISTRATION_URL = URL to register the information to
 * @author  Liefeld
 */
public class RegisterStringUtil extends CustomCodeAction {
    
    
    /** Creates a new instance of RegisterGenePattern */
    public RegisterStringUtil() {
    }
    
    public String getInstallStatusMessage() {
        return "RegisterStringUtil";
    }
    
    public String getUninstallStatusMessage() {
        return "";
    }
    
    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
   
       
   
        StringBuffer escapedDirs = new StringBuffer();
        String dirs = (String)ip.getVariable( "gp.project.dirs" );
        if (dirs == null) return;

         System.out.println("FOUND:"+dirs);
        for (int i=0; i < dirs.length(); i++){
             String next = "" + dirs.charAt(i);
            
            if (":".equals(next)) escapedDirs.append( '\\' );
            
            if ("\\".equals(next)) {
                escapedDirs.append('\\');
            }
            escapedDirs.append(next);
            
        }
        
         System.out.println("LEFT:"+escapedDirs.toString());
        ip.setVariable("gp.project.dirs",  escapedDirs.toString());

    }
    
    
    
    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {
    // do nothing on uninstall
    
    }
    
}
