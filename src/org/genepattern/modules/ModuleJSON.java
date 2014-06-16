/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2014) by the
 Broad Institute. All rights are reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. The Broad Institute cannot be responsible for its
 use, misuse, or functionality.
*/

package org.genepattern.modules;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import org.apache.log4j.Logger;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.util.GPConstants;

import java.io.File;
import java.util.Set;
import java.util.Iterator;

/**
 * User: nazaire
 * Date: Mar 12, 2012
 */
public class ModuleJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ModuleJSON.class);

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String AUTHOR = "author";
    public static final String PRIVACY = "privacy";
    public static final String QUALITY = "quality";
    public static final String VERSION_COMMENT = "version";
    public static final String LSID = "LSID";
    public static final String CATEGORY = "taskType";
    public static final String LANGUAGE = "language";
    public static final String JVM_LEVEL = "JVMLevel";        
    public static final String CPU = "cpuType";
    public static final String OS = "os";
    public static final String COMMAND_LINE = "commandLine";
    public static final String FILEFORMAT = "fileFormat";    
    public static final String SUPPORTFILES = "supportFiles";
    public static final String FILESTODELETE = "filesToDelete";
    public static final String LICENSE = "license";


    public static final String KEY = "module";

    private String[] supportFiles;
    private String fileFormats;

    //list of files that were in previous version of module that
    // were deleted in this updated version
    private String[] removedFiles;


    public ModuleJSON(JSONObject object) {
        try {
            this.put(NAME, object.get(NAME));
            this.put(DESCRIPTION, object.get(DESCRIPTION));
            this.put(AUTHOR, object.get(AUTHOR));
            this.put(PRIVACY, object.get(PRIVACY));
            this.put(VERSION_COMMENT, object.get(VERSION_COMMENT));
            this.put(CATEGORY, object.get(CATEGORY));
            this.put(QUALITY, object.get(QUALITY));
            this.put(LANGUAGE, object.get(LANGUAGE));
            this.put(JVM_LEVEL, object.get(JVM_LEVEL));
            this.put(CPU, object.get(CPU));
            this.put(OS, object.get(OS));
            this.put(COMMAND_LINE, object.get(COMMAND_LINE));
            this.put(LSID, object.get(LSID));
            this.put(SUPPORTFILES, object.get(SUPPORTFILES));
            this.put(FILESTODELETE, object.get(FILESTODELETE));
            this.put(FILEFORMAT, object.get(FILEFORMAT));
	        this.put(LICENSE, object.get(LICENSE));

            Iterator<String> kIter = object.keys();
            while(kIter.hasNext())
            {
                String keyName = kIter.next();
                if(!this.has(keyName))
                {
                    this.put(keyName, object.get(keyName));
                }
            }
        }
        catch (JSONException e) {
            log.error(e);
            log.error("Unable to create ModuleJSON from generic JSONObject");
        }
    }

    public ModuleJSON(TaskInfo taskInfo, File[] supportFiles)
    {
        try
        {
            TaskInfoAttributes tia = taskInfo.getTaskInfoAttributes();
            this.put(NAME, taskInfo.getName());
            this.put(DESCRIPTION, taskInfo.getDescription());
            this.put(AUTHOR, tia.get(GPConstants.AUTHOR));
            this.put(PRIVACY, tia.get(GPConstants.PRIVACY));
            this.put(VERSION_COMMENT, tia.get(GPConstants.VERSION));
            this.put(CATEGORY, tia.get(GPConstants.TASK_TYPE));
            this.put(QUALITY, tia.get(GPConstants.QUALITY));
            this.put(LANGUAGE, tia.get(GPConstants.LANGUAGE));
            this.put(FILEFORMAT, tia.get(GPConstants.FILE_FORMAT));
            this.put(CPU, tia.get(GPConstants.CPU_TYPE));
            this.put(OS, tia.get(GPConstants.OS));
            this.put(COMMAND_LINE, tia.get(GPConstants.COMMAND_LINE));
            this.put(LSID, tia.get(GPConstants.LSID));

            //add remaining task info attributes
            Set keys = tia.keySet();
            Iterator<String> kIter = keys.iterator();
            while(kIter.hasNext())
            {
                String keyName = kIter.next();
                if(!this.has(keyName))
                {
                    this.put(keyName, tia.get(keyName));
                }
            }
            String supportFileNames="";
            if(supportFiles != null)
            {
                for(int i=0;i<supportFiles.length;i++)
                {
                    supportFileNames+=supportFiles[i].getName();

                    //append ; separator between each file name
                    if(i+1 < supportFiles.length)
                    {
                        supportFileNames+=";";
                    }
                }
            }
            this.put(SUPPORTFILES, supportFileNames);
        }
        catch (JSONException e) {
            log.error(e);
            log.error("Error creating module JSON for: " + taskInfo.getName());
        }
    }

    public static JSONObject parseBundle(String bundle)
    {
        JSONObject moduleJSON = null;
        try {
            moduleJSON = new JSONObject(bundle);
        }
        catch (JSONException e) {
            log.error("Error parsing JSON in the saved bundle");
        }
        return moduleJSON;
    }

    public static ModuleJSON extract(JSONObject json) {
        try {
            JSONObject object = (JSONObject) json.get(ModuleJSON.KEY);
            return new ModuleJSON(object);
        }
        catch (JSONException e) {
            log.error("Unable to extract ModuleJSON from saved bundle");
            return null;
        }
    }

    public String getLsid() throws JSONException {
        return this.getString(LSID);
    }

    public void setLsid(String lsid) throws JSONException {
        this.put(LSID, lsid);
    } 

    public String getDescription() throws JSONException {
        return this.getString(DESCRIPTION);
    }

    public void setDescription(String description) throws JSONException {
        this.put(DESCRIPTION, description);
    }

    public String getName() throws JSONException {
        return this.getString(NAME);
    }

    public void setName(String name) throws JSONException {
        this.put(NAME, name);
    }

    public String getFileFormats()throws JSONException
    {
        if(fileFormats == null)
        {
            fileFormats = "";

            JSONArray files = (JSONArray)this.get(FILEFORMAT);
            if(files != null)
            {
                for(int i=0; i < files.length();i++)
                {
                    fileFormats += (String)files.get(i);
                    if(i+1 < files.length())
                    {
                        fileFormats += GPConstants.PARAM_INFO_CHOICE_DELIMITER;
                    }
                }
            }
        }
        return fileFormats;
    }
    
    public String[] getRemovedFiles()throws JSONException
    {
        if(removedFiles == null)
        {
            JSONArray files = (JSONArray)this.get(FILESTODELETE);
            if(files != null)
            {
                removedFiles = new String[files.length()];
                for(int i=0; i < files.length();i++)
                {
                    removedFiles[i] = (String)files.get(i);
                }
            }
        }

        return removedFiles;
    }

    public String[] getSupportFiles()throws JSONException
    {
        if(supportFiles == null)
        {
            JSONArray files = (JSONArray)this.get(SUPPORTFILES);
            if(files != null)
            {
                supportFiles = new String[files.length()];
                for(int i=0; i < files.length();i++)
                {
                    supportFiles[i] = (String)files.get(i);
                }
            }
        }

        return supportFiles;
    }
}
