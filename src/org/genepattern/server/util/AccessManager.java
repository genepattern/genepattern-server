/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jfree.util.Log;

/**
 * @author Liefeld
 * 
 */
public class AccessManager {
    protected static String clientList = "";
    protected static String blacklistedClientList = "";

    protected static List<String> allowedClients = null;
    protected static List<BlacklistChecker> blacklistedClients = null;
    
    /**
     * 
     */
    public static boolean isAllowed(String host, String address) {
        boolean allowed = false;
        List<String> okClients = getAllowedClients();

        // white list of allowed trumps the blacklist so check it first
        if (okClients != null) {
            for (int i = 0; i < okClients.size(); i++) {
                String validClient = (String) okClients.get(i);
                if (host.indexOf(validClient) >= 0)
                    return true;
                if (address.indexOf(validClient) >= 0)
                    return true;
            }
            return false;
        }
        // no whitelist specified so now check the blacklist
        return !isBlacklisted(host, address);
    }

    
    public static boolean isBlacklisted(String host, String address) {

        List<BlacklistChecker> badClients = getBlacklistedClients();

        if (badClients != null) {
            for (int i = 0; i < badClients.size(); i++) {
                BlacklistChecker badClientChecker = (BlacklistChecker) badClients.get(i);
                
                 if (badClientChecker.matches(address)) {
                     Log.info("Blacklist - rejecting connection from " + address + " matches " + badClientChecker.toString());
                     return true;
                 } else {
                     Log.debug("Blacklist - allow connection " + address + " with " + badClientChecker.toString());
                 }
             }
            return false;
        }
        return false;
    }
    
    protected static List<BlacklistChecker> getBlacklistedClients() {
        String badClientList = System.getProperty("gp.blacklisted.clients");

        // refresh on first time through or if something has changed since last
        // time
        //
        boolean refresh = (blacklistedClients == null);
        if ((blacklistedClientList == null) && (badClientList != null))
            refresh = true;
        else if ((blacklistedClientList != null) && (badClientList == null))
            refresh = true;
        else {

            if ((blacklistedClientList == null) && (badClientList == null))
                refresh = true;
            else if (!(blacklistedClientList.trim().equals(badClientList.trim())))
                refresh = true;
        }
        if (refresh) {
            blacklistedClientList= System.getProperty("gp.blacklisted.clients");
            if (blacklistedClientList != null) {
                blacklistedClients = new ArrayList<BlacklistChecker>();
                // replace any newlines from the editor with spaces
                blacklistedClientList = blacklistedClientList.replace("\n", " ").replace("\r", " ");
                StringTokenizer strtok = new StringTokenizer(blacklistedClientList, " ");
                while (strtok.hasMoreTokens()) {
                    String tok = strtok.nextToken();
                    try {
                         blacklistedClients.add(new AccessManager.BlacklistChecker(tok));
                    } catch (Exception e){
                        Log.error("Could not initalize BlacklistChecker for: " + tok);
                    }
                }            
            }
        }
        return blacklistedClients;
        
    }
    
    protected static List<String> getAllowedClients() {
        String allowedClientList = System.getProperty("gp.allowed.clients");

        // refresh on first time through or if something has changed since last
        // time
        //
        boolean refresh = (allowedClients == null);
        if ((clientList == null) && (allowedClientList != null))
            refresh = true;
        else if ((clientList != null) && (allowedClientList == null))
            refresh = true;
        else {

            if ((clientList == null) && (allowedClientList == null))
                refresh = true;
            else if (!(clientList.trim().equals(allowedClientList.trim())))
                refresh = true;
        }
        if (refresh) {
            clientList = System.getProperty("gp.allowed.clients");
            if ((clientList != null) && (!(clientList.trim().equals("Any")))) {
                allowedClients = new ArrayList<String>();
                StringTokenizer strtok = new StringTokenizer(clientList, ",");
                while (strtok.hasMoreTokens()) {
                    String tok = strtok.nextToken();
                    allowedClients.add(tok);
                }
                allowedClients.add("127.0.0.1");// so that you can always get in
                // locally
                allowedClients.add("localhost");// so that you can always get in
                // locally

                try {
                    InetAddress addr = InetAddress.getLocalHost();
                    String host_address = addr.getCanonicalHostName();
                    String host_address2 = addr.getHostAddress();
                    allowedClients.add(host_address);// so that you can
                    // always
                    // get in locally
                    allowedClients.add(host_address2);// so that you can
                    // always
                    // get in locally

                } catch (UnknownHostException uke) {
                    // do nothing
                }

            } else {// null indicates allow anyone
                allowedClients = null;
            }
        }
        return allowedClients;
    }
    
    static class BlacklistChecker {
        private final int maskSize;
        private final InetAddress matchAddress;
        private final String matchString;
        
        public BlacklistChecker(String addressToMatch) throws Exception {
            matchString = addressToMatch;  // just for the to-string method
            // pull the netmask if present
            if (addressToMatch.indexOf('/') > 0) {
                String[] addressAndMask = addressToMatch.split("/");
                addressToMatch = addressAndMask[0];
                maskSize = Integer.parseInt(addressAndMask[1]);
            } else {
                maskSize = -1;
            }
            matchAddress = InetAddress.getByName(addressToMatch);
            assert  (matchAddress.getAddress().length * 8 >= maskSize) :
                    String.format("IP address %s is too short for bitmask of length %d",
                            addressToMatch, maskSize);
        }

        public boolean matches(String address) {
            try {
                InetAddress remoteAddress = InetAddress.getByName(address);
                byte[] matchAddressBytes = matchAddress.getAddress();
                   
                if (maskSize < 0) {
                    // no mask just a straigh forward string compare
                    return remoteAddress.equals(matchAddress);
                }
                
                // need to replicate the net mask
                byte[] remoteAddressBytes = remoteAddress.getAddress();
                int masksizeInBytes = maskSize / 8;
                byte finalByte = (byte) (0xFF00 >> (maskSize & 0x07));
                for (int i = 0; i < masksizeInBytes; i++) {
                    if (remoteAddressBytes[i] != matchAddressBytes[i]) {
                        return false;
                    }
                }
                if (finalByte != 0) {
                    return (remoteAddressBytes[masksizeInBytes] & finalByte) == (matchAddressBytes[masksizeInBytes] & finalByte);
                }
                
            } catch (Exception e){
                return true;
            }
            return true;
        }
        
        public String toString(){
            return "AccessManager.BlacklistChecker(" + matchString +")";
        }
    }

}





