/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.startapp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Class for correctly generating the LSID authority in the Mac app
 *
 * Much of this code was copies from the old LSID code called by InstallAnywhere.
 *
 * @author Thorin Tabor
 */
public class GenerateLsid {

    /**
     * Generate the LSID
     * @return return the LSID
     */
    public static String lsid() { 
        try {
            final String hostAddress;
            final InetAddress localHost=InetAddress.getLocalHost();
            if (localHost.isLoopbackAddress() || localHost.isSiteLocalAddress()) {
                hostAddress = localHost.getHostName() + "." + localHost.getHostAddress();
            }
            else {
                hostAddress = localHost.getHostAddress();
            }
            final String hostPort =  "8080";
            final String username = System.getProperty("user.name");
            return hostPort + "." + username + "." + hostAddress;
        } 
        catch (Throwable t) {
            t.printStackTrace();
            return "8080."+System.getProperty("user.name")+".127.0.0.1";
        }
    }

    /**
     * Check if the address is a loopback address
     * @param addr
     * @return
     */
    public static boolean isLoopbackAddress(InetAddress addr) {
 		/* 127.x.x.x */
        byte[] byteAddr = addr.getAddress();
        return byteAddr[0] == 127;
    }

    /**
     * Check if the address is a local address
     * @param addr
     * @return
     */
    private static boolean isSiteLocalAddress(InetAddress addr) {
        // refer to RFC 1918
        // 10/8 prefix
        // 172.16/12 prefix
        // 192.168/16 prefix
        byte[] addressBytes = addr.getAddress();
        int address;
        address  = addressBytes [3] & 0xFF;
        address |= ((addressBytes [2] << 8) & 0xFF00);
        address |= ((addressBytes [1] << 16) & 0xFF0000);
        address |= ((addressBytes [0] << 24) & 0xFF000000);

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
     * 
     * @deprecated shouldn't call this, it expects the server to be running
     */
    private static String getNonLocalAddress(String localAddress) throws UnknownHostException {
        String hostname = InetAddress.getLocalHost().getHostName();

        try {
            String serverUrl = "http://" + localAddress + ":8080";

            URL url = new URL(serverUrl);

            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            // expect hostname|ipaddress
            int idx = line.indexOf('|');
            String name = line.substring(0, idx);
            String ipaddr = line.substring(idx+1);

            // check


            if (name.length() > 0){
                // we may be able to get useful domain from the name...
                int domidx = name.indexOf('.');
                String domain = name.substring(domidx);
                return hostname + domain;

            } else if (ipaddr.length() > 0) {
                InetAddress addr = InetAddress.getByName(ipaddr);
                if (!(isLoopbackAddress(addr) || isSiteLocalAddress(addr))){
                    return ipaddr ;
                }
            }
        } catch (Exception e){
            // the server is down or returned garbage.  Add a UID to the
            // local address
        }

        return hostname + "." +  localAddress;

    }
}
