/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.UserAccountManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.util.PropertiesManager_3_2;
import org.genepattern.server.webapp.rest.api.v1.oauth.OAuthConstants;
import org.genepattern.visualizer.RunVisualizerConstants;

public class UserPrefsBean {
    private static Logger log = Logger.getLogger(UserPrefsBean.class);
    private String userId;
    private UserProp javaFlagsProp;
    private UserProp recentJobsProp;
    private List<String> groups;

    private UserProp globusUserIdentityProp;
    private UserProp globusUserEmailProp;
    private UserProp globusIdentityProviderIdProp;
    private UserProp globusIdentityProviderNameProp;
    private UserProp globusAccessTokenProp;
    private UserProp globusRefreshTokenProp;
    private UserProp globusTransferTokenProp;
    private UserProp globusTransferRefreshTokenProp;
   

    public UserPrefsBean() {
        this.userId = UIBeanHelper.getUserId();
        UserDAO dao = new UserDAO();
        
        GpContext userContext = GpContext.getContextForUser(userId);
        String systemVisualizerJavaFlags = ServerConfigurationFactory.instance().getGPProperty(userContext, RunVisualizerConstants.JAVA_FLAGS_VALUE);
        javaFlagsProp = dao.getProperty(userId, UserPropKey.VISUALIZER_JAVA_FLAGS, systemVisualizerJavaFlags);

        globusUserIdentityProp = dao.getProperty(userId, OAuthConstants.OAUTH_USER_ID_USERPROPS_KEY, "");
        globusUserEmailProp = dao.getProperty(userId, OAuthConstants.OAUTH_EMAIL_USERPROPS_KEY, "");
        globusIdentityProviderIdProp = dao.getProperty(userId, OAuthConstants.OAUTH_ID_PROVIDER_ID_USERPROPS_KEY, "");
        globusIdentityProviderNameProp = dao.getProperty(userId, OAuthConstants.OAUTH_ID_PROVIDER_DISPLAY_USERPROPS_KEY, "");
        globusAccessTokenProp = dao.getProperty(userId, OAuthConstants.OAUTH_TOKEN_ATTR_KEY, "");
        globusRefreshTokenProp = dao.getProperty(userId, OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY, "");
        globusTransferTokenProp = dao.getProperty(userId, OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY, "");
        globusTransferRefreshTokenProp = dao.getProperty(userId, OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, "");
        
        String historySize = null;
        try {
            historySize = (String) PropertiesManager_3_2.getDefaultProperties().get("historySize");
        } 
        catch (IOException e) {
            log.error("Unable to retrive historySize property", e);
        }
        recentJobsProp = dao.getProperty(userId, 
                UserPropKey.RECENT_JOBS_TO_SHOW, 
                (historySize == null) ? 
                        UserPropKey.RECENT_JOB_TO_SHOW_DEFAULT 
                        : 
                        historySize);
        
        Set<String> groupSet = UserAccountManager.instance().getGroupMembership().getGroups(userId);
        groups = new ArrayList<String>(groupSet);
        //TODO: sort the groups by name
    }

    public String saveJavaFlags() {
        UIBeanHelper.setInfoMessage("Visualizer memory successfully updated.");
        return "my settings";
    }

    public String saveRecentJobs() {
        UIBeanHelper.setInfoMessage("Number of recent jobs successfully updated.");
        return "my settings";
    }
    
    public String clearGlobus(){
        globusUserIdentityProp.setValue(null);
        globusIdentityProviderIdProp.setValue(null);
        globusIdentityProviderNameProp.setValue(null);
        globusUserEmailProp.setValue(null);
        globusAccessTokenProp.setValue(null);
        globusRefreshTokenProp.setValue(null);
        
        globusTransferTokenProp.setValue(null);
        globusTransferRefreshTokenProp.setValue(null);
        
        HttpServletRequest servletRequest = UIBeanHelper.getRequest();
        
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_TRANSFER_REFRESH_TOKEN_ATTR_KEY, null);
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_TRANSFER_TOKEN_ATTR_KEY, null);
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_TOKEN_ATTR_KEY, null);
        servletRequest.getSession().setAttribute(OAuthConstants.OAUTH_REFRESH_TOKEN_ATTR_KEY, null);
        
        
        
        UIBeanHelper.setInfoMessage("Globus association cleared.");
        return "my settings";
    }
    

    public int getNumberOfRecentJobs() {
        if (recentJobsProp == null) {
            return 0;
        }
        return Integer.parseInt(recentJobsProp.getValue());
    }

    public void setNumberOfRecentJobs(int value) {
        if (recentJobsProp == null) {
            return;
        }
        recentJobsProp.setValue(String.valueOf(value));
    }

    public String getJavaFlags() {
        if (javaFlagsProp == null) {
            return "";
        }
        return javaFlagsProp.getValue();
    }

    public void setJavaFlags(String value) {
        if (javaFlagsProp == null) {
            return;
        }
        this.javaFlagsProp.setValue(value);
    }
    
    public boolean isGlobusLinked() {
        String val = globusUserIdentityProp.getValue();
        if (val == null) return false;
        else return !val.isEmpty();
    }
    
    public String getGlobusUserIdentity(){
        return globusUserIdentityProp.getValue();
    }
    public void setGlobusUserIdentity(String value){
        if (globusUserIdentityProp == null) return;
         globusUserIdentityProp.setValue(null);
    }
    
    public String getGlobusUserEmail(){
        return globusUserEmailProp.getValue();
    }
    public void setGlobusUserEmail(String value){
        if (globusUserEmailProp == null) return;
         globusUserEmailProp.setValue(null);
    }
    
    public String getGlobusIdentityProviderId(){
        return globusIdentityProviderIdProp.getValue();
    }
    public void setGlobusIdentityProviderId(String value){
        if (globusIdentityProviderIdProp == null) return;
        globusIdentityProviderIdProp.setValue(null);
    }
    public String getGlobusIdentityProviderName(){
        return globusIdentityProviderNameProp.getValue();
    }
    public void setGlobusIdentityProviderName(String value){
        if (globusIdentityProviderNameProp == null) return;
        globusIdentityProviderNameProp.setValue(null);
    }
    public String getGlobusAccessToken(){
        return globusAccessTokenProp.getValue();
    }
    public void setGlobusAccessToken(String value){
        if (globusAccessTokenProp == null) return;
        globusAccessTokenProp.setValue(null);
    }
    public String getGlobusRefreshToken(){
        return globusRefreshTokenProp.getValue();
    }
    public void setGlobusRefreshToken(String value){
        if (globusRefreshTokenProp == null) return;
        globusRefreshTokenProp.setValue(null);
    }
    
    public String getGlobusTransferToken(){
        return globusTransferTokenProp.getValue();
    }
    public void setGlobusTransferToken(String value){
        if (globusTransferTokenProp == null) return;
        globusTransferTokenProp.setValue(null);
    }
    public String getGlobusTransferRefreshToken(){
        return globusTransferRefreshTokenProp.getValue();
    }
    public void setGlobusTransferRefreshToken(String value){
        if (globusTransferRefreshTokenProp == null) return;
        globusTransferRefreshTokenProp.setValue(null);
    }
    
    //add support for groups
    public List<String> getGroups() {
        return groups;
    }
    
    public List<SelectItem> getGroupSelectItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        //items.add(new SelectItem("[0] all", "all"));
        //items.add(new SelectItem("[1] me", "owned by: "+userId));
        items.add(new SelectItem("", "Private"));
        for(String groupName : groups) {
            items.add(new SelectItem(groupName+"RW", groupName+" (View and edit)"));
            items.add(new SelectItem(groupName+"R", groupName+" (View)"));
        }
        return items;
    }
}
