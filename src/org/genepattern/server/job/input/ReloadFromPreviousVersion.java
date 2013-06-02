package org.genepattern.server.job.input;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.genepattern.server.FileUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.webservice.ParameterInfo;

/**
 * Helper class for reloading a job run on a previous (<= 3.5.0) version of GP
 * on a newer (>=3.6.0) version of GP.
 * 
 * For the use-case: on 3.5.0 run a job and upload the file as part of the job input form.
 *     Reload that job after updating the server to 3.6.0 or greater.
 * @author pcarr
 *
 */
public class ReloadFromPreviousVersion {
    final static private Logger log = Logger.getLogger(ReloadFromPreviousVersion.class);

    final private boolean isWebUpload;
    final private String inputFormValue;

    public ReloadFromPreviousVersion(final String originalJobId, final ParameterInfo pinfo) {
        if (originalJobId==null) {
            throw new IllegalArgumentException("originalJobId==null");
        }
        //expecting an Integer value
        try {
            Integer.parseInt(originalJobId);
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("originalJobId='"+originalJobId+"' is not an integer: "+e.getLocalizedMessage());
        }
        //initialize in constructor
        if (pinfo == null) {
            log.debug("pinfo==null, ignore");
            isWebUpload=false;
            inputFormValue="";
            return;
        }
        if (!pinfo.isInputFile()) {
            log.debug("pinfo is not an input file, ignore");
            isWebUpload=false;
            inputFormValue="";
            return;
        }
        
        //assume it's an input file, check to see if it's a legacy web upload
        final String originalValue=pinfo.getValue();
        final File originalServerFile=new File(originalValue);
        
        final File inputFileParent = originalServerFile.getParentFile();
        final File inputFileGrandParent = inputFileParent == null ? null : inputFileParent.getParentFile();
        final File webUploadDirectory = new File(System.getProperty("java.io.tmpdir"));
        
        this.isWebUpload=FileUtil.fileEquals(inputFileGrandParent, webUploadDirectory);

        if (isWebUpload) {
            log.debug("isWebUpload, originalValue="+originalValue);
            String fileParam = "";
            if (inputFileParent != null) {
                fileParam += inputFileParent.getName() + "/";
            }
            fileParam += originalServerFile.getName();
            //url encode fileParam
            try {
                fileParam = URLEncoder.encode(fileParam, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                log.error("Error encoding inputFile param, '"+fileParam+"' "+e.getLocalizedMessage(), e);
            } 
            
            final URL gpURL=ServerConfiguration.instance().getGenePatternURL();
            String gpUrlStr=gpURL.toExternalForm();
            if (!gpUrlStr.endsWith("/")) {
                gpUrlStr += "/";
            }
            
            this.inputFormValue=gpUrlStr + "getFile.jsp?task=job="+originalJobId+"&file="+fileParam;
            log.debug("inputFormValue="+inputFormValue);
        }
        else {
            this.inputFormValue="";
        }
    }
    
    public boolean isWebUpload() {
        return isWebUpload;
    }
    
    public String getInputFormValue() {
        return inputFormValue;
    }

}
