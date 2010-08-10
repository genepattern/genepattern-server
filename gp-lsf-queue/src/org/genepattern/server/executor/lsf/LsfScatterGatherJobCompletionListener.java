package org.genepattern.server.executor.lsf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.mit.broad.core.Main;
import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJobDAO;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;

/**
 * Special handler for scatter-gather jobs. 
 * Use the following configuration properties:<pre>
     lsf.firehose.scatter.gather: true
     lsf.firehose.scatter.gather.output.filename: scatter.gather.out
     lsf.firehose.scatter.gather.output.filename.lsf.0: scatter.gather.lsf.out.0
     lsf.firehose.scatter.gather.output.filename.lsf.1: scatter.gather.lsf.out.1
 * </pre>
 * 
 * @author pcarr
 */
public class LsfScatterGatherJobCompletionListener implements JobCompletionListener  {
    private static Logger log = Logger.getLogger(LsfScatterGatherJobCompletionListener.class);

    //the output of the initial scatter job, must contain a single line with the LSF ID of the LSF job to wait for
    private static final String outputFilename = "scatter.gather.out";
    //the LSF output of the initial scatter job
    private static final String lsfOut0 = ".lsf.out.0";
    //the LSF output of the wait for job
    private static final String lsfOut1 = ".lsf.out.1";
    //the LSF error output of the wait for job
    private static final String lsfErr1 = ".lsf.err.1";
    
    private static final LsfJobDAO dao = new LsfJobDAO();
    
    //add another entry in the broad core database so that we are notified when the given job completes
    public void jobCompleted(LsfJob job) throws Exception { 
        int waitForLsfId = -1;
        try {
            waitForLsfId = getWaitForLsfId(job);
            addNewJobEntry(waitForLsfId, job);
        }
        catch (Exception e) {
            //TODO: call handleJobCompletion with ERROR status
            log.error("TODO: call handleJobCompletion with ERROR status", e);
            return;
        }
    }

    /**
     * Expect to find an output file named 'scatter.gather.out' in the working directory for the job,
     * which contains a single line with the LSF job id for the job to wait for.
     * 
     * @param job
     * @return
     * @throws Exception
     */
    private int getWaitForLsfId(LsfJob job) throws Exception {
        if (job == null) {
            throw new Exception("null arg");
        }
        if (job.getWorkingDirectory() == null) {
            throw new Exception("null job.workingDirectory");
        }
        //TODO: use configuration parameter
        File outputFile = new File(job.getWorkingDirectory(), outputFilename);
        if (!outputFile.canRead()) {
            throw new Exception("Can't read output file: "+outputFile.getAbsolutePath());
        }

        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            fileReader = new FileReader(outputFile);
            reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            return Integer.parseInt(line);
        }
        catch (FileNotFoundException e) {
            throw e;
        }
        catch (IOException e) {
            throw e;
        }
        catch (NumberFormatException e) {
            throw e;
        }
        finally {
            if (fileReader != null) {
                try {
                    fileReader.close();
                }
                catch (IOException e) {
                    log.error("error closing file: "+outputFile.getAbsolutePath(), e);
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * Add a new entry to the broad core lsf job table.
     * 
     * @param lsfJobId, the lsf id of the job to wait for
     * @param job0, the originating job
     */
    private void addNewJobEntry(int waitForLsfJobId, LsfJob origJob) {
        Session session = null;
        Transaction tx = null;
        try {
            session = Main.getInstance().getHibernateSession();
            tx = session.beginTransaction();

            LsfJob waitForJob = new LsfJob();
            waitForJob.setWorkingDirectory(origJob.getWorkingDirectory());
            waitForJob.setOutputFilename(lsfOut1);
            waitForJob.setErrorFileName(lsfErr1);
            //note: use setName to hold onto the GP job id
            waitForJob.setName(origJob.getName());
            waitForJob.setQueue(origJob.getQueue());
            waitForJob.setProject(origJob.getProject());
            waitForJob.setLsfJobId(""+waitForLsfJobId);

            //TODO: correct this assumption: hard coding the callback for the subsequent job to the 'normal' handler
            waitForJob.setCompletionListenerName(LsfJobCompletionListener.class.getCanonicalName());
            waitForJob.setGapServerId(origJob.getGapServerId());
            waitForJob.setStatus("UNKWN");
            waitForJob.setUpdatedDate(new Date());
        
            dao.save(waitForJob);
            tx.commit();
        }
        catch (Throwable t) {
            log.error("Error handling scatter gather job submission: "+t.getLocalizedMessage(), t);
            if (tx != null) {
                tx.rollback();
            }
        }
        finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
