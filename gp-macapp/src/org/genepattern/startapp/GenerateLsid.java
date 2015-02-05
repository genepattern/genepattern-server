package org.genepattern.startapp;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by tabor on 2/5/15.
 */
public class GenerateLsid {

    public static String lsid() {
        String host_address = null;
        try {
            host_address = Inet4Address.getLocalHost().getHostAddress();

            String host_port =  "8080";
            String username = System.getProperty("user.name");

            InetAddress addr = InetAddress.getByName(host_address);

            if (isLoopbackAddress(addr) || isSiteLocalAddress(addr)){
                host_address = getNonLocalAddress(host_address);

            }

            return host_port + "." + username + "." + host_address;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "LSID.ERROR";
        }
    }

    public static boolean isLoopbackAddress(InetAddress addr) {
 		/* 127.x.x.x */
        byte[] byteAddr = addr.getAddress();
        return byteAddr[0] == 127;
    }

    public static boolean isSiteLocalAddress(InetAddress addr) {
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
     */

    public static String getNonLocalAddress(String localAddress) throws UnknownHostException {
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

    public static Properties getProperties(File propFile) {
        // Ensure the file exists
        if (!propFile.exists()) {
            try {
                propFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(propFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}
