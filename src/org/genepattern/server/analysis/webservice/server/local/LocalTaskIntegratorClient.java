package org.genepattern.server.analysis.webservice.server.local;



import java.io.File;

import javax.activation.*;

import org.genepattern.server.analysis.*;
import org.genepattern.server.analysis.webservice.server.*;
import org.genepattern.server.webservice.WebServiceException;
import org.genepattern.util.GPConstants;


/**
 * @author Joshua Gould
 */
public class LocalTaskIntegratorClient {
   ITaskIntegrator service;


   public LocalTaskIntegratorClient(final String userName) {
      service=new TaskIntegrator() {
         protected String getUserName() {
            return userName;
         }
      };
   }

   public String modifyTask(int accessId, String taskName, String description, 
                            ParameterInfo[] parameterInfoArray, 
                            java.util.Map taskAttributes, 
                            javax.activation.DataHandler[] dataHandlers, 
                            String[] fileNames)
                     throws WebServiceException {
      return service.modifyTask(accessId, taskName, description, 
                                parameterInfoArray, taskAttributes, 
                                dataHandlers, fileNames);
   }

   public String cloneTask(String lsid, String cloneName)
                    throws WebServiceException {
      return service.cloneTask(lsid, cloneName);
   }

   public DataHandler[] getSupportFiles(String lsid)
                                 throws WebServiceException {
      return service.getSupportFiles(lsid);
   }

   public String[] getSupportFileNames(String lsid)
                                throws WebServiceException {
      return service.getSupportFileNames(lsid);
   }

   public File[] getAllFiles(TaskInfo task)
                      throws WebServiceException {
      String taskId=(String) task.getTaskInfoAttributes().get(GPConstants.LSID);
      if(taskId==null) {
         taskId=task.getName();
      }
      DataHandler[] dh=service.getSupportFiles(taskId);
      File[] files=new File[dh.length];
      for(int i=0, length=dh.length; i<length; i++) {
         FileDataSource ds=(FileDataSource) dh[i].getDataSource();
         files[i]=ds.getFile();
      }
      return files;
   }

   public File[] getDocFiles(TaskInfo task)
                      throws WebServiceException {
      String taskId=(String) task.getTaskInfoAttributes().get(GPConstants.LSID);
      if(taskId==null) {
         taskId=task.getName();
      }
      DataHandler[] dh=service.getDocFiles(taskId);
      File[] files=new File[dh.length];
      for(int i=0, length=dh.length; i<length; i++) {
         FileDataSource ds=(FileDataSource) dh[i].getDataSource();
         files[i]=ds.getFile();
      }
      return files;
   }

   public void deleteTask(String lsid)
                   throws WebServiceException {
      service.deleteTask(lsid);
   }

   public String deleteFiles(String lsid, String[] fileNames)
                      throws WebServiceException {
      return service.deleteFiles(lsid, fileNames);
   }

   public String importZipFromURL(String url, int privacy)
                           throws WebServiceException {
      return service.importZipFromURL(url, privacy, true);
   }
	
   public String importZipFromURL(String url, int privacy, boolean recursive)
                           throws WebServiceException {
      return service.importZipFromURL(url, privacy, recursive);
   }

   public boolean isZipOfZips(String url) throws WebServiceException {
      File file = null;
      java.io.OutputStream os = null;
      java.io.InputStream is = null;
      boolean deleteFile = false;
      try {
        
         if(url.startsWith("file://")) {
            String fileStr=url.substring(7, url.length());
            file=new File(fileStr);
         } else {
            deleteFile = true;
            file=File.createTempFile("gpz", ".zip");
            os=new java.io.FileOutputStream(file);
            is=new java.net.URL(url).openStream();
            byte[] buf=new byte[100000];
            int i;
            while((i=is.read(buf, 0, buf.length))>0) {
               os.write(buf, 0, i);
            }
         }
      } catch(java.io.IOException ioe) {
         throw new WebServiceException(ioe);  
      } finally {
         if(os!=null) {
            try {
               os.close();
            } catch(java.io.IOException x){}
         }
         if(is!=null) {
            try {
               is.close();
            } catch(java.io.IOException x){}
         }
         
      }
      try { 
         return org.genepattern.server.analysis.TaskUtil.isZipOfZips(file);
      } catch(java.io.IOException ioe) {
         throw new WebServiceException(ioe);  
      } finally {
         if(deleteFile && file!=null) {
            file.delete();  
         }  
      }
   }
}