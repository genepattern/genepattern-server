package org.genepattern.server.webservice.server;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis.MessageContext;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.webapp.AbstractPipelineCodeGenerator;
import org.genepattern.server.webservice.WebServiceErrorMessageException;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;


/**
 * TaskIntegrator Web Service. Do a Thread.yield at beginning of each method-
 * fixes BUG in which responses from AxisServlet are sometimes empty
 * 
 * @author Joshua Gould
 */
public class TaskIntegrator
   implements ITaskIntegrator {
   protected String getUserName() {
      MessageContext context=MessageContext.getCurrentContext();
      String username=context.getUsername();
      if(username==null) {
         username="";
      }
      return username;
   }

   public DataHandler exportToZip(String taskName)
                           throws WebServiceException {
      return exportToZip(taskName, false);
   }
                           
   public DataHandler exportToZip(String taskName, boolean recursive)
                           throws WebServiceException {
      try {
         Thread.yield();
         String username=getUserName();
         
         org.genepattern.server.process.ZipTask zt=null;
         if(recursive) {
            zt = new org.genepattern.server.process.ZipTaskWithDependents(); 
         } else {
            zt = new org.genepattern.server.process.ZipTask();
         }
         File zipFile=zt.packageTask(taskName, username);
         // FIXME delete zip file after returning
         DataHandler h=new DataHandler(new FileDataSource(zipFile.getCanonicalPath()));
         return h;
      } catch(Exception e) {
         throw new WebServiceException("while exporting to zip file", e);
      }
   }

   private File getFile(DataHandler dh) {
      javax.activation.DataSource ds = dh.getDataSource();
      if(ds instanceof FileDataSource) { // if local
           return ((FileDataSource) ds).getFile();
      }
      // if through SOAP org.apache.axis.attachments.ManagedMemoryDataSource
      return new File(dh.getName());  
   }
   
   public String importZip(DataHandler handler, int privacy)
                    throws WebServiceException {
       return importZip(handler, privacy, true);                
   }
   
   public String importZip(DataHandler handler, int privacy, boolean recursive)
                    throws WebServiceException {
      Vector vProblems=null;
      String lsid=null;
      try {
         Thread.yield();
         String username=getUserName();
         File axisFile= getFile(handler);
         File zipFile=new File(handler.getName() + ".zip");
         axisFile.renameTo(zipFile);
         String path=zipFile.getCanonicalPath();
         // replace task, do not version lsid or replace the lsid in the zip
         // with a local one
         try {
            lsid=GenePatternAnalysisTask.installNewTask(path, username, 
                                                        privacy, recursive);
         } catch(TaskInstallationException tie) {
            vProblems=tie.getErrors();
         }
      } catch(Exception e) {
         throw new WebServiceException("while importing from zip file", e);
      }
      if(vProblems!=null&&vProblems.size()>0) {
         throw new WebServiceErrorMessageException(vProblems);
      }
      return lsid;
   }
   
   

   public String importZipFromURL(String url, int privacy, boolean recursive)
                           throws WebServiceException {
      File zipFile=null;
      FileOutputStream os=null;
      InputStream is=null;
      Vector vProblems=null;
      String lsid=null;
      try {
         String username=getUserName();
         if(url.startsWith("file://")) {
            String fileStr=url.substring(7, url.length());
            zipFile=new File(fileStr);
         } else {
            zipFile=File.createTempFile("gpz", ".zip");
            if(username==null) {
               username=getUserName();
            }
            os=new FileOutputStream(zipFile);
            is=new java.net.URL(url).openStream();
            byte[] buf=new byte[30000];
            int i;
            while((i=is.read(buf, 0, buf.length))>0) {
               os.write(buf, 0, i);
            }
         }
         String path=zipFile.getCanonicalPath();
         // replace task, do not version lsid or replace the lsid in the zip
         // with a local one
         lsid=GenePatternAnalysisTask.installNewTask(path, username, privacy, recursive);
      } catch(TaskInstallationException tie) {
         throw new WebServiceErrorMessageException(tie.getErrors());
      }
       catch(IOException ioe) {
         throw new WebServiceException("while importing zip from " + url, ioe);
      } finally {
         if(zipFile!=null) {
            zipFile.delete();
         }
         if(is!=null) {
            try {
               is.close();
            } catch(IOException x) {
            }
         }
         if(os!=null) {
            try {
               os.close();
            } catch(IOException x) {
            }
         }
      }
      return lsid;
   }

   public String importZipFromURL(String url, int privacy) throws WebServiceException {
   	return importZipFromURL(url, privacy, true);
   }

   public String[] getSupportFileNames(String lsid)
                                throws WebServiceException {
      if(lsid==null||lsid.equals("")) {
          throw new WebServiceException("Invalid LSID");
      }
      try {
         Thread.yield();
         String attachmentDir=GenePatternAnalysisTask.getTaskLibDir(lsid);
         File dir=new File(attachmentDir);
         String[] oldFiles=dir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
               return (!name.endsWith(".old"));
            }
         });
         return oldFiles;
      } catch(Exception e) {
         throw new WebServiceException("while getting support filenames", e);
      }
   }

   public DataHandler getSupportFile(String lsid, String fileName)
                              throws WebServiceException {
      if(lsid==null||lsid.equals("")) {
          throw new WebServiceException("Invalid LSID");
      }
      try {
         Thread.yield();
         String attachmentDir=GenePatternAnalysisTask.getTaskLibDir(lsid);
         File dir=new File(attachmentDir);
         File f = new File(dir, fileName);
         if(!f.exists()) {
            throw new WebServiceException("File " + fileName + " not found.");
         }
         return new DataHandler(new FileDataSource(f));
      } catch(Exception e) {
         throw new WebServiceException("while getting support file " + 
                                       fileName + " from " + lsid, e);
      }
   }

   public DataHandler[] getSupportFiles(String lsid, String[] fileNames)
                                 throws WebServiceException {
      try {
         if(lsid==null||lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
         }
         DataHandler[] dhs=new DataHandler[fileNames.length];
         String attachmentDir=GenePatternAnalysisTask.getTaskLibDir(lsid);
         File dir=new File(attachmentDir);
         for(int i=0; i<fileNames.length; i++) {
            File f=new File(dir, fileNames[i]);
            if(!f.exists()) {
                 throw new WebServiceException("File " + fileNames[i] + " not found.");
            }
            dhs[i]=new DataHandler(new FileDataSource(f));
         }
         return dhs;
      } catch(Exception e) {
         throw new WebServiceException("Error getting support files.", e);
      }
   }

   public long[] getLastModificationTimes(String lsid, String[] fileNames)
                                   throws WebServiceException {
      try {
         if(lsid==null||lsid.equals("")) {
            throw new WebServiceException("Invalid LSID");
         }
         long[] modificationTimes=new long[fileNames.length];
         String attachmentDir=GenePatternAnalysisTask.getTaskLibDir(lsid);
         File dir=new File(attachmentDir);
         for(int i=0; i<fileNames.length; i++) {
            File f=new File(dir, fileNames[i]);
            modificationTimes[i]=f.lastModified();
         }
         return modificationTimes;
      } catch(Exception e) {
         throw new WebServiceException("Error getting support files.", e);
      }
   }

   public DataHandler[] getSupportFiles(String lsid)
                                 throws WebServiceException {
      if(lsid==null||lsid.equals("")) {
          throw new WebServiceException("Invalid LSID");
      }
      String[] files=getSupportFileNames(lsid);
      DataHandler[] dhs=new DataHandler[files.length];
      for(int i=0; i<files.length; i++) {
         dhs[i]=getSupportFile(lsid, files[i]);
      }
      return dhs;
   }

   public String modifyTask(int accessId, String taskName, String description, 
                            ParameterInfo[] parameterInfoArray, 
                            Map taskAttributes, DataHandler[] dataHandlers, 
                            String[] fileNames)
                     throws WebServiceException {
      Vector vProblems=null;
      String lsid=null;
      String username=getUserName();
      try {
         Thread.yield();
         if(taskAttributes==null) {
            taskAttributes=new HashMap();
         }
         //   COMMAND_LINE, TASK_TYPE, CLASSNAME, CPU_TYPE, OS, JVM_LEVEL, LANGUAGE, VERSION, AUTHOR, USERID, PRIVACY, QUALITY, PIPELINE_SCRIPT, LSID, SERIALIZED_MODEL
         //GPConstants.TASK_INFO_ATTRIBUTES[i];
         if(parameterInfoArray==null) {
            parameterInfoArray=new ParameterInfo[0];
         }
         lsid=(String) taskAttributes.get(GPConstants.LSID);
         // if an LSID is set, make sure that it is for the current authority, not the task's source, since it is now modified
         if(lsid!=null&&lsid.length()>0) {
            try {
               LSID l=new LSID(lsid);
               String authority=LSIDManager.getInstance().getAuthority();
               if(!l.getAuthority().equals(authority)) {
                  System.out.println(
                         "TaskIntegrator.modifyTask: resetting authority from " + 
                         l.getAuthority() + " to " + authority);
                  lsid="";
                  taskAttributes.put(GPConstants.LSID, lsid);
                  // change owner to current user
                  String owner=(String) taskAttributes.get(GPConstants.USERID);
                  if(owner.length()>0) {
                     owner=" (" + owner + ")";
                  }
                  owner=username + owner;
                  taskAttributes.put(GPConstants.USERID, owner);
               }
            } catch(MalformedURLException mue) {
            }
         }
         if(false&&
            GenePatternAnalysisTask.taskExists(lsid!=null ? lsid : taskName, 
                                               null)) {
            // task exists, update it
            lsid=GenePatternAnalysisTask.updateTask(taskName, description, 
                                                    "edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask", 
                                                    parameterInfoArray, // FIXME
                                                    
                                                    new TaskInfoAttributes(
                                                           taskAttributes), 
                                                    username, accessId);
         } // task does not already exist, treat as new 
         else {
            lsid=GenePatternAnalysisTask.installNewTask(taskName, description, 
                                                        "edu.mit.wi.omnigene.service.analysis.genepattern.GenePatternAnalysisTask", 
                                                        parameterInfoArray, // FIXME
                                                        
                                                        new TaskInfoAttributes(
                                                               taskAttributes), 
                                                        username, accessId);
         }
         taskAttributes.put(GPConstants.LSID, lsid); // update so that upon return, the LSID is the new one
         String attachmentDir=GenePatternAnalysisTask.getTaskLibDir(taskName, 
                                                                    lsid, 
                                                                    username);
         File dir=new File(attachmentDir);
         for(int i=0, length=dataHandlers!=null ? dataHandlers.length : 0;
             i<length;
             i++) {
            DataHandler dataHandler=dataHandlers[i];
            File f= getFile(dataHandler);
            File newFile=new File(dir, fileNames[i]);
            if(!f.getParentFile().getParent().equals(dir.getParent())) {
               System.out.println(
                      "TaskIntegrator.modifyTask: renaming " + 
                      f.getCanonicalPath() + " to " + 
                      newFile.getCanonicalPath());
               System.out.println(
                      f.getParentFile().getParent() + " vs. " + 
                      dir.getParent());
               f.renameTo(newFile);
            } else {
               // copy file, leaving original intact
               byte[] buf=new byte[100000];
               int j;
               FileOutputStream os=new FileOutputStream(newFile);
               FileInputStream is=new FileInputStream(f);
               while((j=is.read(buf, 0, buf.length))>0) {
                  os.write(buf, 0, j);
               }
               is.close();
               os.close();
            }
         }
      } catch(TaskInstallationException tie) {
         throw new WebServiceErrorMessageException(tie.getErrors());
      }
       catch(Exception e) {
         throw new WebServiceException("in modifyTask", e);
      }
      return lsid;
   }

   public String deleteFiles(String lsid, String[] fileNames)
                      throws WebServiceException {
                         if(lsid==null || lsid.equals("")) {
                            throw new WebServiceException("Invalid LSID");  
                         }
      try {
         String username=getUserName();
         TaskInfo taskInfo=new LocalAdminClient(username).getTask(lsid);
         String taskName=taskInfo.getName();
		
         String attachmentDir=GenePatternAnalysisTask.getTaskLibDir(taskName, 
                                                                    lsid, 
                                                                    username);

	 
	 Vector lAttachments = new Vector(Arrays.asList(getSupportFileNames(lsid))); // Vector of String
	 Vector lDataHandlers = new Vector(Arrays.asList(getSupportFiles(lsid)));    // Vector of DataHandler

         for(int i=0; i<fileNames.length; i++) {
		int exclude = lAttachments.indexOf(fileNames[i]);
		lAttachments.remove(exclude);
		lDataHandlers.remove(exclude);
	 }

	 String newLSID = modifyTask(taskInfo.getAccessId(), taskInfo.getName(), taskInfo.getDescription(), 
                            taskInfo.getParameterInfoArray(), 
                            taskInfo.getTaskInfoAttributes(), (DataHandler[])lDataHandlers.toArray(new DataHandler[0]), 
                            (String[])lAttachments.toArray(new String[0]));
         return newLSID;
      } catch(Exception e) {
      e.printStackTrace();
         throw new WebServiceException("while deleting files from " + lsid, e);
      }
   }

   public void deleteTask(String lsid) throws WebServiceException {
      if(lsid==null || lsid.equals("")) {
         throw new WebServiceException("Invalid LSID");
      }
       
      String username=getUserName();
      try {
         TaskInfo taskInfo = new LocalAdminClient(username).getTask(lsid);
         
         if(taskInfo==null) {
            throw new WebServiceException("no such task " + lsid);  
         }
         
         String attachmentDir=GenePatternAnalysisTask.getTaskLibDir(taskInfo);
         GenePatternAnalysisTask.deleteTask(lsid);
         File dir=new File(attachmentDir);
         // clear out the directory
         File[] oldFiles=dir.listFiles();
         for(int i=0, length=oldFiles!=null ? oldFiles.length : 0;
             i<length;
             i++) {
            oldFiles[i].delete();
         }
         dir.delete();
      } catch(Throwable e) {
         throw new WebServiceException("while deleting task " + lsid, e);
      }
   }

   // copy the taskLib entries to the new directory
   private void cloneTaskLib(String oldTaskName, String cloneName, String lsid, 
                             String cloneLSID, String username)
                      throws Exception {
      String dir =GenePatternAnalysisTask.getTaskLibDir(oldTaskName, lsid, username);
      String newDir=GenePatternAnalysisTask.getTaskLibDir(cloneName, cloneLSID, username);
      String[] oldFiles=getSupportFileNames(lsid);
      byte[] buf=new byte[100000];
      int j;
      for(int i=0; i<oldFiles.length; i++) {
         FileOutputStream os=new FileOutputStream(new File(newDir, oldFiles[i]));
         FileInputStream is=new FileInputStream(new File(dir, oldFiles[i]));
         while((j=is.read(buf, 0, buf.length))>0) {
            os.write(buf, 0, j);
         }
         is.close();
         os.close();
      }
   }

   public String cloneTask(String oldLSID, String cloneName)
                    throws WebServiceException {
      String userID=getUserName();
      String requestURL=null;
      try {
         TaskInfo taskInfo=null;
         try {
            requestURL="http://" + java.net.InetAddress.getLocalHost().getCanonicalHostName() + ":" + System.getProperty("GENEPATTERN_PORT");
            taskInfo=new LocalAdminClient(userID).getTask(oldLSID);
         } catch(Exception e) {
            throw new WebServiceException(e);
         }
         taskInfo.setName(cloneName);
         taskInfo.setAccessId(GPConstants.ACCESS_PRIVATE);
         taskInfo.setUserId(userID);
         TaskInfoAttributes tia=taskInfo.giveTaskInfoAttributes();
         tia.put(GPConstants.USERID, userID);
         tia.put(GPConstants.PRIVACY, GPConstants.PRIVATE);
         oldLSID=(String)tia.remove(GPConstants.LSID);
         if(tia.get(GPConstants.TASK_TYPE).equals(
                   GPConstants.TASK_TYPE_PIPELINE)) {
            URL request=null;
            try {
               request=new URL(requestURL);
            } catch(MalformedURLException mue) {
               throw new WebServiceException(mue.getMessage());
            }
            String language="R";
            AbstractPipelineCodeGenerator codeGenerator=null;
            PipelineModel model=null;
            model=PipelineModel.toPipelineModel(
                         (String) tia.get(GPConstants.SERIALIZED_MODEL));
            Class clsPipelineCodeGenerator=Class.forName(
                                                  AbstractPipelineCodeGenerator.class
                                   .getPackage().getName() + "." + language + 
                                                  "PipelineCodeGenerator");
            Constructor consAbstractPipelineCodeGenerator=clsPipelineCodeGenerator.getConstructor(
                                                                 new Class[] {
               PipelineModel.class, String.class, int.class, String.class, 
               Collection.class
            });
            codeGenerator=(AbstractPipelineCodeGenerator) consAbstractPipelineCodeGenerator.newInstance(
                                 new Object[] {
               model, request.getHost(), new Integer(request.getPort()), 
				 System.getProperty("GenePatternURL") +
                                 "makePipeline.jsp?" + 
                                 request.getFile(), null
            });
            // update the pipeline model with the new name
            model.setName(cloneName);
            // update the task with the new model and command line
            TaskInfoAttributes newTIA=codeGenerator.getTaskInfoAttributes();
            tia.put(GPConstants.SERIALIZED_MODEL, model.toXML());
            tia.put(language + GPConstants.INVOKE, codeGenerator.invoke()); // save invocation string in TaskInfoAttributes
            tia.put(GPConstants.COMMAND_LINE, 
                    newTIA.get(GPConstants.COMMAND_LINE));
         }
         String newLSID=modifyTask(GPConstants.ACCESS_PRIVATE, cloneName, 
                                taskInfo.getDescription(), 
                                taskInfo.getParameterInfoArray(), tia, null, 
                                null);
         cloneTaskLib(taskInfo.getName(), cloneName, oldLSID, newLSID, userID);
         return newLSID;
      } catch(Exception e) {
         System.err.println(e.getMessage());
         e.printStackTrace();
         throw new WebServiceException(e);
      }
   }

   public DataHandler[] getDocFiles(String lsid)
                             throws WebServiceException {
                                
      String userID=getUserName();
      String taskLibDir=null;
      try {
         taskLibDir=GenePatternAnalysisTask.getTaskLibDir(lsid);
      } catch(Exception e) {
         throw new WebServiceException(e);
      }
      File[] docFiles=new File(taskLibDir).listFiles(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            return GenePatternAnalysisTask.isDocFile(name) && !name.equals("version.txt");
         }
      });
      boolean hasDoc=docFiles!=null&&docFiles.length>0;
      if(hasDoc) {
         // put version.txt last, all others alphabetically
         Arrays.sort(docFiles, 
                     new Comparator() {
            public int compare(Object o1, Object o2) {
               if(((File) o1).getName().equals("version.txt")) {
                  return 1;
               }
               return ((File) o1).getName().compareToIgnoreCase(((File) o2).getName());
            }
         });
      }
      if(docFiles==null) {
         return new DataHandler[0];
      }
      DataHandler[] dh=new DataHandler[docFiles.length];
      for(int i=0, length=docFiles.length; i<length; i++) {
         dh[i]=new DataHandler(new FileDataSource(docFiles[i]));
      }
      return dh;
   }
}