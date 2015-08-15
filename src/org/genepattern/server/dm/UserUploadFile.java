/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.input.cache.FileCache;
import org.genepattern.server.job.input.dao.JobInputValueRecorder;


public class UserUploadFile extends GpFilePath {
    private static Logger log = Logger.getLogger(UserUploadFile.class);

    private URI relativeUri;
    private File serverFile;
    private File relativeFile;

    public UserUploadFile(URI relativeUri) {
        this.relativeUri = relativeUri;
    }
    
    public URI getRelativeUri() {
        return this.relativeUri;
    }

    public File getServerFile() {
        return this.serverFile;
    }
    void setServerFile(File file) {
        this.serverFile = file;
    }
    
    public File getRelativeFile() {
        return this.relativeFile;
    }
    void setRelativeFile(File file) {
        this.relativeFile = file;
    }

    public String getFormFieldValue() {
        String formFieldValue = "";
        try {
            formFieldValue = this.getUrl().toExternalForm();
        }
        catch (Exception e) {
            log.error(e);
        }
        return formFieldValue;
    }

    public String getParamInfoValue() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean canRead(final boolean isAdmin, final GpContext userContext) {
        if (isAdmin) {
            return true;
        }

        // special-case for cached data files from external URL
        if (owner.equals( FileCache.CACHE_USER_ID )) {
            return true;
        }

        if (owner == null || owner.length() == 0) {
            return false;
        }
        
        if (userContext == null) {
            return false;
        }
        
        if (userContext.getUserId()==null) {
            log.debug("userContext.userId==null");
            return false;
        }
        
        if ( owner.equals( userContext.getUserId() ) ) {
            if (log.isDebugEnabled()) {
                log.debug("file owned by userId="+userContext.getUserId());
            }
            return true;
        }
        
        // special-case, is this an input file for a shared job
        boolean isSharedJobInput=canRead(userContext, this.getFormFieldValue());
        if (log.isDebugEnabled() && isSharedJobInput) {
            log.debug("isShared, userId="+userContext.getUserId()+", file="+this.getFormFieldValue());
        }
        return isSharedJobInput;
    }
    
    public boolean canRead(final GpContext userContext, final String inputValue) {
        String userId=userContext.getUserId();
        try {
            List<String> matchingGroups=new JobInputValueRecorder(HibernateUtil.instance()).fetchMatchingGroups(inputValue);
            if (matchingGroups.size()==0) {
                return false;
            }
            IGroupMembershipPlugin groupMembership = UserAccountManager.instance().getGroupMembership();
            for(final String groupId : matchingGroups) {
                if (groupMembership.isMember(userId, groupId)) {
                    return true;
                } 
            }
        }
        catch (Exception e) {
            log.error("Error checking access permissions for userId="+userId+", file="+inputValue, e);
        }
        return false; 
    }

}
