package org.genepattern.server.rest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.executor.JobSubmissionException;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.rest.JobInput.Param;
import org.genepattern.server.rest.JobInput.ParamId;
import org.genepattern.server.rest.JobInput.ParamValue;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * this class accepts a job submit form from the end user and adds a job to the queue.
 * 
 * special cases:
 *     1) when a value is not supplied for an input parameter, the default value will be used
 *         if there is no default value, then assume that the value is not set.
 *     2) when a parameter is not optional, throw an exception if no value has been set
 *     3) when a list of values is supplied, automatically generate a file list file before adding the job to the queue
 *     
 * TODO: 
 *     4) when an external URL is supplied, GET the contents of the file before adding the job to the queue
 *     5) transferring data from an external URL as well as generating a file list can take a while, we should
 *         update this code so that it does not have to block the web client.
 *     
 * @author pcarr
 *
 */
public class JobInputApiImpl implements JobInputApi {
    final static private Logger log = Logger.getLogger(JobInputApiImpl.class);
    
    private GetTaskStrategy getTaskStrategy;
    public JobInputApiImpl() {
    }
    public JobInputApiImpl(final GetTaskStrategy getTaskStrategy) {
        this.getTaskStrategy=getTaskStrategy;
    }
    
    /**
     * Optionally set the strategy for initializing a TaskInfo from a task lsid.
     * 
     * @param impl, an object which implements this interface, can be null.
     */
    public void setGetTaskStrategy(final GetTaskStrategy getTaskStrategy) {
        this.getTaskStrategy=getTaskStrategy;
    }

    @Override
    public String postJob(final String currentUser, final JobInput jobInput) throws GpServerException {
        if (currentUser==null) {
            throw new IllegalArgumentException("currentUser==null");
        }
        if (jobInput==null) {
            throw new IllegalArgumentException("jobInput==null");
        }
        if (jobInput.getLsid()==null) {
            throw new IllegalArgumentException("jobInput.lsid==null");
        }
        try {
            JobInputApiLegacy jobInputHelper=new JobInputApiLegacy(currentUser, jobInput);
            if (getTaskStrategy==null) {
                getTaskStrategy=new GetTaskStrategyDefault();
            }
            jobInputHelper.initTaskInfo(getTaskStrategy);
            jobInputHelper.initParameterValues();
        
            String jobId=jobInputHelper.submitJob();
            return jobId;
        }
        catch (Throwable t) {
            String message="Error adding job to queue, currentUser="+currentUser+", lsid="+jobInput.getLsid();
            log.error(message,t);
            throw new GpServerException(message, t);
        }
    }
    
    static class JobInputApiLegacy {
        private String currentUser;
        private JobInput jobInput;
        //private String lsid;
        private TaskInfo taskInfo;

        public JobInputApiLegacy(final String currentUser, final JobInput jobInput) {
            if (currentUser==null) {
                throw new IllegalArgumentException("currentUser==null");
            }
            if (jobInput==null) {
                throw new IllegalArgumentException("jobInput==null");
            }
            if (jobInput.getLsid()==null) {
                throw new IllegalArgumentException("jobInput.lsid=null");
            }
            this.currentUser=currentUser;
            this.jobInput=jobInput;
        }
        
        public TaskInfo getTaskInfo() {
            return taskInfo;
        }
        
        //legacy code from RunTaskHelper
        private synchronized void initTaskInfo(final GetTaskStrategy getTaskStrategy) {
            this.taskInfo=getTaskStrategy.getTaskInfo(jobInput.getLsid());
            this.taskInfo.getParameterInfoArray();
        }
        
        private String getGpUrl() {
            URL url = ServerConfiguration.instance().getGenePatternURL();
            String rval=url.toString();
            return rval;
        }
        
        private void initParameterValues() throws Exception {
            //initialize a map of paramName to ParameterInfo 
            final Map<String,ParameterInfo> paramInfoMap=new HashMap<String,ParameterInfo>();
            for(ParameterInfo pinfo : taskInfo.getParameterInfoArray()) {
                paramInfoMap.put(pinfo.getName(), pinfo);
            }
            
            if (jobInput.getParams()==null) {
                log.debug("jobInput.params==null");
                return;
            }
            
            //walk through the list of user-provided input values
            for(Entry<ParamId, Param> entry : jobInput.getParams().entrySet()) {
                final Param param=entry.getValue();                
                final ParamId id = param.getParamId();
                final ParameterInfo pinfo=paramInfoMap.get(id.getFqName());
                if (pinfo==null) {
                    log.error("Can't get pInfo for id="+id.getFqName());
                }
                else {
                    initParameterValue(pinfo, param);
                }
            } 
        }

        private void initParameterValue(final ParameterInfo pinfo, final Param param) throws Exception { 
            if (pinfo._isDirectory() || pinfo.isInputFile()) {
                HashMap attrs = pinfo.getAttributes();
                attrs.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
                attrs.remove(ParameterInfo.TYPE);
            }
            if (param.getValues()==null) {
                pinfo.setValue(null);
            }
            else if (param.getValues().size()==0) {
                pinfo.setValue("");
            }
            else if (param.getValues().size()==1) {
                pinfo.setValue(param.getValues().get(0).getValue());
            }
            else if (param.getValues().size()>1) {
                try {
                    String val=initFilelist(param);
                    pinfo.setValue(val);
                }
                catch (Exception e) {
                    log.error(e);
                    throw e;
                }
            }
        }
        
        private String initFilelist(final Param param) throws Exception {
            //TODO: need to handle external urls and server file paths
            
            List<GpFilePath> filepaths=extractFilelist(param); 
            //now, create a new filelist file, add it into the user uploads directory for the given job
            GpFilePath filelist=getFilelist(param);
            writeFilelist(filelist.getServerFile(), filepaths, false);
            
            //String rval="<GenePatternURL>"+filelist.getRelativeUri().toString();
            return filelist.getUrl().toExternalForm();
        }

        private GpFilePath getFilelist(final Param param) throws Exception {
            Context userContext=ServerConfiguration.Context.getContextForUser(currentUser);
            GpFilePath inputdir=jobInput.getInputFileDir();
            if (inputdir==null) {
                inputdir=jobInput.initInputFileDir(userContext);
            }
            String path=inputdir.getRelativePath();
            if (!path.endsWith("/")) {
                path+="/";
            }
            path+="filelist/"+param.getParamId().getFqName()+".filelist";
            File uploadFile=new File(path);
            GpFilePath filelist=GpFileObjFactory.getUserUploadFile(userContext, uploadFile);
            
            //if necessary, create the parent directory
            File serverFile=filelist.getServerFile();
            File parentDir=serverFile.getParentFile();
            if (parentDir == null) {
                throw new Exception("Error initializing input filelist file: parentDir==null, serverFile="+serverFile.getAbsolutePath());
            }
            if (!parentDir.exists()) {
                log.debug("creating input directory: "+parentDir.getAbsolutePath());
                boolean success=parentDir.mkdirs();
                if (success) {
                    log.debug("success");
                }
                else {
                    String message="failed to create input directory: "+parentDir.getAbsolutePath();
                    log.error(message);
                    throw new Exception(message);
                }
            }
            if (serverFile.exists()) {
                log.error("filelist file already exists: "+serverFile.getAbsolutePath());
                throw new Exception("filelist file already exists: "+serverFile.getPath());
            }
            
            return filelist;
        }

        private static void writeFilelist(File output, List<GpFilePath> files, boolean writeTimestamp) throws IOException {
            final String SEP="\t";
            FileWriter writer = null;
            BufferedWriter out = null;
            try {
                writer = new FileWriter(output);
                out = new BufferedWriter(writer);
                for(GpFilePath filePath : files) {
                    File file = filePath.getServerFile();
                    out.write(file.getAbsolutePath());
                    if (writeTimestamp) {
                        out.write(SEP); out.write("timestamp="+file.lastModified());
                        out.write(SEP); out.write(" date="+new Date(file.lastModified())+" ");
                    }
                    out.newLine();
                }
            }
            finally {
                if (out != null) {
                    out.close();
                }
            }
        }

        private List<GpFilePath> extractFilelist(final Param param) throws Exception {
            List<GpFilePath> filepaths=new ArrayList<GpFilePath>();
            final String gpUrl=getGpUrl();
            for(ParamValue pval : param.getValues()) {
                String value=pval.getValue();
                GpFilePath filepath = GpFileObjFactory.getRequestedGpFileObj(value);
                filepaths.add(filepath);
            }
            return filepaths;
        }
        
        private String submitJob() throws JobSubmissionException {
            ParameterInfo[] paramInfos = taskInfo == null ? null : taskInfo.getParameterInfoArray();
            paramInfos = paramInfos == null ? paramInfos = new ParameterInfo[0] : paramInfos;
            JobInfo job = submitJob(taskInfo.getID(), paramInfos);
            String jobId = "" + job.getJobNumber();
            return jobId;
        }

        private JobInfo submitJob(final int taskID, final ParameterInfo[] parameters) throws JobSubmissionException {
            AddNewJobHandler req = new AddNewJobHandler(taskID, currentUser, parameters);
            JobInfo jobInfo = req.executeRequest();
            return jobInfo;
        }


//        private void setParameterValues_orig(
//                /* HttpServletRequest request */
//                ) throws IOException {
//            String server = getGpUrl(); 
//            if (!server.endsWith("/")) {
//                server += '/';
//            }
//            List<ParameterInfo> missingParameters = new ArrayList<ParameterInfo>();
//            for (int i = 0; i < parameterInfoArray.length; i++) {
//                ParameterInfo pinfo = parameterInfoArray[i];
//                String value;
//                if (pinfo.isInputFile() || pinfo._isDirectory()) {
//                    value = inputFileParameters.get(pinfo.getName());
//                    if (value == null) {
//                        pinfo.getAttributes().put(ParameterInfo.TYPE, "");
//                    }
//                    if (value != null && !value.equals("")) {
//                        if (pinfo._isDirectory()) {
//                            GpFilePath directory = null;
//                            try {
//                                //TODO: improve this; it works on the first run, but
//                                //    ... the input param value changes on the job status page, (from a url to a server file),
//                                //    ... and reload job doesn't work as expected
//                                directory = GpFileObjFactory.getRequestedGpFileObj(value);
//                                value = directory.getServerFile().getAbsolutePath();
//                                inputFileParameters.put(pinfo.getName(), value);
//                            }
//                            catch (Exception e) {
//                                log.error("Could not get a GP file path to the directory " + value);
//                            }
//                        } 
//                        if (urlParameters.contains(pinfo.getName())) {
//                            HashMap attrs = pinfo.getAttributes();
//                            attrs.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
//                            attrs.remove(ParameterInfo.TYPE);
//                        } 
//                    }
//                }
//                else {
//                    value = requestParameters.get(pinfo.getName());
//                }
//
//                // look for missing required params
//                if ((value == null) || (value.trim().length() == 0)) {
//                    Map pia = pinfo.getAttributes();
//                    boolean isOptional = ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET])).length() > 0;
//                    if (!isOptional) {
//                        missingParameters.add(pinfo);
//                    }
//                }
//                pinfo.setValue(value);
//            }
//        } 
    }
    
}
