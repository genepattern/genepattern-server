package org.genepattern.server.webservice.server;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.soap.SOAPException;

import org.apache.axis.MessageContext;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.log4j.Category;
import org.genepattern.server.handler.AddNewJobHandler;
import org.genepattern.server.handler.GetJobStatusHandler;
import org.genepattern.server.webservice.GenericWebService;
import org.genepattern.webservice.FileWrapper;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.genepattern.webservice.AnalysisJob;

/**
 * Analysis Web Service.
 *
 * @author David Turner, Hui Gong
 * @version 1.1
 */

public class Analysis extends GenericWebService
{
    private MessageContext context = null;
    private static Category _cat = Category.getInstance(Analysis.class.getName());

    /**
     * Default constructor.
     * Constructs a <code>Analysis</code> web service object.
     */
    public Analysis()
    {
	Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are sometimes empty
    }

    /**
    * Gets the latest versions of all tasks
    * 
    * @return The latest tasks
    * @exception WebServiceException  If an error occurs
    */
    public TaskInfo[] getTasks() throws WebServiceException
    {
	Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are sometimes empty
		return new AdminService() {
			protected String getUserName() {
				return getUsernameFromContext();
			}
		}.getLatestTasksByName();
    }

    /**
     * Submits an analysis job to be processed.
     *
     * @param taskID the ID of the task to run.
     * @param parmInfo the parameters to the process
     * @return the job information for this process
     * @exception is thrown if problems are encountered
     */
    public JobInfo submitJob(int taskID, ParameterInfo[] parmInfo, DataHandler dataHandler)
    throws WebServiceException
    {
	Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are sometimes empty
        // get the username
        String username = getUsernameFromContext();
        JobInfo jobInfo = null;
        String dhFileName = null;
        File inputFile = null;
        String filename = "";
        if (dataHandler != null) {
            dhFileName = dataHandler.getName();
            inputFile = new File(dhFileName);
            filename = inputFile.getPath();
            _cat.debug("File Name: " + inputFile.getName());
            _cat.debug("File Path: " + inputFile.getPath());
        }
        try {
            AddNewJobHandler req = new AddNewJobHandler(taskID, username, parmInfo, filename);
            jobInfo = req.executeRequest();
        }  catch (org.genepattern.webservice.OmnigeneException oe) {
            _cat.error(oe.getMessage());
            throw new WebServiceException(oe.getMessage());
        }   catch (Throwable t) {
             _cat.error(t.getMessage());
            throw new WebServiceException(t.getMessage());
        }
        return jobInfo;
    }
    
 

    /**
     * Submits an analysis job to be processed.
     *
     * @param taskID the ID of the task to run.
     * @param parmInfo the parameters to process
     * @param files a HashMap of input files sent as attachments
     * @return the job information for this process
     * @exception is thrown if problems are encountered
     */
    public JobInfo submitJob(int taskID, ParameterInfo[] parameters, Map files)
    throws WebServiceException
    {
	Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are sometimes empty

        // get the username
        String username = getUsernameFromContext();

        JobInfo jobInfo = null;
        // find any input files and concat axis name with original file name.
	if (parameters != null)
        for (int x = 0; x < parameters.length; x++) {
    	    if (parameters[x].isInputFile() ) {
        		String orgFilename = parameters[x].getValue();
        		AttachmentPart ap = (AttachmentPart)files.get(orgFilename);
        		DataHandler dataHandler = null;
        		try {
        		    dataHandler = ap.getDataHandler();
        		}
        		catch (SOAPException se) {
        		    throw new WebServiceException("Error while processing files");
        		}
        		String newFilename = dataHandler.getName() + "_" + orgFilename;
        		File f = new File(dataHandler.getName());
			File newFile = new File(newFilename);
        		boolean renamed = f.renameTo(newFile);
        		//reset parameter's value with new filename
        		if (renamed) {
				parameters[x].setValue(newFilename);
			} else {
			    try {
				parameters[x].setValue(f.getCanonicalPath());
			    } catch (IOException ioe) {
				throw new WebServiceException(ioe.getMessage());
			    }
			}
    	    }
    	}
        
        
        try {
            AddNewJobHandler req = new AddNewJobHandler(taskID, username, parameters, "");
            jobInfo = req.executeRequest();
        }
        catch (org.genepattern.webservice.OmnigeneException oe) {
            _cat.error(oe.getMessage());
	    oe.printStackTrace();
            throw new WebServiceException(oe.getMessage());
        }
        catch (Throwable t) {
            _cat.error(t.getMessage());
	    t.printStackTrace();
            throw new WebServiceException(t.getMessage());
        }
        
        return jobInfo;
    }

    /**
     * Checks the status of a particular job.
     *
     * @param jobID the ID of the task to check
     * @return the job information
     * @exception is thrown if problems are encountered
     */
    public JobInfo checkStatus(int jobID) throws WebServiceException
    {
	Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are sometimes empty

        JobInfo jobInfo = null;

        try {
            GetJobStatusHandler req = new GetJobStatusHandler(jobID);
            jobInfo = req.executeRequest();
        }
        catch (org.genepattern.webservice.OmnigeneException oe) {
            _cat.error(oe.getMessage());
            throw new WebServiceException(oe.getMessage());
        }
        catch (Throwable t) {
            _cat.error(t.getMessage());
            t.printStackTrace();
            throw new WebServiceException(t.getMessage());
        }

        return jobInfo;
    }

    /**
     * Returns the results of a completed job.
     *
     * @param jobID the ID of the job that completed.
     * @return the DataHandler containing the results for this process
     * @exception is thrown if problems are encountered
     */
    public DataHandler getResults(int jobID) throws WebServiceException
    {
        JobInfo jobInfo = null;
        DataHandler data = null;

	Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are sometimes empty

        try {
            GetJobStatusHandler req = new GetJobStatusHandler(jobID);
            jobInfo = req.executeRequest();
            if(jobInfo!=null){
                data = new DataHandler(new FileDataSource(new File(jobInfo.getResultFileName())));
            }
        }
        catch (org.genepattern.webservice.OmnigeneException oe) {
            _cat.error(oe.getMessage());
            throw new WebServiceException(oe.getMessage());
        }
        catch (Throwable t) {
            _cat.error(t.getMessage());
            throw new WebServiceException(t.getMessage());
        }
        return data;
    }

    /**
     * Returns the result files of a completed job.
     *
     * @param jobID the ID of the job that completed.
     * @return the List of FileWrappers containing the results for this process
     * @exception is thrown if problems are encountered
     */
    public List getResultFiles(int jobID) throws WebServiceException
    {
	Thread.yield(); // JL: fixes BUG in which responses from AxisServlet are sometimes empty

        JobInfo jobInfo = null;
        ArrayList filenames = null;
        
        try {
            GetJobStatusHandler req = new GetJobStatusHandler(jobID);
            jobInfo = req.executeRequest();
        }
        catch (org.genepattern.webservice.OmnigeneException oe) {
            _cat.error(oe.getMessage());
            throw new WebServiceException(oe.getMessage());
        }
        catch (Throwable t) {
            _cat.error(t.getMessage());
            throw new WebServiceException(t.getMessage());
        }
              
        if (jobInfo != null) {
            ParameterInfo[] parameters = jobInfo.getParameterInfoArray();
            if (parameters != null) {
                for (int x = 0; x < parameters.length; x++) {
                    if (parameters[x].isOutputFile() ) {
                        if (filenames == null)
                            filenames = new ArrayList();
                        filenames.add(System.getProperty("jobs") + "/" + parameters[x].getValue());
                    }
                }
            }
        }
        
        
        ArrayList list = null;
        if (filenames != null) {
            list = new ArrayList(filenames.size());
        
            for (Iterator iterator = filenames.iterator(); iterator.hasNext();) {
                String fn = (String)iterator.next();
		File f = new File(fn);
                DataHandler dataHandler = new DataHandler(new FileDataSource(fn));
                list.add(new FileWrapper(dataHandler.getName(), dataHandler, f.length(), f.lastModified()));
            }
        }
                                
        return list;
    }
   
   
    /**
    * Deletes the all the input and output files for the given job and removes the job from the stored history.
    *
    * @param jobId the job id
    */
    public void deleteJob(int jobId) throws WebServiceException {
       try {
          File jobDir = new File(org.genepattern.server.genepattern.GenePatternAnalysisTask.getJobDir(String.valueOf(jobId)));
          File[] files = jobDir.listFiles();
          if(files!=null) {
             for(int i = 0; i < files.length; i++) {
                files[i].delete();
                org.genepattern.server.indexer.Indexer.deleteJobFile(jobId, files[i].getName());
             }
          }
          jobDir.delete();
          org.genepattern.server.ejb.AnalysisJobDataSource ds = org.genepattern.server.util.BeanReference.getAnalysisJobDataSourceEJB();
          ds.deleteJob(jobId);
       } catch(Exception e) {
          throw new WebServiceException(e);
       }
    }
    
    /**
    *
    * Deletes the given output files for the given job
    *
    * @param jobId the job id
    * @param fileNames the file names to delete
    */
    public void deleteJobOutputFiles(int jobId, String[] fileNames) {
       String jobDir = org.genepattern.server.genepattern.GenePatternAnalysisTask.getJobDir(String.valueOf(jobId));
       if (fileNames != null) {
			for (int j = 0; j < fileNames.length; j++) {
				String name = fileNames[j];
				File file = new File(jobDir, name);
            if(file.exists()) {
               file.delete();
            }
            try {
                org.genepattern.server.indexer.Indexer.deleteJobFile(jobId, name);
            } catch (IOException ioe) {
               // ignore Lucene Lock obtain timed out exceptions
               _cat.debug(ioe + " while deleting search indices for job " + jobId);
            }
				
			}
		}
    }
    
    /**
    *
    * Gets the jobs for the current user
    *
    * @return the jobs
    */
    public AnalysisJob[] getJobs() throws WebServiceException {
       try {
         org.genepattern.server.ejb.AnalysisJobDataSource ds = org.genepattern.server.util.BeanReference.getAnalysisJobDataSourceEJB();
         JobInfo[] jobs = ds.getJobInfo(getUsernameFromContext());
         String server = (String) MessageContext.getCurrentContext().getProperty("transport.url");
        
         java.net.URL url = new java.net.URL(server);
         server = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();  
         
         AnalysisJob[] analysisJobs = new AnalysisJob[jobs.length];
         for(int i = 0; i < jobs.length; i++) {
            String taskName = "TODO"; // FIXME
            AnalysisJob analysisJob = new AnalysisJob(server, taskName, jobs[i]);
            analysisJob.setLSID("TODO");
            analysisJobs[i] = analysisJob;
         }
         return analysisJobs;
       } catch(Exception e) {
          throw new WebServiceException(e);  
       }
    }

    /**
     * Returns the username trying to access this service.  The username is retrieved
     * from the incoming soap header.
     *
     * @return a String containing the username or an empty string if one not found.
     */
    private String getUsernameFromContext()
    {
        // get the context then the username from the soap header
	context = MessageContext.getCurrentContext();
        String username = context.getUsername();
        if (username == null)
            username = "";
        return username;
    }


}

