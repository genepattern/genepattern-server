package edu.mit.genome.gp;

import junit.framework.TestCase;
import edu.mit.wi.omnigene.framework.analysis.*;
import edu.mit.wi.omnigene.framework.webservice.*;
import edu.mit.wi.omnigene.util.*;
import edu.mit.wi.omniview.analysis.*;
import edu.mit.wi.omnigene.framework.analysis.webservice.client.*;
import java.io.*;
import java.net.*;
import java.util.*;
import edu.mit.broad.util.*;

public class TestCallTasks extends TestCase{
    static Map serviceMap = new HashMap();
    static RequestHandlerFactory rhFactory = null;
    static String dataRootDir = null;
    static boolean debug = false;
	 static String userName = "GenePattern";
	 static String password = null;
	 
    public TestCallTasks(String name){
	super(name);
    }

    protected void setUp() { 
	try {
		final String separator = java.io.File.separator;
		
		debug = Boolean.getBoolean("DEBUG");
		

		if (System.getProperty("test.data.root") != null) {
		    dataRootDir = System.getProperty("test.data.root");
		    if (!dataRootDir.endsWith(separator)) dataRootDir += separator;
		} else {
		    dataRootDir = "../../testdata/";
		}
		
		
		if (System.getProperty("omnigene.test.conf") == null) {
		    final String home = System.getProperty("user.home");
		    final String base = separator+"gp"+separator+"resources"+separator;
		    final String conf_location = home+base;
		    logDebug("Setting omnigene.conf='"+conf_location+"'");
		    System.setProperty("omnigene.conf", conf_location);
		} else {
		    System.setProperty("omnigene.conf", System.getProperty("omnigene.test.conf"));
		}
		
		
		if (isDebug()){
		    PropertyFactory property = PropertyFactory.getInstance();
		    Properties p = property.getProperties("omnigene.properties");
		    String url = p.getProperty("analysis.service.URL");
		    logDebug("Using config from '"+System.getProperty("omnigene.test.conf")+"'");
		    logDebug("Testing against server '"+url+"'");
		
		}
		RequestHandlerFactory rhFactory = RequestHandlerFactory.getInstance(userName, password);
		
		Vector services = rhFactory.getAllServices();
		for (Iterator iter = services.iterator(); iter.hasNext(); ){
		    AnalysisService svc = (AnalysisService)	iter.next();
		    TaskInfo tinfo = svc.getTaskInfo();
		    String name = tinfo.getName();
		    
		    serviceMap.put(name, svc);
		}
	    
	    } catch (Exception e){
		e.printStackTrace();
	    }  
	
    }
    
    protected void tearDown(){
	String tmpDirName = System.getProperty("java.io.tmpdir");
		
	File tmpDir = new File(tmpDirName);
	File[] axisFiles = tmpDir.listFiles(new FilenameFilter(){
		public boolean accept(File dir, String name){
		    return name.startsWith("Axis");
		}

	    });
	for (int i=0; i < axisFiles.length; i++){
	    logDebug("Deleting Axis Temp file==> " + axisFiles[i].getAbsolutePath() + " "+axisFiles[i].delete());
	}

    }


    public boolean isDebug(){
	return debug;
    }
    
    public void logDebug(String str){
	if (isDebug()) System.out.println(str);
    }

    /**
     * submit a job based on a service and its parameters
     */
    public AnalysisJob submitJob(AnalysisService svc, ParameterInfo[] parmInfos) throws Exception{
	TaskInfo tinfo = svc.getTaskInfo();

	RequestHandler handler = rhFactory.getRequestHandler(svc.getName());
	
	//final ParameterFormatConverter converter = new ParameterFormatConverter();
	final JobInfo job = handler.submitJob(tinfo.getID(), parmInfos);
	final AnalysisJob aJob = new AnalysisJob( svc.getName(), svc.getTaskInfo().getName(), job);
	return aJob;
    }

    /**
     * Wait for a job to end or error.  This loop will wait for a max of 36 seconds
     * for 10 tries doubling the wait time each time after 6 seconds
     * to a max of a 16 seconds wait
     */
    public JobInfo waitForErrorOrCompletion(AnalysisJob job) throws Exception{
	
	int maxtries = 20;
	int sleep = 1000;

	return waitForErrorOrCompletion(job, maxtries, sleep);
    }

    public JobInfo waitForErrorOrCompletion(AnalysisJob job, int maxTries, int initialSleep) throws Exception{
	String status = "";
	JobInfo info = null;
	int count = 0;
	int sleep = initialSleep;

	while (!(status.equalsIgnoreCase("ERROR") || (status.equalsIgnoreCase("Finished")))) {
	    count++;
	    Thread.currentThread().sleep(sleep);
	    info = rhFactory.getInstance().getRequestHandler(										                        job.getSiteName()).checkStatus(job.getJobInfo().getJobNumber());
	    status = info.getStatus();
	    if (isDebug()) System.out.print(".");
	    	    
	    if ( (count > (maxTries/2)) && (sleep <= (initialSleep * 8))) sleep *=2;
	 
	    if (count > maxTries) break;
	}
	logDebug(" "+ status);
	return info;
    }

    public String[] getResultFiles(AnalysisJob job) throws Exception{
		
	RequestHandler handler  = rhFactory.getInstance().getRequestHandler( job.getSiteName());
	String[] files = handler.getResultFiles(job.getJobInfo().getJobNumber());
	

	return files;
    }
    
    public String getFileWithNameFragment(AnalysisJob job, String name) throws Exception{
	String files[] = getResultFiles(job);
	if (files == null) return null;

	for (int i=0; i < files.length; i++){
	    logDebug("looking for: " + name + " found: " + files[i]);
	    if (files[i].lastIndexOf(name) >= 0) return files[i];
	}
	return null;
    }

    public void assertNoStddErrFileGenerated(AnalysisJob job) throws Exception{
	String file = getFileWithNameFragment(job, "stderr");
	assertTrue("STDERR present", file == null);
    }

    public void assertFileWithNameGenerated(AnalysisJob job, String name) throws Exception{
	String file = getFileWithNameFragment(job, name);
	assertTrue("MISSING output "+name, file != null);
    }

    public ParameterInfo getInputFileParam(String paramName, String fileName){
	ParameterInfo infile =  new ParameterInfo(paramName, getDataFilePath(fileName),ParameterInfo.FILE_TYPE);
	infile.setAsInputFile();
	return infile;
    }

    public String getDataRootDir(){
	return dataRootDir;
    }
    
    public String getDataFilePath(String name){
	return getDataRootDir() + name;
    }


  
}






 
