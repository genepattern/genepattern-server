/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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

public class GetHostNameMac extends CustomCodeAction {


    /**
     * Creates a new instance of RegisterGenePattern
     */

    public GetHostNameMac() {
    }

    public String getInstallStatusMessage() {
        return "GetHostNameMac";
    }

    public String getUninstallStatusMessage() {
        return "";
    }


    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        try {


            InetAddress addr = InetAddress.getLocalHost();
            String host = addr.getHostName();
            String user = System.getProperty("user.name");
            // String host_address = addr.getCanonicalHostName();
            String host_address = addr.getHostName();
            String host_address2 = addr.getHostAddress();

            int idx = host.indexOf('.');
            if (idx > 0) host = host.substring(0, idx);

            String domain = "";
            if (host_address.startsWith(host)) {
                domain = host_address.substring(host.length() + 1);
            } else {
                domain = host_address;
            }

            ip.setVariable("HOST_NAME", host);
            ip.setVariable("USERNAME", user);
            ip.setVariable("HOST_ADDRESS", host_address);
            ip.setVariable("HOST_ADDR", host_address2);
            ip.setVariable("DOMAIN", domain);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {
        // do nothing on uninstall

    }

}
