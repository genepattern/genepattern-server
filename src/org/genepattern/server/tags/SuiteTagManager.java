/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.tags;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.server.webservice.server.dao.AdminDAOSysException;
import org.genepattern.util.LSID;
import org.genepattern.webservice.SuiteInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Singleton class for managing tags in GenePattern
 * @author tabor
 */
public class SuiteTagManager {
    final static private Logger log = Logger.getLogger(SuiteTagManager.class);
    private static SuiteTagManager singleton = null;
    
    public static SuiteTagManager instance() {
        if (singleton == null) {
            singleton = new SuiteTagManager();
        }
        return singleton;
    }
    
    private Map<String, Date> userCacheMap = new ConcurrentHashMap<String, Date>();                     // User to cache update date
    private Map<TagCacheKey, Set<String>> tagMap = new ConcurrentHashMap<TagCacheKey, Set<String>>();         // User+lsid to set of tags
    
    public Set<String> getSuites(GpContext context, TaskInfo taskInfo) {
        
        // Lazily update the tag cache for each TaskInfo
        if (needsUpdate(context)) {
            updateCache(context);
        }
        
        // Protect against null and return
        String baseLsid = getBaseLsid(taskInfo.getLsid());
        TagCacheKey key = new TagCacheKey(context.getUserId(), baseLsid);
        Set<String> tagSet = tagMap.get(key);
        if (tagSet == null) return new HashSet<String>();
        else return tagSet; 
    }
    
    private String getBaseLsid(String lsid) {
        try {
            return new LSID(lsid).toStringNoVersion();
        }
        catch (MalformedURLException e) {
            log.error("Problem getting base lsid: " + lsid);
            return lsid;
        }
    }
    
    /**
     * Determine whether the cached tags for the TaskInfo need to be updated
     * Rule: Update them if they haven't been loaded or if more than 5 minutes old
     * @param context
     * @return
     */
    private boolean needsUpdate(GpContext context) {
        Date now = new Date();
        Date updated = userCacheMap.get(context.getUserId());
        if (updated == null) return true;

        if (updated.before(new Date(now.getTime() - 300000 ))) return true;
        else return false;
    }
    
    /**
     * Update the tag cache and the map of updated dates
     * @param context
     */
    private void updateCache(GpContext context) {
        // Remove old tags
        removeOld(context);
        
        // Add suites for the user
        addSuites(context);
        
        // Update the user's update time
        userCacheMap.put(context.getUserId(), new Date());
    }
    
    private void removeOld(GpContext context) {
        for (TagCacheKey key : tagMap.keySet()) {
            if (key.getUser().equals(context.getUserId())) {
                tagMap.remove(key);
            }
        }
    }
    
    private void addSuites(final GpContext context) {
        if (context==null) {
            throw new IllegalArgumentException("context==null");
        }
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            final AdminDAO adminDao = new AdminDAO();
            final SuiteInfo[] suites = adminDao.getLatestSuitesForUser(context.getUserId());
            for (final SuiteInfo suite : suites) {
                for (final String moduleLsid : suite.getModuleLsids()) {
                    final String baseLsid = getBaseLsid(moduleLsid);
                    final TagCacheKey key = new TagCacheKey(context.getUserId(), baseLsid);
                    Set<String> tags = tagMap.get(key);

                    // Protect against null
                    if (tags == null) tags = new HashSet<String>();

                    // Add the suite tag
                    tags.add(suite.getName());
                    tagMap.put(key, tags);
                }
            }
        }
        catch (AdminDAOSysException e1) {
            log.error("Exception adding suites for user="+context.getUserId(), e1);
        }
        catch (Throwable t) {
            log.error("Unexpected exception adding suites for user="+context.getUserId(), t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    private class TagCacheKey {
        String user;
        String lsid;
        
        public TagCacheKey(String user, String lsid) {
            this.setUser(user);
            this.setLsid(lsid);
        }
        
        public String getUser() {
            return user;
        }
        public void setUser(String user) {
            this.user = user;
        }
        public String getLsid() {
            return lsid;
        }
        public void setLsid(String lsid) {
            this.lsid = lsid;
        }
        
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (!(obj instanceof TagCacheKey))
                return false;
            
            TagCacheKey key = (TagCacheKey) obj;
            if (user.equals(key.getUser()) && lsid.equals(key.getLsid())) {
                return true;
            }
            else {
                return false;
            }
        }
        
        public int hashCode() {
            return new HashCodeBuilder(17, 31).append(user).append(lsid).toHashCode();
        }
    }
}
