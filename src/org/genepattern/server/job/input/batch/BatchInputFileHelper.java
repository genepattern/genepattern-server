package org.genepattern.server.job.input.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamValue;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for preparing a batch of jobs, when batching over input files and directories.
 * 
 * @author pcarr
 *
 */
public class BatchInputFileHelper {
    private static final Logger log = Logger.getLogger(BatchInputFileHelper.class);

    /**
     * Get the GpFilePath for a batch input directory, if and only if, the given value
     * is a valid batch input directory. Otherwise, return null.
     * @param value
     * @return
     */
    public static GpFilePath initGpFilePath(final String value) {
        final boolean includeExternalUrl=false;
        return initGpFilePath(value, includeExternalUrl);
    }

    /**
     * Get the GpFilePath for a batch input directory, if and only if, the given value
     * is a valid batch input directory. Otherwise, return null.
     * @param value
     * @return
     */
    public static GpFilePath initGpFilePath(final String value, final boolean includeExternalUrl) {
        GpFilePath gpPath=null;
        URL externalUrl=JobInputHelper.initExternalUrl(value);
        if (externalUrl!=null) {
            //it's an externalURL
            if (!includeExternalUrl) {
                return null;
            }
            else {
                return new ExternalFile(externalUrl);
            }
        }

        try {
            gpPath = GpFileObjFactory.getRequestedGpFileObj(value);
            return gpPath;
        }
        catch (Exception e) {
            log.debug("getRequestedGpFileObj("+value+") threw an exception: "+e.getLocalizedMessage(), e);
            //ignore
        }

        //if we are here, it could be a server file path
        File serverFile=new File(value);
        gpPath = ServerFileObjFactory.getServerFile(serverFile);
        return gpPath;
    }

    private static FilenameFilter listFilesFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            if (".svn".equals(name)) {
                return false;
            }
            return true;
        }
    };

    /**
     * Get the list of matching input files in the given batch directory,
     * only include files which are valid input types for the given pinfo.
     * 
     * @param pinfo, the ParameterInfo instance for the batch input parameter.
     * @param batchDir, the value of the batch input directory.
     * @return
     * @throws GpServerException
     */
    public static List<GpFilePath> getBatchInputFiles(final ParameterInfo pinfo, final GpFilePath batchDir) throws GpServerException {
        final String parentUrl;
        try {
            parentUrl=batchDir.getUrl().toExternalForm();
        }
        catch (Exception e) {
            throw new GpServerException("Error initializing parentUrl: "+batchDir.getRelativeUri().toString());
        }
        List<GpFilePath> filePaths = new ArrayList<GpFilePath>();
        File[] files = batchDir.getServerFile().listFiles(listFilesFilter);
        for(File file : files) {
            final String fileUrl = parentUrl + UrlUtil.encodeURIcomponent( file.getName() );
            try {
                GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj(fileUrl);
                filePath.initMetadata();
                if (accept(pinfo,filePath)) {
                    filePaths.add(filePath);
                }
            }
            catch (Throwable t) {
                log.error("Server error preparing batch input fileUrl="+fileUrl, t);
            }
        }
        return filePaths;
    }

    /**
     * Is the given input value a valid batch input parameter for the given parameter?
     * 
     * Policy (circa GP <=3.6.0):
     *    if the pinfo is of type DIRECTORY, accept only input values which are directories
     *    if the pinfo is of type FILE,
     *        if it has fileFormats, accept any file which matches one of the file formats
     *        if it has not fileFormats, match any file which is not a directory
     *    otherwise, it's not a match
     * 
     * @param pinfo
     * @param inputValue
     * @return
     */
    private static boolean accept(final ParameterInfo pinfo, final GpFilePath inputValue) {
        //special-case for DIRECTORY input parameter
        if (pinfo._isDirectory()) {
            if (inputValue.isDirectory()) {
                return true;
            }
            else {
                return false;
            }
        }
        
        if (inputValue.isDirectory()) {
            //the value is a directory, but the parameter type is not a directory
            log.debug("Not implemented!");
            return false;
        }
        
        List<String> fileFormats = SemanticUtil.getFileFormats(pinfo);
        if (fileFormats.size()==0) {
            //no declared fileFormats, acceptAll
            return true;
        }
        final String kind=inputValue.getKind();
        if (fileFormats.contains(kind)) {
            return true;
        }
        final String ext = inputValue.getExtension();
        if (fileFormats.contains(ext)) {
            return true;
        }
        return false;
    }

    public static String getBaseFilename(final GpFilePath file) {
        if (file==null) {
            log.error("file==null");
            return "";
        }
        String filename=null;       
        if (file.getName()!=null) {
            filename=file.getName();
        }
        else {
            try {
                URL url=file.getUrl();
                if (url != null) {
                    String path=url.getPath();
                    if (path != null) {
                        filename=new File(path).getName();
                    }
                }
            }
            catch (Exception e) {
                log.error(e);
            }
        }
        if (filename==null) {
            log.error("error getting filename for file="+file);
            return "";
        }
        int periodIndex = filename.lastIndexOf('.');
        if (periodIndex > 0) {
            return filename.substring(0, periodIndex);
        }
        else {
            return filename;
        }
    }
    
    private final Context userContext;
    private final JobInput jobInput;
    private Map<String,List<GpFilePath>> batchValues=new LinkedHashMap<String,List<GpFilePath>>();
    private final Map<String,ParameterInfoRecord> paramInfoMap;

    public BatchInputFileHelper(final Context userContext, final TaskInfo taskInfo) {
        this.userContext=userContext;
        this.jobInput=new JobInput();
        this.jobInput.setLsid(taskInfo.getLsid());
        this.paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
    }

    public void addValue(final String id, final String value) {
        final boolean isBatchParam=false;
        Param param=jobInput.getParam(id);
        if (param != null) {
            if (param.isBatchParam()) {
                log.error("adding a non-batch value to an existing batch parameter");
            }
        }
        jobInput.addValue(id, value, isBatchParam);
    }

    /**
     * Helper method, based on the value provided from the web upload form
     * or the REST API request, 
     * figure out whether to add a single batch value as an external url,
     *    or as a listing of the contents of a local directory
     *    or as an individual file.
     * @param id
     * @param value
     */
    public void addBatchValue(final String id, final String value) throws GpServerException {
        URL externalUrl=JobInputHelper.initExternalUrl(value);
        if (externalUrl != null) {
            addBatchExternalUrl(id, externalUrl);
            return;
        }

        final GpFilePath gpPath=BatchInputFileHelper.initGpFilePath(value);
        if (gpPath == null) {
            throw new GpServerException("batch input not supported for param="+id+", value="+value);
        }
        if (!gpPath.getServerFile().exists()) {
            throw new GpServerException("batch input file does not exist for param="+id+", value="+value);
        }

        if (gpPath.isDirectory()) {
            addBatchDirectory(id, gpPath);
            return;
        }
        else {
            addBatchFile(id, gpPath);
            return;
        }
    }

    private void addBatchExternalUrl(final String id, final URL externalUrl) throws GpServerException {
        addBatchFile(id, new ExternalFile(externalUrl));
    }

    private void addBatchFile(final String id, final GpFilePath batchFile) throws GpServerException {
        Param param=jobInput.getParam(id);
        if (param!=null) {
            if (!param.isBatchParam()) {
                throw new GpServerException("Error adding batch value to a non-batch parameter: ");
            }
        }
        final List<GpFilePath> batchFiles;
        if (!batchValues.containsKey(id)) {
            batchFiles=new ArrayList<GpFilePath>();
            batchValues.put(id, batchFiles);
        }
        else {
            batchFiles=batchValues.get(id);
        }
        batchFiles.add(batchFile);
    }

    private void addBatchDirectory(final String id, final GpFilePath batchDir) throws GpServerException {
        final ParameterInfoRecord record=paramInfoMap.get(id);
        if (record==null) {
            final String message="No matching parameter, '"+id+"', for task="+jobInput.getLsid();
            log.error(message);
            throw new GpServerException(message);
        }
        final List<GpFilePath> batchValues=listBatchDir(record.getFormal(), batchDir);
        if (batchValues==null || batchValues.size()==0) {
            log.debug("No matching batchValues for "+id+"="+batchDir);
            return;
        }
        for(final GpFilePath batchValue : batchValues) {
            try {
                addBatchFile(id, batchValue);
            }
            catch (Exception e) {
                log.error(e);
            }
        }
    }

    /**
     * 
     * @param formalParam
     * @param initialValue
     * @return
     */
    private List<GpFilePath> listBatchDir(final ParameterInfo formalParam, final GpFilePath batchInputDir) throws GpServerException {
        if (batchInputDir==null) {
            throw new IllegalArgumentException("batchInputDir==null");
        }
        if (formalParam==null) {
            throw new IllegalArgumentException("formalParam==null");
        }
        if (!batchInputDir.canRead(userContext.isAdmin(), userContext)) {
            throw new GpServerException("The current user ("+userContext.getUserId()+") doesn't have permission to read the batch input directory: "+batchInputDir);
        }
        if (!batchInputDir.getServerFile().exists()) {
            //another error
            throw new GpServerException("Can't read batch input directory: "+batchInputDir.getRelativeUri().toString());
        }
        if (!batchInputDir.getServerFile().isDirectory()) {
            final String errorMessage=""+batchInputDir+" is not a valid batch input directory";
            throw new GpServerException(errorMessage);
        }

        final List<GpFilePath> batchInputFiles=BatchInputFileHelper.getBatchInputFiles(formalParam, batchInputDir);
        if (batchInputFiles.size()==0) {
            throw new GpServerException("No matching input files in batch input directory: "+batchInputDir);
        }
        return batchInputFiles;
    }


    //
    // code for dealing with multiple batch parameters
    //
    public List<JobInput> prepareBatch() throws GpServerException {
        if (batchValues.size()==0) {
            //no batch params
            List<JobInput> rval=new ArrayList<JobInput>();
            rval.add( new JobInput(jobInput) );
            return rval;
        }
        else if (batchValues.size()==1) {
            //one batch param
            final boolean isBatchParam=true;
            for(final Entry<String,List<GpFilePath>> entry : batchValues.entrySet()) {
                for(final GpFilePath inputFile : entry.getValue()) {
                    final String value;
                    try {
                        value=inputFile.getUrl().toExternalForm();
                    }
                    catch (Throwable t) {
                        throw new GpServerException(t.getLocalizedMessage(),t);
                    }
                    jobInput.addValue(entry.getKey(), value, isBatchParam);
                }
            }
        }
        else {
            //multi batch params
            Set<String> commonBasenames=computeCommonBasenames();
            //special-case: no matching parameters, because commonBasenames is empty
            if (commonBasenames.isEmpty()) {
                throw new GpServerException("No matching input files for multi-batch job.");
            }
            appendBatchValuesToJobInputTemplate(commonBasenames);
        }
        return prepareBatch(jobInput);
    }

    /**
     * For multi batch parameters ... first get the intersection of common basenames.
     * @return
     */
    public Set<String> computeCommonBasenames() {
        boolean first=true;
        final Set<String> commonBasenames = new LinkedHashSet<String>();
        for(final Entry<String,List<GpFilePath>> entry : batchValues.entrySet()) {
            final Set<String> basenames=new LinkedHashSet<String>();
            for(GpFilePath inputFile : entry.getValue()) {
                final String basename = BatchInputFileHelper.getBaseFilename(inputFile);
                basenames.add(basename);
            }
            if (first) {
                first=false;
                commonBasenames.addAll(basenames);
            }
            else {
                commonBasenames.retainAll(basenames);
                if (commonBasenames.isEmpty()) {
                    //no matching basenames!
                    break;
                }
            }
        }
        return commonBasenames;
    }

    /**
     * After all user input values have been added from the web input form 
     * update the jobInput template by adding any matching batch values.
     */
    public void appendBatchValuesToJobInputTemplate(final Set<String> commonBasenames) throws GpServerException {
        //if there are any common basenames, only add the parameters which match
        //ensure the values are added in the correct order
        for(final Entry<String,List<GpFilePath>> entry : batchValues.entrySet()) {
            SortedMap<String, GpFilePath> sortedValues=new TreeMap<String, GpFilePath>();
            for(final GpFilePath inputFile : entry.getValue()) {
                final String basename=BatchInputFileHelper.getBaseFilename(inputFile);
                if (commonBasenames.contains(basename)) {
                    sortedValues.put(basename,inputFile);
                }
            }
            for(final Entry<String,GpFilePath> next : sortedValues.entrySet()) {
                final GpFilePath inputFile = next.getValue();
                final String value;
                try {
                    value=inputFile.getUrl().toExternalForm();
                }
                catch (Throwable t) {
                    throw new GpServerException(t.getLocalizedMessage(),t);
                }
                final boolean isBatchParam=true;
                jobInput.addValue(entry.getKey(), value, isBatchParam);
            }
        }
    }

    public static List<JobInput> prepareBatch(final JobInput jobInput) throws GpServerException {
        List<JobInput> batchInputs=new ArrayList<JobInput>();
        int numJobs=jobInput.getNumBatchJobs();
        if (numJobs==1) {
            batchInputs.add(jobInput);
            return batchInputs;
        }

        //it is a batch of jobs
        for(int idx=0; idx<numJobs; ++idx) {
            JobInput nextJob=prepareJobInput(idx, jobInput);
            batchInputs.add(nextJob);
        }
        return batchInputs;
    }

    private static JobInput prepareJobInput(final int idx, final JobInput template) {
        //start with a copy of the jobInput template
        final JobInput nextJobInput = new JobInput(template);
        //then replace batch parameters with the values for this particular (idx) batch job
        for(final Param batchParamIn : template.getBatchParams()) {
            final Param batchParam=new Param(batchParamIn.getParamId(), false);
            final ParamValue batchParamValue = template.getParam(batchParamIn.getParamId()).getValues().get(idx);
            batchParam.addValue(batchParamValue);
            nextJobInput.setValue(batchParamIn.getParamId(), batchParam);
        }
        return nextJobInput;
    }
}
