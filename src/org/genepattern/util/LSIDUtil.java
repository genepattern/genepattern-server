/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.util;

import static org.genepattern.util.GPConstants.TASK_NAMESPACE;
import static org.genepattern.util.GPConstants.SUITE_NAMESPACE;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;

/**
 * @author Liefeld
 * 
 * 
 */
public class LSIDUtil {

    private static Logger log = Logger.getLogger(LSIDUtil.class);

    public static String AUTHORITY_MINE = "mine";

    public static String AUTHORITY_BROAD = "broad";

    public static String AUTHORITY_FOREIGN = "foreign";

    public static String BROAD_AUTHORITY = "broad.mit.edu";

    private static LSIDUtil inst = null;

    private static String authority = "broad-cancer-genomics";

    private static String SUITE_NAMESPACE_INCLUDE = "suite";

    private LSIDUtil() {
        String auth = System.getProperty("lsid.authority");
        if (auth != null) {
            authority = auth;
        }
    }

    public static LSIDUtil getInstance() {
        if (inst == null) {
            inst = new LSIDUtil();
        }
        return inst;
    }

    public String getAuthority() {
        return authority;
    }

    public String getTaskNamespace() {
        return TASK_NAMESPACE;
    }

    public String getSuiteNamespace() {
        return SUITE_NAMESPACE;
    }

    public String getAuthorityType(LSID lsid) {
        String authorityType;
        if (lsid == null) {
            authorityType = AUTHORITY_MINE;
        } else {
            String lsidAuthority = lsid.getAuthority();
            if (lsidAuthority.equals(authority)) {
                authorityType = AUTHORITY_MINE;
            } 
            else if (lsidAuthority.equals(BROAD_AUTHORITY)) {
                authorityType = AUTHORITY_BROAD;
            } 
            else if (lsidAuthority.equals("broadinstitute.org")) {
                authorityType = AUTHORITY_BROAD;
            }
            else {
                authorityType = AUTHORITY_FOREIGN;
            }
        }
        return authorityType;
    }

    // compare authority types: 1=lsid1 is closer, 0=equal, -1=lsid2 is closer
    // closer is defined as mine > Broad > foreign
    public int compareAuthorities(LSID lsid1, LSID lsid2) {
        String at1 = getAuthorityType(lsid1);
        String at2 = getAuthorityType(lsid2);
        if (!at1.equals(at2)) {
            if (at1.equals(AUTHORITY_MINE))
                return 1;
            if (at2.equals(AUTHORITY_MINE))
                return -1;
            if (at1.equals(AUTHORITY_BROAD))
                return 1;
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Convenience method. Returns true if an lsid's authority == "MINE".
     * 
     * @param lsid
     */
    public boolean isAuthorityMine(LSID lsid) {
        String authType = getAuthorityType(lsid);
        return AUTHORITY_MINE.equals(authType);
    }

    public boolean isAuthorityMine(String lsid) {
        LSID lsidObj;
        try {
            lsidObj = new LSID(lsid);
            return isAuthorityMine(lsidObj);
        } catch (MalformedURLException e) {
            log.error(e);
            return false;
        }

    }

    public LSID getNearerLSID(LSID lsid1, LSID lsid2) {
        int authorityComparison = compareAuthorities(lsid1, lsid2);
        if (authorityComparison < 0)
            return lsid2;
        if (authorityComparison > 0) {
            // closer authority than lsid2.getAuthority()
            return lsid1;
        }
        // same authority, check identifier
        int identifierComparison = lsid1.getIdentifier().compareTo(lsid2.getIdentifier());
        if (identifierComparison < 0)
            return lsid2;
        if (identifierComparison > 0) {
            // greater identifier than lsid2.getIdentifier()
            return lsid1;
        }
        // same authority and identifier, check version
        int versionComparison = lsid1.compareTo(lsid2);
        if (versionComparison < 0)
            return lsid2;
        if (versionComparison > 0) {
            // later version than lsid2.getVersion()
            return lsid1;
        }
        return lsid1; // equal???
    }

    public static boolean isSuiteLSID(String lsid) {

        try {
            LSID anLsid = new LSID(lsid);
            String nom = anLsid.getNamespace();
            return isSuiteLSID(anLsid);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isSuiteLSID(LSID lsid) {
        String nom = lsid.getNamespace();

        return (nom.indexOf(SUITE_NAMESPACE_INCLUDE)) >= 0;
    }

}
