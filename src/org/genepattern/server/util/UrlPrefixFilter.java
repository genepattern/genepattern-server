/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.webservice.ParameterInfo;

/**
 * Filter external url based on a server configured list of external url prefixes (aka remote directories).
 * 
 * <p>
 * For example, 
 * <pre>
 * # match all files in the Broad hosted FTP server
   cache.externalUrlDirs: [ "ftp://gpftp.broadinstitute.org/" ]
 * </pre>
 * 
 * @author pcarr
 */
public class UrlPrefixFilter {
    private static final Logger log = Logger.getLogger(UrlPrefixFilter.class);

    /**
     * Set 'cache.externalUrlDirs' to a list of zero or more remote directories;
     * matching file input values will be cached in the GP server file system.
     * 
     * Examples,
     * <pre>
     # accept all
     cache.externalUrlDirs: [ "*" ]
     # accept none
     cache.externalUrlDirs: [ "!*" ]
     # cache all files from gpftp server
     cache.externalUrlDirs: [ "ftp://gpftp.broadinstitute.org/" ]
     * </pre>
     */
    public static final String PROP_CACHE_EXTERNAL_URL = "cache.externalUrlDirs";

    /**
     * Get the server configured UrlPrefixFilter. Any externalUrl accepted by the filter
     * should be cached.
     * 
     * @param gpConfig
     * @param gpContext
     * @return
     */
    public static UrlPrefixFilter initCacheExternalUrlDirsFromConfig(final GpConfig gpConfig, final GpContext gpContext) {
        if (gpConfig==null) {
            log.error("gpConfig==null; return null");
            return null;
        }
        Value value=gpConfig.getValue(gpContext, PROP_CACHE_EXTERNAL_URL);
        if (value==null) {
            return null;
        }
        UrlPrefixFilter filter=new UrlPrefixFilter();
        for(final String val : value.getValues()) {
            filter.addUrlPrefix(val);
        }
        return filter;
    }

    /**
     * Get the optional UrlPrefixFilter for the given input parameter; This can be null.
     * 
     * @param formal
     * @return
     */
    public static UrlPrefixFilter initDropDownFilter(final ParameterInfo formal) {
        final boolean initDropdown=false;
        final ChoiceInfo dropDown=ChoiceInfoHelper.initChoiceInfo(formal, initDropdown);
        UrlPrefixFilter dropDownFilter=null;
        if (dropDown != null && dropDown.getChoiceDir() != null && dropDown.getChoiceDir().length()>0) {
            dropDownFilter=new UrlPrefixFilter();
            dropDownFilter.addUrlPrefix(dropDown.getChoiceDir());
        }
        return dropDownFilter;
    }
    
    /**
     * Is the given externalUrl accepted by the list of zero or more filters?
     * 
     * @param externalUrl
     * @param filters
     * @return
     */
    public static boolean accept(final String externalUrl, UrlPrefixFilter... filters) {
        if (filters==null || filters.length==0) {
            return false;
        }
        for(final UrlPrefixFilter filter : filters) {
            if (filter != null) {
                if (filter.accept(externalUrl)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> urlPrefixes=null;
    private boolean acceptAll=false;

    protected void reset() {
        urlPrefixes=null;
        acceptAll=false;
    }

    /**
     * Add a url prefix to the filter. 
     * Special case, "*" means accept all externalUrl;
     * Special case, "!*" means ignore all externalUrl;
     * Recommended to append a 'directory', e.g.
     *     "ftp://gpftp.broadinstitute.org/"
     * @param urlPrefix
     */
    public void addUrlPrefix(String urlPrefix) {
        if (urlPrefix==null) {
            log.error("urlPrefix==null; ignoring");
            return;
        }
        // special-case, "*" means all
        if ("*".equals(urlPrefix)) {
            this.acceptAll=true;
            return;
        }
        // special-case, "!*" means none
        else if ("!*".equals(urlPrefix)) {
            this.acceptAll=false;
            return;
        }
        
        if (urlPrefixes==null) {
            // lazy-init 
            urlPrefixes=new ArrayList<String>();
        }
        // map all entries as lower case strings
        urlPrefixes.add(urlPrefix.toLowerCase());
    }
    
    protected void setAcceptAll(final boolean acceptAll) {
        this.acceptAll=acceptAll;
    }

    protected boolean getAcceptAll() {
        return acceptAll;
    }

    public boolean accept(final String externalUrl) {
        if (acceptAll) {
            return true;
        }
        if (urlPrefixes==null) {
            return false;
        }
        for(final String urlPrefix : urlPrefixes) {
            if (externalUrl.toLowerCase().startsWith(urlPrefix)) {
                return true;
            }
        }
        return false;
    }

}
