package org.genepattern.server.tags;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.domain.PinModule;
import org.genepattern.server.domain.PinModuleDAO;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webservice.server.dao.AdminDAO;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Singleton class for managing tags in GenePattern
 * @author tabor
 */
public class TagManager {
    final static private Logger log = Logger.getLogger(TagManager.class);
    private static TagManager singleton = null;
    
    public static TagManager instance() {
        if (singleton == null) {
            singleton = new TagManager();
        }
        return singleton;
    }
    
    private Map<String, Date> userCacheMap = new ConcurrentHashMap<String, Date>();                     // User to cache update date
    private Map<TagCacheKey, Set<Tag>> tagMap = new ConcurrentHashMap<TagCacheKey, Set<Tag>>();         // User+lsid to set of tags
    
    public Set<Tag> getTags(Context context, TaskInfo taskInfo) {
        
        // Lazily update the tag cache for each TaskInfo
        if (needsUpdate(context)) {
            updateCache(context);
        }
        
        // Protect against null and return
        String baseLsid = getBaseLsid(taskInfo.getLsid());
        TagCacheKey key = new TagCacheKey(context.getUserId(), baseLsid);
        Set<Tag> tagSet = tagMap.get(key);
        if (tagSet == null) return new HashSet<Tag>();
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
     * Rule: Update them if they haven't been loaded or if more than 10 seconds old
     * @param taskInfo
     * @return
     */
    private boolean needsUpdate(Context context) {
        Date now = new Date();
        Date updated = userCacheMap.get(context.getUserId());
        if (updated == null) return true;

        if (updated.before(new Date(now.getTime() - 10000 ))) return true;
        else return false;
    }
    
    /**
     * Update the tag cache and the map of updated dates
     * @param taskInfo
     */
    private void updateCache(Context context) {
        // Remove old tags
        removeOld(context);
        
        // Add tags for the user
        addRecent(context);
        addPinned(context);
        
        // Update the user's update time
        userCacheMap.put(context.getUserId(), new Date());
    }
    
    private void removeOld(Context context) {
        for (TagCacheKey key : tagMap.keySet()) {
            if (key.getUser().equals(context.getUserId())) {
                tagMap.remove(key);
            }
        }
    }
    
    private void addRecent(Context context) {
        AdminDAO adminDao = new AdminDAO();
        int recentJobsToShow = Integer.parseInt(new UserDAO().getPropertyValue(context.getUserId(), UserPropKey.RECENT_JOBS_TO_SHOW, "4"));
        TaskInfo[] recentModules = adminDao.getRecentlyRunTasksForUser(context.getUserId(), recentJobsToShow);
        
        for (TaskInfo recent : recentModules) {
            String baseLsid = getBaseLsid(recent.getLsid());
            TagCacheKey key = new TagCacheKey(context.getUserId(), baseLsid);
            Set<Tag> tags = tagMap.get(key);
            
            // Protect against null
            if (tags == null) tags = new HashSet<Tag>();
            
            // Add the tag
            Tag recentTag = new Tag("recent");
            tags.add(recentTag);
            tagMap.put(key, tags);
        }
    }
    
    private void addPinned(Context context) {
        PinModuleDAO pinDao = new PinModuleDAO();
        List<PinModule> pins = pinDao.getPinsForUser(context.getUserId());

        for (PinModule pin : pins) {
            TagCacheKey key = new TagCacheKey(context.getUserId(), pin.getLsid());
            Set<Tag> tags = tagMap.get(key);
            
            // Protect against null
            if (tags == null) tags = new HashSet<Tag>();
            
            // Add the tag
            Tag pinTag = new Tag("pinned");
            pinTag.setMetadata(new Double(pin.getPosition()).toString());
            tags.add(pinTag);
            tagMap.put(key, tags);
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
    
    /**
     * Class representing tags in GenePattern
     * @author tabor
     */
    public class Tag {
        String tag;
        String description;
        String domain;
        String metadata;
        
        public Tag(String tag) {
            this.setTag(tag);
            this.setDescription("");
            this.setDomain("");
            this.setMetadata("");
        }
        
        public String getMetadata() {
            return metadata;
        }
        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
        public String getTag() {
            return tag;
        }
        public void setTag(String tag) {
            this.tag = tag;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getDomain() {
            return domain;
        }
        public void setDomain(String domain) {
            this.domain = domain;
        }
        
        public String toString() {
            return this.getTag();
        }
        
        public JSONObject toJSON() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("tag", this.getTag());
            object.put("description", this.getDescription());
            object.put("domain", this.getDomain());
            object.put("metadata", this.getMetadata());
            
            return object;
        }
    }
}
