package org.genepattern.server.executor.serverthread;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.genepattern.webservice.JobStatus;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * For the PathSeq pipeline, conditional execution of the RepeatMaskerRead module,
 * only if the previous step has a '.unmasked' output file.
 * Otherwise, simply copy the input file into the job result directory of this job.
 * 
 * @author pcarr
 */
public class RepeatMaskerStep extends AbstractServerTask {
    String unmasked = null;
    String orig = null;

    public String call() throws Exception {
        //cmdLine= --orig=<orig> --unmasked=<unmasked>
        // TODO Auto-generated method stub
        parseArgs();
        run();
        return JobStatus.FINISHED;
    }
    
    /**
     * Parse the command line.
     */
    private void parseArgs() throws Exception { 
        OptionParser parser = new OptionParser();
        parser.accepts( "orig", "the original .fq1 file input to the repeat masker" ).withRequiredArg().ofType(String.class).required();
        parser.accepts( "unmasked", "the .unmasked file output from the repeat masker" ).withRequiredArg().ofType(String.class);
        try {
            OptionSet options = parser.parse( args );
            orig = (String) options.valueOf("orig");
            if (options.has("unmasked")) {
                unmasked = (String) options.valueOf("unmasked");
            }
        }
        catch (Exception e) {
            throw e;
        }
        catch (Throwable t) {
            throw new Exception(t);
        }
    }
    
    private void run() throws Exception {
        if (unmasked != null && unmasked.trim().length() != 0) {
            runRepeatMasker();
        }
        else {
            copyOrigFq1();
        }
    }
    
    private void runRepeatMasker() {
    }
    
    private void copyOrigFq1() throws Exception {
        File origFile = new File(orig);
        
        File cwd = new File("test").getParentFile().getCanonicalFile();
        File origDir = origFile.getParentFile().getCanonicalFile();
        if (cwd.equals( origDir )) {
            //don't make a copy if it's already in the current directory
            return;
        }
        File toFile = new File( cwd, origFile.getName() );
        boolean clobberTo = true;
        copyFile(origFile, toFile, clobberTo);
    }
    
    public static boolean copyFile(File from, File to, boolean clobberTo) throws Exception {
        if (!from.exists()) {
            throw new Exception(from + " doesn't exist to copy.");
        }
        if (to.exists()) {
            if (!clobberTo) {
                throw new Exception(to + " already exists.");
            }
            to.delete();
        }
        BufferedInputStream is = null;
        BufferedOutputStream os = null;
        try {
            is = new BufferedInputStream(new FileInputStream(from));
            os = new BufferedOutputStream(new FileOutputStream(to));
            int i;
            while ((i = is.read()) != -1) {
                os.write(i);
            }
            to.setLastModified(from.lastModified());
            return true;
        } 
        catch (IOException e) {
            throw new Exception("Error copying " + from.getAbsolutePath() + " to " + to.getAbsolutePath() + ": " + e.getMessage());
        } 
        finally {
            if (is != null) {
                try {
                    is.close();
                } 
                catch (IOException e1) {
                }
            }
            if (os != null) {
                try {
                    os.close();
                } 
                catch (IOException e1) {
                }
            }
        }
    }
}
