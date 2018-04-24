package org.genepattern.server.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

public class UserGroups implements IGroupMembershipPlugin {
    private static Logger log = Logger.getLogger(UserGroups.class);

    /**
     * Resolve the path to the filename. 
     * Paths are resolved relative to the 'resources' directory. 
     */
    protected static File getResourcesFile(final GpConfig gpConfig, final String filename) {
        final File file = new File(filename);
        if (file.isAbsolute()) {
            return file;
        }        
        // special-case: when 'filename' is a relative path
        return new File(gpConfig.getResourcesDir(), filename);
    }

    public static UserGroups initFromConfig(final GpConfig gpConfig) {
        return initFromConfig(gpConfig, "userGroups.xml");
    }

    public static UserGroups initFromConfig(final GpConfig gpConfig, final String filename) {
        final File userGroupsXmlFile = getResourcesFile(gpConfig, filename );
        return initFromXml(userGroupsXmlFile);
    }

    public static UserGroups initFromXml(final File userGroupsXmlFile) {
        final UserGroups.Builder ugb=new UserGroups.Builder();

        // Parse the input into a JDOM document
        InputStream is=null;
        try { 
            is = new FileInputStream(userGroupsXmlFile);
            final SAXBuilder builder = new SAXBuilder();
            final Document document = builder.build(is);
            final Element root = document.getRootElement();
            for (final Iterator i = root.getChildren("group").iterator(); i.hasNext();) {
                final Element groupElem = (Element) i.next();
                final String groupName = groupElem.getAttribute("name").getValue();
                ugb.addGroup(groupName);
                for (final Iterator i2 = groupElem.getChildren("user").iterator(); i2.hasNext();) {
                    final Element user = (Element) i2.next();
                    final String userName = user.getAttribute("name").getValue();
                    ugb.addUserToGroup(userName, groupName);
                }
            }
        }
        catch (JDOMException e) {
            log.error("Didn't initialize group access permissions: "+e.getLocalizedMessage(), e);
        }
        catch (IOException e) {
            log.error("Didn't initialize group access permissions: "+e.getLocalizedMessage(), e);
        }
        catch (Throwable t) {
            log.error("Didn't initialize group access permissions: "+t.getLocalizedMessage(), t);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    log.error("Didn't initialize group access permissions: "+e.getLocalizedMessage(), e);
                } 
            }
        }
        return ugb.build();
    }

    // as a multimap of groupId -> userId
    final SortedSetMultimap<String,String> groups;
    // as a multimap of userId -> groupId
    final SortedSetMultimap<String,String> users;

    private UserGroups(final Builder builder) {
        this.groups=Multimaps.unmodifiableSortedSetMultimap(builder.groups);
        this.users=Multimaps.unmodifiableSortedSetMultimap(builder.users);
    }

    public Set<String> getGroups(final String userId) {
        if (userId==null) {
            return null;
        }
        return Sets.union(users.get("*"), users.get(userId));
    }

    public boolean isMember(final String userId, final String groupId) {
        return users.containsEntry("*", groupId) ||
            users.containsEntry(userId, groupId);
    }
    
    public Set<String> getUsers(final String groupId) {
        return groups.get(groupId);
    }
    
    public static final class Builder {
        // as a multimap of groupId -> userId
        final SortedSetMultimap<String,String> groups = TreeMultimap.create();
        // as a multimap of userId -> groupId
        final SortedSetMultimap<String,String> users = TreeMultimap.create();

        public Builder addGroup(final String groupId) {
            // no-op, not needed
            return this;
        }

        protected Builder addUserToGroup(final String userId, final String groupId) {
            users.put(userId, groupId);
            groups.put(groupId, userId);
            return this;
        }

        public UserGroups build() {
            return new UserGroups(this);
        }
    }

}
