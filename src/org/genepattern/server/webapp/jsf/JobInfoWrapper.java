package org.genepattern.server.webapp.jsf;

import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.auth.GroupPermission.Permission;
import org.genepattern.server.genepattern.RunVisualizer;
import org.genepattern.server.webapp.jsf.JobInfoBean.InputParameter;
import org.genepattern.server.webapp.jsf.JobInfoBean.OutputParameter;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class JobInfoWrapper {
    private static Logger log = Logger.getLogger(JobInfoWrapper.class);

    private JobInfo jobInfo = null;
    private int jobNumber;
    private String taskName;
    private String status;
    private Date dateSubmitted;
    private Date dateCompleted;
    private List<InputParameter> inputParameters;
    private List<OutputParameter> outputFiles;
    private TaskInfo taskInfo = null;
    private boolean isVisualizer = false;
    
    private PermissionsHelper permissionsHelper;
    
    public JobInfoWrapper(JobInfo jobInfo, TaskInfo taskInfo) {
        this.jobInfo = jobInfo;
        this.jobNumber = jobInfo.getJobNumber();
        this.taskName = jobInfo.getTaskName();
        this.status = jobInfo.getStatus();
        this.dateSubmitted = jobInfo.getDateSubmitted();
        this.dateCompleted = jobInfo.getDateCompleted();
        
        this.isVisualizer = taskInfo != null && taskInfo.isVisualizer();
        this.taskInfo = taskInfo;
    }
    
    public void setInputParameters(List<InputParameter> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public void setOutputFiles(List<OutputParameter> outputFiles) {
        this.outputFiles = outputFiles;
    }
    
    public void setPermissionsHelper(PermissionsHelper ph) {
        this.permissionsHelper = ph;
    }
    
    public String getOwner() {
        return jobInfo.getUserId();
    }
    
    public int getJobNumber() {
        return jobNumber;
    }
     
    public String getTaskName() {
        return taskName;
    }

    public String getStatus() {
        return status;
    }

    public Date getDateSubmitted() {
        return dateSubmitted;
    }
    
    public Date getDateCompleted() {
        return dateCompleted;
    }

    public List<InputParameter> getInputParameters() {
        return inputParameters;
    }

    public List<OutputParameter> getOutputFiles() {
        return outputFiles;
    }

    public long getElapsedTimeMillis() {
        if (dateSubmitted == null) return 0;
        else if (dateCompleted != null) return dateCompleted.getTime() - dateSubmitted.getTime();
        else if (!"finished".equals(getStatus())) return new Date().getTime() - dateSubmitted.getTime();
        else return 0;
    }
    
    public long getRefreshInterval() {
        long elapsedTimeMillis = getElapsedTimeMillis();
        if (elapsedTimeMillis < 10000) {
            return 1000;
        }
        else if (elapsedTimeMillis < 60000) {
            return 5000;
        }
        else {
            return 10000;
        }
    }
    
    public String getRefreshIntervalLabel() {
        long millis = getRefreshInterval();
        long sec = millis / 1000L;
        if (sec == 1) {
            return "1 second";
        }
        else {
            return sec + " seconds";
        }
    }

    public List<GroupPermission> getNonPublicGroupPermissions() {
        return permissionsHelper.getNonPublicPermissions();
    }
    
    public Permission getPublicAccessPermission() {
        return permissionsHelper.getPublicAccessPermission();
    }
    
    public void setPublicAccessPermission(Permission p) {
        log.error("ignoring setPublicAccessPermission: "+p);
    }
    
    public boolean isPublicNone() {
        return permissionsHelper.getPublicAccessPermission().equals( Permission.NONE );
    }

    public boolean isPublicRead() {
        return permissionsHelper.getPublicAccessPermission().equals( Permission.READ );        
    }
    
    public boolean isPublicReadWrite() {
        return permissionsHelper.getPublicAccessPermission().equals( Permission.READ_WRITE );
    }

    /**
     * get the number of groups that this user is a member of, excluding public groups from the count.
     * @return
     */
    public int getNumNonPublicGroups() {
        return permissionsHelper.getNonPublicPermissions().size();
    }
    
    public List<GroupPermission> getGroupPermissions() {
        boolean includeUsersGroups = true;
        return permissionsHelper.getJobResultPermissions(includeUsersGroups);
    }
    
    public boolean getSetJobPermissionsAllowed() {
        return permissionsHelper.canSetJobPermissions();
    }
    
    /**
     * Process request parameters (from form submission) and update the access permissions for the current job.
     * Only the owner of a job is allowed to change its permissions.
     */
    public String saveGroupPermissions() { 
        //List<GroupPermission> permissions = getGroupPermissions();
        boolean includeUsersGroups = true;
        List<GroupPermission> permissions = permissionsHelper.getJobResultPermissions(includeUsersGroups);

        /* 
         JSF auto generated parameter names from jobResult.xhtml, e.g.
           permForm:   permForm
           permForm:permTable:0:RW:    true
           permForm:permTable:0:R:     true
           permForm:permTable:1:R:     true
         Note: only selected checkboxes are submitted.
        */
        
        //not sure this is the best approach, but for now, 
        //    regenerate the table in the exact order as was done to present the input form
        
        //NOTE: don't edit the jobResult.xhtml without also editing this page 
        //    in other words, DON'T REUSE THIS CODE in another page unless you know what you are doing
        Set<GroupPermission> updatedPermissions = new HashSet<GroupPermission>();
        Map<String,String[]> requestParameters = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
        for(String name : requestParameters.keySet()) {
            //System.out.println("\t"+val);
            if (name.endsWith("R") || name.endsWith("RW")) {
                int gin = -1;
                String[] splits = name.split(":");
                String permFlag = splits[ splits.length - 1 ];
                int sin = splits.length - 2;
                if (sin > 0) {
                    try {
                        gin = Integer.parseInt( splits[sin] );
                        String groupId = permissions.get(gin).getGroupId();
                        System.out.println("set "+permFlag+" permission for group: " + groupId);
                        
                        Permission p = null;
                        if (permFlag.equalsIgnoreCase("R")) {
                            p = GroupPermission.Permission.READ;
                        }
                        else if (permFlag.equalsIgnoreCase("RW")) {
                            p = GroupPermission.Permission.READ_WRITE;                                
                        }
                        else {
                            handleException("Ignoring permissions flag: "+permFlag);
                            return "error";
                        }
                        GroupPermission gp = new GroupPermission(groupId, p);
                        updatedPermissions.add(gp);
                    }
                    catch (NumberFormatException e) {
                        handleException("Can't parse input form", e);
                        return "error";
                    }
                }
            }
        }
        
        try {
            permissionsHelper.setPermissions(updatedPermissions);
            return "success";
        }
        catch (Exception e) {
            handleException("You are not authorized to change the permissions for this job", e);
            return "error";
        }
    }
    
    private void handleException(String message) {
        log.error(message);
        UIBeanHelper.setErrorMessage(message);
    }

    private void handleException(String message, Exception e) {
        log.error(message, e);
        UIBeanHelper.setErrorMessage(message);
    }

    public String getPermissionsLabel() {
        List<GroupPermission> groups = getGroupPermissions();
        if (groups == null || groups.size() == 0) {
            return "";
        }
        String rval = "";
        for (GroupPermission gp : groups) {
            rval += gp.getGroupId() + " " + gp.getPermission() + ", ";
        }
        int idx = rval.lastIndexOf(", ");
        return rval.substring(0, idx) + "";
    }
    
    public boolean getIsVisualizer() {
        return isVisualizer;
    }

    public String getVisualizerAppletTag() {
        RunVisualizer runVis = new RunVisualizer();
        runVis.setJobInfo(jobInfo);
        TaskInfoAttributes taskInfoAttributes = taskInfo.giveTaskInfoAttributes();
        runVis.setTaskInfoAttributes(taskInfoAttributes);
        //TODO: lookup context path from servlet context
        runVis.setContextPath("/gp");
        StringWriter writer = new StringWriter();
        try {
            runVis.writeVisualizer(writer);
            writer.close();
        }
        catch (Exception e) {
            writer.write("<p>Error in getVisualizerAppletTag: "+e.getLocalizedMessage()+"</p>");
        }
        return writer.toString();
    }
    
//    public String getVisualizerAppletTag2() throws MalformedURLException //, UnsupportedEncodingException 
//    {
//        String name = this.taskName;
//        //TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
//        TaskInfoAttributes tia = taskInfo.getTaskInfoAttributes();
//        String userID = UIBeanHelper.getUserId();
//        // unique name so that multiple instances of applet on a single page will not collide
//        String appletName = "a" + ("" + Math.random()).substring(2); // unique
//        String javaFlags = new LocalAdminClient(userID).getUserProperty(UserPropKey.VISUALIZER_JAVA_FLAGS);
//        if(javaFlags==null) {
//            javaFlags = System.getProperty(RunVisualizerConstants.JAVA_FLAGS_VALUE);
//        }
//        //String lsid = (String) tia.get(GPConstants.LSID);
//        String lsid = taskInfo.getLsid();
//
//        //String contextPath = request.getContextPath();
//        //String contextPath = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
//        //String pathInfo = FacesContext.getCurrentInstance().getExternalContext().getRequestPathInfo();
//        String codebase = "http://127.0.0.1:8080/gp/downloads";
//        
//        ParameterInfo[] parameterInfoArray = ParameterFormatConverter.getParameterInfoArray(taskInfo.getParameterInfo());
//        if (parameterInfoArray == null) parameterInfoArray = new ParameterInfo[0];
//        
//        StringWriter appletTag = new StringWriter();
//        appletTag.append("<applet code=\"" + org.genepattern.visualizer.RunVisualizerApplet.class.getName() + "\" archive=\"runVisualizer.jar,commons-httpclient.jar,commons-codec-1.3.jar\" codebase=\"../downloads\" width=\"1\" height=\"1\" alt=\"Your browser refuses to run applets\" name=\""+appletName+"\">");
//        appletTag.append("<param name=\"" + RunVisualizerConstants.NAME + "\" value=\"" + name + "\">");
//        appletTag.append("<param name=\"" + RunVisualizerConstants.OS + "\" value=\"" + tia.get(GPConstants.OS) + "\">");
//        appletTag.append("<param name=\"" + RunVisualizerConstants.CPU_TYPE + "\" value=\"" + tia.get(GPConstants.CPU_TYPE) + "\">");
//        appletTag.append("<param name=\"" + RunVisualizerConstants.JAVA_FLAGS_VALUE + "\" value=\"" + javaFlags + "\">");
//        //appletTag.append("<param name=\"" + RunVisualizerConstants.CONTEXT_PATH + "\" value=\"" + URLEncoder.encode(contextPath, "UTF-8") + "\">");
//        StringBuffer paramValue = new StringBuffer();
//        for (int i = 0; i < parameterInfoArray.length; i++) {
//            if (i > 0) paramValue.append(",");
//            paramValue.append(parameterInfoArray[i].getName());
//        }
//
//        appletTag.append("<param name=\"" + RunVisualizerConstants.PARAM_NAMES + "\" value=\"" + paramValue.toString() + "\">");
//        for (ParameterInfo pInfo : parameterInfoArray) {
//            String paramName = pInfo.getName();
//            String value = pInfo.getValue();
//            //appletTag.append("<param name=\"" + paramName + "\" value=\"" + URLEncoder.encode(value, "UTF-8") + "\">");
//            appletTag.append("<param name=\"" + paramName + "\" value=\"" + value + "\">");
//        }
//
//        StringBuffer vis = new StringBuffer();
//        int numToDownload = 0;
//        for (ParameterInfo pInfo : parameterInfoArray) {
//            if (pInfo.isInputFile() && pInfo.getValue() != null) {
//                try {
//                    new java.net.URL(pInfo.getValue());
//                    // note that this parameter is a URL that must be downloaded by adding it to the CSV list for the applet
//                    if (numToDownload > 0) {
//                        vis.append(",");
//                    }
//                    vis.append(pInfo.getName());
//                    numToDownload++;
//                } 
//                catch (Exception x) {
//                }
//            }
//        }
//
//        appletTag.append("<param name=\"" + RunVisualizerConstants.DOWNLOAD_FILES + "\" value=\"" + vis.toString() + "\">");
//        String commandLine = tia.get(GPConstants.COMMAND_LINE);
//        commandLine = StringUtils.htmlEncode(commandLine);
//        //try {
//        //    commandLine = URLEncoder.encode(commandLine, "UTF-8");
//        //}
//        //catch (UnsupportedEncodingException e) {
//        //    log.error(e.getLocalizedMessage(), e);
//        //}
//        appletTag.append("<param name=\"" + RunVisualizerConstants.COMMAND_LINE + "\" value=\"" + commandLine + "\">");
//        appletTag.append("<param name=\"" + RunVisualizerConstants.DEBUG + "\" value=\"1\">");
//
//        StringBuffer fileNamesBuf = new StringBuffer();
//        String libdir = DirectoryManager.getTaskLibDir(null, lsid, userID);
//        File[] supportFiles = new File(libdir).listFiles();
//        for (int i = 0; i < supportFiles.length; i++) {
//            if (i > 0) fileNamesBuf.append(",");
//                fileNamesBuf.append(supportFiles[i].getName());
//        }
//        appletTag.append("<param name=\"" + RunVisualizerConstants.SUPPORT_FILE_NAMES + "\" value=\"" + fileNamesBuf.toString() + "\" >");
//
//        StringBuffer fileDatesBuf = new StringBuffer();
//        for (int i = 0; i < supportFiles.length; i++) {
//            if (i > 0) fileDatesBuf.append(",");
//                fileDatesBuf.append(supportFiles[i].lastModified());
//        }
//        appletTag.append("<param name=\"" + RunVisualizerConstants.SUPPORT_FILE_DATES + "\" value=\"" + fileDatesBuf.toString() + "\" >");
//        appletTag.append("<param name=\"" + RunVisualizerConstants.LSID + "\" value=\"" + lsid + "\">");
//
//
//        StringBuffer rval = new StringBuffer();
//        //for debugging
//        rval.append("<SCRIPT LANGUAGE=\"JavaScript\">window.alert(\"Launching visualizer ... codebase="+codebase+"\");</SCRIPT>\n");
//        //use JavaScript to write the applet tag so that the browserCookie is property set as an applet parameter
//        rval.append("<SCRIPT LANGUAGE=\"JavaScript\">");
//        rval.append("document.writeln('"+appletTag.toString()+" <PARAM name=\"browserCookie\" value=\"' + document.cookie + '\"></applet>');</SCRIPT>");
//        return rval.toString();
//        
//    }

}
