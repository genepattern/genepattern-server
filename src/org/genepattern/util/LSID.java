/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


package org.genepattern.util;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.StringTokenizer;

public class LSID implements Comparable<LSID>, Serializable {

    /** computed servialVersionUID */
    private static final long serialVersionUID = -1579338628906221673L;

    String authority = "";

    String namespace = "";

    String identifier = "";

    String version = "";

    protected boolean precomputed = false;

    public static final String URN = "urn";

    public static final String SCHEME = "lsid";

    public static final String DELIMITER = ":";

    public static final String VERSION_DELIMITER = ".";

    // URN encoding constants

    protected static final String encodable = "\\\"&<>[]^`{|}~%/?#";

    protected static final String escape = "%";

    protected static final String UTF8 = "utf-8";

    // sample valid LSID:
    // urn:lsid:broadinstitute.org:genepatternmodule:123:2

    public LSID(String lsid) throws MalformedURLException {
        if (lsid == null || lsid.length() == 0) {
            // empty initializer is okay, it will be set later
            return;
        }
        String usage = lsid + " must be of the form " + URN + DELIMITER
                + SCHEME + DELIMITER + "authority" + DELIMITER + "namespace"
                + DELIMITER + "identifier" + DELIMITER + "version";
        StringTokenizer stLSID = new StringTokenizer(lsid, DELIMITER);
        int numParts = stLSID.countTokens();
        if (numParts != 5 && numParts != 6) {
            throw new MalformedURLException("Wrong number of parts: " + usage);
        }
        if (!stLSID.nextToken().equals(URN)) {
            throw new MalformedURLException("Bad or missing URN: " + usage);
        }
        if (!stLSID.nextToken().equals(SCHEME)) {
            throw new MalformedURLException("Bad or missing SCHEME: " + usage);
        }
        setAuthority(decode(stLSID.nextToken()));
        setNamespace(decode(stLSID.nextToken()));
        setIdentifier(decode(stLSID.nextToken()));
        if (numParts == 6) {
            setVersion(stLSID.nextToken());
        }
    }

    public LSID(String authority, String namespace, String identifier,
            String version) throws MalformedURLException {
        setAuthority(decode(authority));
        setNamespace(decode(namespace));
        setIdentifier(decode(identifier));
        setVersion(decode(version));
    }

    public static boolean isLSID(String lsid) {
        try {
            new LSID(lsid);
            return true;
        } catch (MalformedURLException mue) {
            return false;
        }
    }

    public boolean hasVersion() {
        return !getVersion().equals("");
    }

    public boolean matchingVersion(LSID other) {
        return (getVersion().equals("") || other.getVersion().equals("") || getVersion()
                .equals(other.getVersion()));
    }

    public String toString() {
        StringBuffer lsid = new StringBuffer();
        lsid.append(URN);
        lsid.append(DELIMITER);
        lsid.append(SCHEME);
        lsid.append(DELIMITER);
        lsid.append(encode(getAuthority()));
        lsid.append(DELIMITER);
        lsid.append(encode(getNamespace()));
        lsid.append(DELIMITER);
        lsid.append(encode(getIdentifier()));
        if (!(getVersion().equals(""))) {
            lsid.append(DELIMITER);
            lsid.append(getVersion());
        }
        return lsid.toString();
    }

    public String toStringNoVersion() {
        LSID temp = copy();
        try {
            temp.setVersion("");
        } catch (MalformedURLException mue) {
            // ignore
        }
        return temp.toString();
    }

    public int compareTo(final LSID other) throws ClassCastException {
        if (!isSimilar(other)) {
            String thisStr = this.toString().toLowerCase();
            String otherStr = other.toString().toLowerCase();
            return thisStr.compareTo(otherStr);
        } else {
            // versions sort in inverse order

            // crawl version string
            StringTokenizer stThisVersion = new StringTokenizer(getVersion(),
                    VERSION_DELIMITER);
            StringTokenizer stOtherVersion = new StringTokenizer(other
                    .getVersion(), VERSION_DELIMITER);
            String thisVersionMinor;
            String otherVersionMinor;
            int thisMinor;
            int otherMinor;
            NumberFormat df = NumberFormat.getIntegerInstance();
            while (stThisVersion.hasMoreTokens()) {
                thisVersionMinor = stThisVersion.nextToken();
                if (!stOtherVersion.hasMoreTokens()) {
                    // this version has more parts than other, but was equal
                    // until now
                    // That means that it has an extra minor level and is
                    // therefore later
                    return -1;
                }
                otherVersionMinor = stOtherVersion.nextToken();
                try {
                    thisMinor = df.parse(thisVersionMinor).intValue();
                } catch (ParseException nfe) {
                    // what to do?
                    throw new ClassCastException(
                            "LSID: not a valid version number: " + getVersion());
                }
                try {
                    otherMinor = df.parse(otherVersionMinor).intValue();
                } catch (ParseException pe) {
                    // what to do?
                    throw new ClassCastException(
                            "LSID: not a valid version number: "
                                    + other.getVersion());
                }
                if (thisMinor > otherMinor) {
                    return -1;
                } else if (thisMinor < otherMinor) {
                    return 1;
                }
            }
            if (stOtherVersion.hasMoreTokens()) {
                // other version has more parts than this, but was equal until
                // now
                // That means that it has an extra minor level and is therefore
                // later
                return 1;
            }
            // completely equal!
            return 0;
        }
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object other) {
        try {
            LSID l = (LSID) other;
            return isSimilar(l) && getVersion().equals(l.getVersion());
        } catch (ClassCastException cce) {
            return false;
        }
    }

    // check if two LSIDs are equivalent. Missing versions match each other. But
    // if both are specified, they must match.
    public boolean isEquivalent(LSID other) {
        return (isSameAuthority(other) && isSameNamespace(other)
                && isSameIdentifier(other) && matchingVersion(other));
    }

    // check if two LSIDs are equivalent except for version information.
    public boolean isSimilar(LSID other) {
        return ((other != null) && isSameAuthority(other)
                && isSameNamespace(other) && isSameIdentifier(other));
    }

    public boolean isSameAuthority(LSID other) {
        return getAuthority().equals(other.getAuthority());
    }

    public boolean isSameNamespace(LSID other) {
        return getNamespace().equals(other.getNamespace());
    }

    public boolean isSameIdentifier(LSID other) {
        return getIdentifier().equals(other.getIdentifier());
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) throws MalformedURLException {
        if (authority == null || authority.equals("")) {
            throw new MalformedURLException("Bad or missing authority");
        }
        this.authority = authority;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) throws MalformedURLException {
        if (namespace == null || namespace.equals("")) {
            throw new MalformedURLException("Bad or missing namespace");
        }
        this.namespace = namespace;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) throws MalformedURLException {
        if (identifier == null || identifier.equals("")) {
            throw new MalformedURLException("Bad or missing identifier");
        }
        this.identifier = identifier;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) throws MalformedURLException {
        if (version == null)
            version = "";
        StringTokenizer stVersion = new StringTokenizer(version,
                VERSION_DELIMITER);
        String versionMinor;
        while (stVersion.hasMoreTokens()) {
            versionMinor = stVersion.nextToken();
            try {
                /* int minor = */ 
                Integer.parseInt(versionMinor);
            } catch (NumberFormatException nfe) {
                throw new MalformedURLException("Invalid LSID version in "
                        + toString() + DELIMITER + version);
            }
        }
        this.version = version;

    }

    // do whatever URI encoding is required (RFC 2141:
    // http://www.faqs.org/rfcs/rfc2141.html)
    protected String encode(String original) {
        if (original == null)
            original = "";
        try {
            return URLEncoder.encode(original, UTF8);
        } catch (UnsupportedEncodingException uce) {
            return null;
        }
    }

    protected String decode(String original) {
        if (original == null)
            original = "";
        try {
            return URLDecoder.decode(original, UTF8);
        } catch (UnsupportedEncodingException uce) {
            return null;
        }
    }

    public LSID copy() {
        LSID lsid = null;
        try {
            lsid = new LSID(getAuthority(), getNamespace(), getIdentifier(),
                    getVersion());
        } catch (MalformedURLException mue) {
        }
        return lsid;
    }

    public String getIncrementedMinorVersion() {
        return LSID.getIncrementedMinorVersion(this.version);
    }
    
    public static String getIncrementedMinorVersion(final String version) {
        StringTokenizer stVersion = new StringTokenizer(version,
                VERSION_DELIMITER);
        String versionMinor = "";
        while (stVersion.hasMoreTokens()) {
            versionMinor = stVersion.nextToken();
        }
        int minor = 0;
        try {
            minor = Integer.parseInt(versionMinor);
            minor++;
        } catch (NumberFormatException nfe) {
            System.err.println(version
                    + " doesn't end in an integer minor number");
        }
        return version.substring(0, version.length() - versionMinor.length())
                + Integer.toString(minor);
    }

}
