package org.genepattern.server.executor.lsf;

import java.io.File;

import org.apache.log4j.Logger;


public class LsfProperties {
    private static Logger log = Logger.getLogger(LsfProperties.class);

    //default values
    private String project="gp_dev";
    private String queue="genepattern";
    private String maxMemory="2"; //2G
    private String lsfWrapperScript=null;
    private String lsfOutputFilename=".lsf.out";
    private boolean usePreExecCommand=false;

    public void setProject(String s) {
        project=s;
    }
    public String getProject() {
        return project;
    }

    public void setQueue(String s) {
        queue=s;
    }
    public String getQueue() {
        return queue;
    }

    public void setMaxMemory(String s) {
        //validate maxMemory
        if (s == null) {
            maxMemory = "2";
        }
        try {
            Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            log.error("Invalid setting for 'lsf.max.memory="+s+"': "+e.getLocalizedMessage(), e);
            maxMemory="2";
        }
        maxMemory=s;
    }
    
    
    public String getMaxMemory() {
        return maxMemory;
    }

    public void setWrapperScript(String s) {
        log.debug("setting lsf.wrapper.script: "+s+" ...");
        if (s != null) {
            File f = new File(s);
            if (!f.isAbsolute()) {
                f = new File(System.getProperty("genepattern.properties"), s);
            }
            if (!f.isFile() || !f.canRead()) {
                log.error("Configuration error, 'lsf.wrapper.script="+s+"' can't read: "+f.getAbsolutePath());
                s=null;
            }
            else {
                s=f.getAbsolutePath();
            }
        }
        lsfWrapperScript=s;
        log.debug("lsf.wrapper.script="+lsfWrapperScript);
    }
    public String getWrapperScript() {
        return lsfWrapperScript;
    }
    
    public void setLsfOutputFilename(String s) {
        lsfOutputFilename=s;
    }
    public String getLsfOutputFilename() {
        return lsfOutputFilename;
    }
    
    public void setUsePreExecCommand(boolean b) {
        usePreExecCommand=b;
    }
    public boolean getUsePreExecCommand() {
        return usePreExecCommand;
    }

}
