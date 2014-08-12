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

public class GetHostName extends CustomCodeAction {


    /**
     * Creates a new instance of RegisterGenePattern
     */

    public GetHostName() {
    }

    public String getInstallStatusMessage() {
        return "GetHostName ";
    }

    public String getUninstallStatusMessage() {
        return "";
    }


    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String host = addr.getHostName();
            String user = System.getProperty("user.name");
            String host_address = addr.getCanonicalHostName();
            //String host_address = addr.getHostName();
            String host_address2 = addr.getHostAddress();
            String domain = "";

            /**
             * Deal with platform differences.
             * mac includes hostname in domain
             * linux hostname = domain
             * pc host and address are not overlapping
             */
            if (host_address.equals(host)) {
                int idx = host.indexOf(".");

                if (idx >= 0) {
                    domain = host.substring(idx + 1);
                    host = host.substring(0, idx);
                }
            } else if (host_address.startsWith(host)) {
                domain = host_address.substring(host.length() + 1);
            } else {
                domain = host_address;
            }

            System.out.println("host=" + host);
            System.out.println("user=" + user);
            System.out.println("host_address=" + host_address);
            System.out.println("host_address2=" + host_address2);


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
