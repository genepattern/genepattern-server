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
import java.rmi.server.UID;
import java.io.*;

/**
 * custom class for calculating a LSID authority
 * expects
 * GENEPATTERN_PORT = integer port for web server
 * HOST_ADDRESS = integer port for web server
 *
 * @author Liefeld
 */

public class GetLsidAuthority extends CustomCodeAction {


    /**
     * Creates a new instance of RegisterGenePattern
     */

    public GetLsidAuthority() {
    }

    public String getInstallStatusMessage() {
        return "GetLsidAuthority ";
    }

    public String getUninstallStatusMessage() {
        return "";
    }


    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        try {
            String host_address = (String) ip.getVariable("HOST_ADDRESS");
            String host_port = (String) ip.getVariable("GENEPATTERN_PORT");
            String old_auth = (String) ip.getVariable("lsid.authority");
            String username = System.getProperty("user.name");

            if (old_auth != null) {
                ip.setVariable("LSID_AUTHORITY", old_auth);

            } else {


                InetAddress addr = InetAddress.getByName(host_address);

                if (isLoopbackAddress(addr) || isSiteLocalAddress(addr)) {
                    host_address = getNonLocalAddress(ip, host_address);

                }

                ip.setVariable("LSID_AUTHORITY", host_port + "." + username + "." + host_address);

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean isLoopbackAddress(InetAddress addr) {
         /* 127.x.x.x */
        byte[] byteAddr = addr.getAddress();
        return byteAddr[0] == 127;
    }

    public boolean isSiteLocalAddress(InetAddress addr) {
        // refer to RFC 1918
        // 10/8 prefix
        // 172.16/12 prefix
        // 192.168/16 prefix
        byte[] addressBytes = addr.getAddress();
        int address;
        address = addressBytes[3] & 0xFF;
        address |= ((addressBytes[2] << 8) & 0xFF00);
        address |= ((addressBytes[1] << 16) & 0xFF0000);
        address |= ((addressBytes[0] << 24) & 0xFF000000);

        return (((address >>> 24) & 0xFF) == 10)
                || ((((address >>> 24) & 0xFF) == 172)
                && (((address >>> 16) & 0xF0) == 16))
                || ((((address >>> 24) & 0xFF) == 192)
                && (((address >>> 16) & 0xFF) == 168));
    }


    /**
     * if the address is a loopback or local address, make it unique. First try to
     * get a real IP by connecting the URL and looking at what it sees as the real
     * IP address, then if that fails, simply create a UID and append it to the
     * non-unique address to make it unique
     */

    public String getNonLocalAddress(com.zerog.ia.api.pub.InstallerProxy ip, String localAddress) {
        String hostname = (String) ip.getVariable("HOST_NAME");


        try {
            String serverUrl = (String) ip.getVariable("CHECK_IP_ADDRESS_URL");
            String major = (String) ip.getVariable("version.major");
            String minor = (String) ip.getVariable("version.minor");
            serverUrl = serverUrl + "?version=" + major + "." + minor;


            URL url = new URL(serverUrl);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            // expect hostname|ipaddress
            int idx = line.indexOf('|');
            String name = line.substring(0, idx);
            String ipaddr = line.substring(idx + 1);

            // check


            if (name.length() > 0) {
                // we may be able to get useful domain from the name...
                int domidx = name.indexOf('.');
                String domain = name.substring(domidx);
                return hostname + domain;

            } else if (ipaddr.length() > 0) {
                InetAddress addr = InetAddress.getByName(ipaddr);
                if (!(isLoopbackAddress(addr) || isSiteLocalAddress(addr))) {
                    return ipaddr;
                }
            }
        } catch (Exception e) {
            // the server is down or returned garbage.  Add a UID to the
            // local address
        }

        return hostname + "." + localAddress;

    }


    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {
        // do nothing on uninstall

    }

}
