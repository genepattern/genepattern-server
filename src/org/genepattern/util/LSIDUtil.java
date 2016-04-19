/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.util;

import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * @author Liefeld
 * 
 * 
 */
public class LSIDUtil {
    private static final Logger log = Logger.getLogger(LSIDUtil.class);

    public static final String AUTHORITY_MINE = "mine";
    public static final String AUTHORITY_BROAD = "broad";
    public static final String AUTHORITY_FOREIGN = "foreign";
    public static final String BROAD_AUTHORITY = "broad.mit.edu";

    private static LSIDUtil inst = null;
    private static final String SUITE_NAMESPACE_INCLUDE = "suite";

    private LSIDUtil() {
    }

    /** @deprecated replace use of this singleton with calls to static methods in the class or calls to the GpConfig class  */
    public static LSIDUtil getInstance() {
        if (inst == null) {
            inst = new LSIDUtil();
        }
        return inst;
    }

    /** @deprecated call {@link GpConfig#getLsidAuthority(GpContext)} instead. */
    public String getAuthority() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext gpContext=GpContext.getServerContext();
        return gpConfig.getLsidAuthority(gpContext);
    }
    
    /** @deprecated call static {@link #getAuthorityType(GpConfig, GpContext, LSID)} instead  */
    public String getAuthorityType(final LSID lsid) {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext gpContext=GpContext.getServerContext();
        return getAuthorityType(gpConfig, gpContext, lsid);
    }

    public static String getAuthorityType(final GpConfig gpConfig, final GpContext gpContext, final LSID lsid) {
        final String authorityMine=gpConfig.getLsidAuthority(gpContext);
        return getAuthorityType(authorityMine, lsid);
    }

    /**
     * Get the authority type for the given lsid.
     * One of: MINE | BROAD | FOREIGN
     * 
     * @param authorityMine the 'lsid.authority' from {@link GpConfig#getLsidAuthority(GpContext)}
     * @param lsid
     * @return
     */
    public static String getAuthorityType(final String authorityMine, final LSID lsid) {
        String authorityType;
        if (lsid == null) {
            authorityType = AUTHORITY_MINE;
        } 
        else {
            final String lsidAuthority = lsid.getAuthority();
            if (lsidAuthority.equals(authorityMine)) {
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

    /** @deprecated call static {@link #compareAuthorities(String, LSID, LSID)} instead  */
    public int compareAuthorities(final LSID lsid1, final LSID lsid2) {
        return compareAuthorities(getAuthority(), lsid1, lsid2);
    }

    /**
     * Compare authority types: 1=lsid1 is closer, 0=equal, -1=lsid2 is closer
     * closer is defined as mine > Broad > foreign
     * 
     * @param authorityMine, the server 'lsid.authority'
     * @param lsid1
     * @param lsid2
     * @return
     */
    public static int compareAuthorities(final String authorityMine, final LSID lsid1, final LSID lsid2) {
        final String at1 = getAuthorityType(authorityMine, lsid1);
        final String at2 = getAuthorityType(authorityMine, lsid2);
        if (!at1.equals(at2)) {
            if (at1.equals(AUTHORITY_MINE)) {
                return 1;
            }
            if (at2.equals(AUTHORITY_MINE)) {
                return -1;
            }
            if (at1.equals(AUTHORITY_BROAD)) {
                return 1;
            }
            return -1;
        } 
        else {
            return 0;
        }
    }

    /**
     * Convenience method. Returns true if an lsid's authority == "MINE".
     * 
     * @param lsid
     */
    public static boolean isAuthorityMine(final GpConfig gpConfig, final GpContext gpContext, final LSID lsid) {
        final String authType = getAuthorityType(gpConfig, gpContext, lsid);
        return AUTHORITY_MINE.equals(authType);
    }

    /** @deprecated call static {@link #isAuthorityMine(GpConfig, GpContext, String)} instead  */
    public boolean isAuthorityMine(final String lsid) {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext gpContext=GpContext.getServerContext();
        return isAuthorityMine(gpConfig, gpContext, lsid);
    }

    public static boolean isAuthorityMine(final GpConfig gpConfig, final GpContext gpContext, final String lsid) {
        LSID lsidObj;
        try {
            lsidObj = new LSID(lsid);
            return isAuthorityMine(gpConfig, gpContext, lsidObj);
        } 
        catch (MalformedURLException e) {
            log.error(e);
            return false;
        }
    }

    /** @deprecated call static {@link #getNearerLSID(String, LSID, LSID)} instead */
    public LSID getNearerLSID(final LSID lsid1, final LSID lsid2) {
        final String authorityMine=getAuthority();
        return getNearerLSID(authorityMine, lsid1, lsid2);
    }
    
    public static LSID getNearerLSID(final String authorityMine, final LSID lsid1, final LSID lsid2) {
        final int authorityComparison = compareAuthorities(authorityMine, lsid1, lsid2);
        if (authorityComparison < 0)
            return lsid2;
        if (authorityComparison > 0) {
            // closer authority than lsid2.getAuthority()
            return lsid1;
        }
        // same authority, check identifier
        final int identifierComparison = lsid1.getIdentifier().compareTo(lsid2.getIdentifier());
        if (identifierComparison < 0)
            return lsid2;
        if (identifierComparison > 0) {
            // greater identifier than lsid2.getIdentifier()
            return lsid1;
        }
        // same authority and identifier, check version
        final int versionComparison = lsid1.compareTo(lsid2);
        if (versionComparison < 0) {
            return lsid2;
        }
        if (versionComparison > 0) {
            // later version than lsid2.getVersion()
            return lsid1;
        }
        return lsid1; // equal???
    }

    public static boolean isSuiteLSID(final String lsid) {
        try {
            final LSID anLsid = new LSID(lsid);
            return isSuiteLSID(anLsid);
        } 
        catch (Exception e) {
            log.error(e);
            return false;
        }
    }

    public static boolean isSuiteLSID(final LSID lsid) {
        final String nom = lsid.getNamespace();
        return (nom.indexOf(SUITE_NAMESPACE_INCLUDE)) >= 0;
    }

}
