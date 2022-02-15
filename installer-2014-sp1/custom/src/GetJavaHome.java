/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/


/*

 * 
 *

 * Created on January 14, 2004, 6:54 AM

 */

package custom;


import com.zerog.ia.api.pub.*;

import java.net.*;

import java.io.*;

/**
 * custom class for calculating a shutdown port for the GP server
 * expects
 * GENEPATTERN_PORT = integer port for web server
 *
 * @author Liefeld
 */

public class GetJavaHome extends CustomCodeAction {


    /**
     * Creates a new instance of RegisterGenePattern
     */

    public GetJavaHome() {
    }

    public String getInstallStatusMessage() {
        return "GetJavaHome";
    }

    public String getUninstallStatusMessage() {
        return "";
    }


    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        try {


            // set the java dir for case where VM not installed
            ip.setVariable("JAVA_HOME", System.getProperty("java.home"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {
        // do nothing on uninstall

    }

}
