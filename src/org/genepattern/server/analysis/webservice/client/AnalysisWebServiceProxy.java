package org.genepattern.server.analysis.webservice.client;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.encoding.ser.ArrayDeserializerFactory;
import org.apache.axis.encoding.ser.ArraySerializerFactory;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.encoding.ser.DateDeserializerFactory;
import org.apache.axis.encoding.ser.DateSerializerFactory;
import org.apache.axis.encoding.ser.JAFDataHandlerDeserializerFactory;
import org.apache.axis.encoding.ser.JAFDataHandlerSerializerFactory;
import org.apache.axis.encoding.ser.MapDeserializerFactory;
import org.apache.axis.encoding.ser.MapSerializerFactory;
import org.apache.log4j.Category;
import org.genepattern.analysis.JobInfo;
import org.genepattern.analysis.ParameterInfo;
import org.genepattern.analysis.TaskInfo;
import org.genepattern.analysis.TaskInfoAttributes;
import org.genepattern.analysis.WebServiceException;
import org.genepattern.server.webservice.FileWrapper;
import org.genepattern.server.webservice.UnavailableException;
//import edu.mit.broad.gp.ws.*;

/**
 * A proxy class for the Analysis Web Service.
 *
 * @author David Turner, Hui Gong
 * @version 1.1
 */

public class AnalysisWebServiceProxy
{
    private static Category cat = Category.getInstance(AnalysisWebServiceProxy.class.getName());

    private Service service;
    private Call call;
    private URL serviceURL;

    private QName qnParmInfoArray = null;
    private QName qnFile = null;
    private QName qnJobInfo = null;
    
    private String username = null;
    private String password = null;
    
    

    /**
     * Constructs an AnalysisWebServiceProxy object.
     */
    public AnalysisWebServiceProxy(String url)
    {
        // initialize proxy
        init(url);
    }
    

    /**
     * Constructs an AnalysisWebServiceProxy object.
     */
    public AnalysisWebServiceProxy(String url, String username, String password)
    {
        this.username = username;
        this.password = password;
        
        // initialize proxy
        init(url);
    }


    /**
     * Initialize the proxy.
     */
    private void init(String url)
    {
        try {
				synchronized(getClass()) {
				  service = new Service();
				}
            call = (Call)service.createCall();
            this.serviceURL = new URL(url);
			  
        }
        catch (MalformedURLException me)
        {
            this.serviceURL = null;
        }
        catch (Exception e)
        {
			  e.printStackTrace();
        }

        call.setTargetEndpointAddress(this.serviceURL);
        call.setMaintainSession(true);
        call.setUsername(this.username);
        call.setPassword(this.password);

        // Register type mappings
        QName qnTaskInfo = new QName("Analysis", "TaskInfo");
        call.registerTypeMapping(TaskInfo.class,
                                qnTaskInfo,
                                new BeanSerializerFactory(TaskInfo.class, qnTaskInfo),
                                new BeanDeserializerFactory(TaskInfo.class, qnTaskInfo));

        QName qnTaskInfoArray = new QName("Analysis", "TaskInfoArray");
        call.registerTypeMapping(TaskInfo[].class,
                                qnTaskInfoArray,
                                new ArraySerializerFactory(),
                                new ArrayDeserializerFactory());

        QName qnTaskInfoAttributes = new QName("Analysis", "TaskInfoAttributes");
        call.registerTypeMapping(TaskInfoAttributes.class,
    		                    qnTaskInfoAttributes,
    		                    new MapSerializerFactory(TaskInfoAttributes.class, qnTaskInfoAttributes),
    		                    new MapDeserializerFactory(TaskInfoAttributes.class, qnTaskInfoAttributes));

        QName qnParmInfo = new QName("Analysis", "ParmInfo");
        call.registerTypeMapping(ParameterInfo.class,
                                qnParmInfo,
                                new BeanSerializerFactory(ParameterInfo.class, qnParmInfo),
                                new BeanDeserializerFactory(ParameterInfo.class, qnParmInfo));

        qnParmInfoArray = new QName("Analysis", "ParmInfoArray");
        call.registerTypeMapping(ParameterInfo[].class,
                                qnParmInfoArray,
                                new ArraySerializerFactory(),
                                new ArrayDeserializerFactory());

        qnJobInfo = new QName("Analysis", "JobInfo");
        call.registerTypeMapping(JobInfo.class,
                                qnJobInfo,
                                new BeanSerializerFactory(JobInfo.class, qnJobInfo),
                                new BeanDeserializerFactory(JobInfo.class, qnJobInfo));

        //add Date
        QName date = new QName("Analysis", "Date");
        call.registerTypeMapping(Date.class, date,
                                new DateSerializerFactory(Date.class, date),
                                new DateDeserializerFactory(Date.class, date));


        qnFile = new QName("Analysis", "DataHandler");
        call.registerTypeMapping(DataHandler.class,
                                qnFile,
                                JAFDataHandlerSerializerFactory.class,
                                JAFDataHandlerDeserializerFactory.class);

        QName qnFileWrapper = new QName("Analysis", "FileWrapper");
        call.registerTypeMapping(FileWrapper.class,
                                qnFileWrapper,
                                new BeanSerializerFactory(FileWrapper.class, qnFileWrapper),
                                new BeanDeserializerFactory(FileWrapper.class, qnFileWrapper));
    }

    /**
     * Returns all the available tasks that can be used.
     */
    public TaskInfo[] getTasks() throws WebServiceException
    {
        call.setOperationName(new QName("Analysis","getTasks") );
        call.removeAllParameters();
        TaskInfo[] resp = null;
        try {
            resp = (TaskInfo[])call.invoke(new Object[] {});
        }
        catch (java.rmi.RemoteException re) {
            re.printStackTrace();
            throw new WebServiceException(re.getMessage(), re.detail);
        }

        return resp;
    }

    /**
     * Submits a job to be processed.
     */
    public JobInfo submitJob(int taskID, ParameterInfo[] parmInfo, File file) throws WebServiceException
    {
        call.setOperationName(new QName("Analysis","submitJob") );
        call.removeAllParameters();

        DataHandler dh = new DataHandler(new FileDataSource(file));

        JobInfo resp = null;
        call.setUsername(this.username);
        call.setPassword(this.password);
        try {
            resp = (JobInfo)call.invoke(new Object[] {new Integer(taskID), parmInfo, dh});
        }
        catch (java.rmi.RemoteException re) {
            throw new WebServiceException(re.getMessage());
        }

        return resp;
    }

    /**
     * Submits a job to be processed.
     */
    public JobInfo submitJob(int taskID, ParameterInfo[] parameters) 
        throws WebServiceException
    {
        call.setOperationName(new QName("Analysis","submitJob") );
        call.removeAllParameters();

        HashMap files = null;
        //System.out.println("Parameters:");
        // loop through parameters array looking for any parm of type FILE
        for (int x = 0; x < parameters.length; x++) {
            final ParameterInfo param = parameters[x];
            if ( parameters[x].isInputFile() ) {
        		String filename = parameters[x].getValue();
        		DataHandler dataHandler = new DataHandler(new FileDataSource(filename));
        		if (filename != null) {
        		    if (files == null) {
            			files = new HashMap();
        		    }
        		    parameters[x].setValue(dataHandler.getName()); // pass only name & not path
        		    files.put(dataHandler.getName(), dataHandler);
        		}
            }
        }
                
        JobInfo resp = null;
        call.setUsername(this.username);
        call.setPassword(this.password);
        try {
            resp = (JobInfo)call.invoke(new Object[] {new Integer(taskID), parameters, files});
        }
        catch (java.rmi.RemoteException re) {
            throw new WebServiceException(re.getMessage());
        }

        return resp;
    }

    /**
     * Checks the status of a job.
     */
    public JobInfo checkStatus(int jobID) throws WebServiceException
    {
        call.setOperationName(new QName("Analysis","checkStatus") );
        call.removeAllParameters();

        JobInfo resp = null;
        try {
            resp = (JobInfo)call.invoke(new Object[] {new Integer(jobID)});
        }
        catch (java.rmi.RemoteException re) {
		System.out.println("IN PROXY: " + re);

            throw new WebServiceException(re.getMessage(), re.getCause());
        }

        return resp;
    }

    /**
     * Returns the results of a completed job.
     */
    public DataHandler getResults(int jobID) throws WebServiceException
    {
        call.setOperationName(new QName("Analysis","getResults") );
        call.removeAllParameters();
        call.setReturnType(qnFile);

        DataHandler resp = null;
        try {
            resp = (DataHandler)call.invoke(new Object[] {new Integer(jobID)});
        }
        catch (java.rmi.RemoteException re) {
            throw new WebServiceException(re.getMessage());
        }

        return resp;
    }

    /**
     * Returns the filenames that were generated by a completed job.
     */
    public String[] getResultFiles(int jobID) throws WebServiceException
    {
        call.setOperationName(new QName("Analysis","getResultFiles") );
        call.removeAllParameters();
        call.addParameter("jobid", XMLType.XSD_INT , ParameterMode.IN );
        call.setReturnClass(java.util.ArrayList.class);
        
        ArrayList list = null;
        
        try {
            list = (ArrayList)call.invoke(new Object[] {new Integer(jobID)});
        }
        catch (java.rmi.RemoteException re) {
            throw new WebServiceException(re.getMessage());
        }
        
        String[] filenames = null;

        if (list != null) {
            filenames = new String[list.size()];
            int i = 0;
            for (Iterator iterator = list.iterator(); iterator.hasNext();) {
                FileWrapper fh = (FileWrapper)iterator.next();            
                String newFilename = fh.getDataHandler().getName() + "_" + fh.getFilename();
                File f = new File(fh.getDataHandler().getName());
                f.renameTo(new File(newFilename));                
                filenames[i] = newFilename;
                i++;
                f = null;
            }
        }

        return filenames;
    }


    /**
     * Pings the service to see if it's alive.
     */
    public String ping() throws UnavailableException
    {
        call.setOperationName(new QName("Analysis","ping") );
    	call.removeAllParameters();
    	Object response = null;
            String pingResponse = null;
    	try {
    	    response = call.invoke(new Object[] { });
    	    pingResponse = (String)response;
    	}
    	catch (java.rmi.RemoteException re) {
    	    throw new UnavailableException(re.getMessage());
    	}

    	return pingResponse;
    }


}
