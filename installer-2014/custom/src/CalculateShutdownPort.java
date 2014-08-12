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

package custom;


import com.zerog.ia.api.pub.*;

import java.net.*;

import java.io.*;

/**
 * custom class for calculating a shutdown port for the GP server
 * <p/>
 * expects
 * <p/>
 * GENEPATTERN_PORT = integer port for web server
 *
 * @author Liefeld
 */

public class CalculateShutdownPort extends CustomCodeAction {


    /**
     * Creates a new instance of RegisterGenePattern
     */

    public CalculateShutdownPort() {

    }


    public String getInstallStatusMessage() {

        return "CalculateShutdownPort";

    }


    public String getUninstallStatusMessage() {

        return "";

    }


    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {


        StringBuffer escapedDirs = new StringBuffer();

        String sport = (String) ip.getVariable("GENEPATTERN_PORT");

        int port = Integer.parseInt(sport);

        int shutdownPort = port - 75;

        String ssport = "" + shutdownPort;


        ip.setVariable("GENEPATTERN_SHUTDOWN_PORT", ssport);


    }


    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {

        // do nothing on uninstall


    }


}

