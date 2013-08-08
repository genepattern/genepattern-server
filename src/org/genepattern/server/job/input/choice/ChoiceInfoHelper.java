package org.genepattern.server.job.input.choice;

import java.io.File;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author pcarr
 *
 */
public class ChoiceInfoHelper {
    final static private Logger log = Logger.getLogger(ChoiceInfoHelper.class);
    
    final static public class Ex extends Exception {
        public Ex() {
            super();
        }
        public Ex(String str) {
            super(str);
        }
    }
    
    final static public boolean isSet(final String in) {
        if (in == null || in.length()==0) {
            return false;
        }
        return true;
    }

    final static public ChoiceInfo initChoiceInfo(final ParameterInfo pinfo) {
        try {
            return ChoiceInfo.getChoiceInfoParser().initChoiceInfo(pinfo);
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
    }
    
    /**
     * Get the JSON representation for the given choiceInfo.
     * 
     * @see TasksResource#getChoiceInfo(javax.ws.rs.core.UriInfo, String, String, HttpServletRequest)
     * 
     * @param pinfo
     * @return
     */
    final static public JSONObject initChoiceInfoJson(final HttpServletRequest request, final TaskInfo taskInfo, final ChoiceInfo choiceInfo) throws JSONException {
        if (choiceInfo==null) {
            throw new IllegalArgumentException("choiceInfo==null");
        }
        final JSONObject json=new JSONObject();
        if (request != null && taskInfo != null && isSet(choiceInfo.getParamName())) {
            final String href=TasksResource.getChoiceInfoPath(request, taskInfo, choiceInfo.getParamName());
            json.put("href", href);
        }
        if (isSet(choiceInfo.getChoiceDir())) {
            json.put(ChoiceInfo.PROP_CHOICE_DIR, choiceInfo.getChoiceDir());
        }
        if (choiceInfo.getStatus() != null) {
            final JSONObject statusObj=new JSONObject();
            statusObj.put("flag", choiceInfo.getStatus().getFlag().name());
            statusObj.put("message", choiceInfo.getStatus().getMessage());
            json.put("status", statusObj);
        }
        if (choiceInfo.getSelected() != null) {
            json.put("selectedValue", choiceInfo.getSelected().getValue());
        }
        final JSONArray choices=initChoiceJson(choiceInfo);
        json.put("choices", choices);
        json.put(ChoiceInfo.PROP_CHOICE_ALLOW_CUSTOM_VALUE, choiceInfo.isAllowCustomValue());
        return json;
    }
    
    final static private JSONArray initChoiceJson(final ChoiceInfo choiceInfo) {
        if (choiceInfo==null) {
            log.error("Unexpected null value");
            return null;
        }
        //create a json array of the choices
        try {
            JSONArray choices=new JSONArray();
            for(final Choice choice : choiceInfo.getChoices()) {
                JSONObject choiceObj=new JSONObject();
                choiceObj.put("label", choice.getLabel());
                choiceObj.put("value", choice.getValue());
                choices.put(choiceObj);
            }
            return choices;
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
    }
    
    /**
     * Check for local copy of file from external url, download if necessary.
     * 
     * @param jobContext
     * @param choiceInfo
     * @param selectedChoice
     * @return
     */
    public static GpFilePath getCachedValue(final Context jobContext, final Choice selectedChoice) throws Ex {
        if (jobContext==null) {
            log.warn("jobContext==null");
        }
        if (selectedChoice==null) {
            throw new IllegalArgumentException("choice==null");
        }
        final GpFilePath fromCache;
        if (selectedChoice.isRemoteDir()) {
            fromCache = getSelectedValueAsUserUploadDir(selectedChoice);
        }
        else {
            fromCache = getSelectedValueAsUserUploadFile(selectedChoice);
        }
        return fromCache;
    }
    
    /**
     * If necessary transfer the file from the external URL into the uploads directory for the '.cache'
     * user account.
     * 
     * @param selectedChoice
     * @return
     * @throws InterruptedException
     * @throws Ex
     */
    private static GpFilePath getSelectedValueAsUserUploadFile(final Choice selectedChoice) throws Ex {
        final URL url=JobInputHelper.initExternalUrl(selectedChoice.getValue());
        if (url==null) {
            //it's not an external url
            return null;
        }
        
        GpFilePath selectedGpFilePath=null;
        final Context userContext=ServerConfiguration.Context.getContextForUser(".cache");
        final String relPath="cache/"+url.getHost()+"/"+url.getPath();
        final File relFile=new File(relPath);
        try {
            selectedGpFilePath=GpFileObjFactory.getUserUploadFile(userContext, relFile);
        }
        catch (Throwable t) {
            log.error(t);
        }
        if (selectedGpFilePath==null) {
            return null;
        }

        // Make sure the URL is copied to the right directory, caching as necessary
        try {
            ParamListHelper.copyExternalUrlToUserUploads(selectedGpFilePath, url);
            return selectedGpFilePath;
        }
        catch (Throwable t) {
            log.error(t);
            throw new Ex(t.getLocalizedMessage());
        }
    }
    
    private static GpFilePath getSelectedValueAsUserUploadDir(final Choice selectedChoice) throws Ex {
        throw new Ex("Caching not implemented for remote directory");
    }

}
