package org.genepattern.gpge.ui.tasks;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.gpge.ui.tasks.*;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;
import org.genepattern.gpge.ui.treetable.*;

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */
public class JobModel extends AbstractSortableTreeTableModel {
   String[] columnNames = {"Name", "Completed"};
   Class[] columnClasses = {org.jdesktop.swing.treetable.TreeTableModel.class, String.class};
   static JobModel instance = new JobModel();
   RootNode root = new RootNode();



   private JobModel() { }
   
   public void removeAll() {
      root.removeAllChildren();
      nodeStructureChanged(root);
   }


   public void addJobListener(JobListener l) {
      listenerList.add(JobListener.class, l);
   }


   public void removeJobListener(JobListener l) {
      listenerList.remove(JobListener.class, l);
   }


    /**
    *  Removes the given file from the model
    *
    * @param  serverFile  Description of the Parameter
    */
   public void remove(ServerFileNode serverFile) {
      JobNode node = (JobNode) serverFile.getParent();
      int serverFileIndex = node.getIndex(serverFile);
      node.remove(serverFileIndex);
      nodesWereRemoved(node, new int[]{serverFileIndex}, new Object[]{serverFile});
     
      
   }
   
   /**
    *  Deletes the given file from the server
    *
    * @param  serverFile  Description of the Parameter
    */
   public void delete(ServerFileNode serverFile) {
      JobNode node = (JobNode) serverFile.getParent();
      try {
         JobInfo jobInfo = node.job.getJobInfo();
         AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(node.job.getServer(), jobInfo.getUserId());
         String[] fileNames = {serverFile.name};

         proxy.deleteJobOutputFiles(jobInfo.getJobNumber(), fileNames);
         int serverFileIndex = node.getIndex(serverFile);
         node.remove(serverFileIndex);
         //if(node.getChildCount() == 0) {
        //    int index = root.getIndex(node);
        //    root.remove(index);
       //     nodesWereRemoved(root, new int[]{index}, new Object[]{node});
       //  } else {
            nodesWereRemoved(node, new int[]{serverFileIndex}, new Object[]{serverFile});
       //  }
      } catch(Exception e) {
         e.printStackTrace();
      }
   }


   /**
    *  Deletes the all the output files for a job from the server
    *
    * @param  node  Description of the Parameter
    */
   public void delete(JobNode node) {
      try {
         JobInfo jobInfo = node.job.getJobInfo();
         AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(node.job.getServer(), jobInfo.getUserId());
         proxy.deleteJob(jobInfo.getJobNumber());

         /*
             String[] fileNames = new String[node.getChildCount()];
             for(int i = 0; i < node.getChildCount(); i++) {
             ServerFileNode child = (ServerFileNode) node.getChildAt(i);
             fileNames[i] = child.name;
             }
             proxy.deleteJobOutputFiles(jobInfo.getJobNumber(), fileNames);
           */
         int index = root.getIndex(node);
         root.remove(index);
         nodesWereRemoved(root, new int[]{index}, new Object[]{node});
      } catch(Exception e) {
         e.printStackTrace();
      }

   }


   public void add(AnalysisJob job) {
      JobNode child = new JobNode(job);
      root.add(child);
      int[] newIndexs = new int[1];
      newIndexs[0] = root.getChildCount() - 1;
      Object[] p1 = {root};
      Object[] kids = {child};
      //  nodeStructureChanged(child);
      System.out.println("job added");
      Object[] path = getPathToRoot(root);

      final TreeModelEvent e = new TreeModelEvent(this, path);
      nodeStructureChanged(root);
      nodeStructureChanged(root);
      notifyJobAdded(job);
      // fireTreeStructureChanged(this, getPathToRoot(child));

   }


   public void jobCompleted(AnalysisJob job) {
      JobNode jobNode = findJobNode(job);
      int outputFiles = jobNode.getOutputFiles();
      int[] newIndexs = new int[outputFiles];
      for(int i = 0; i < outputFiles; i++) {
         newIndexs[i] = i;
      }
      // nodesWereInserted(jobNode, newIndexs);
      nodeStructureChanged(root);
      notifyJobCompleted(job);
   }


   public void jobStatusChanged(AnalysisJob job) {
      //nodesChanged(findJobNode(job), null);
      nodeStructureChanged(root);
      notifyJobStatusChanged(job);
   }


   public void sortOrderChanged(SortEvent e) {
      int column = e.getColumn();
      boolean ascending = e.isAscending();
      Vector children = root.getChildren();
      if(children == null) {
         return;
      }
      if(column == 0) {
         Collections.sort(children, new TaskNameComparator(ascending));
      } else {
         Collections.sort(children, new TaskDateComparator(ascending));
      }
      nodeStructureChanged(root);
   }


   protected void notifyJobAdded(AnalysisJob job) {
      Object[] listeners = listenerList.getListenerList();
      JobEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == JobListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new JobEvent(this, job);
            }

            ((JobListener) listeners[i + 1]).jobAdded(e);
         }
      }
   }


   protected void notifyJobStatusChanged(AnalysisJob job) {
      Object[] listeners = listenerList.getListenerList();
      JobEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == JobListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new JobEvent(this, job);
            }

            ((JobListener) listeners[i + 1]).jobStatusChanged(e);
         }
      }
   }


   protected void notifyJobCompleted(AnalysisJob job) {
      Object[] listeners = listenerList.getListenerList();
      JobEvent e = null;
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
         if(listeners[i] == JobListener.class) {
            // Lazily create the event:
            if(e == null) {
               e = new JobEvent(this, job);
            }

            ((JobListener) listeners[i + 1]).jobCompleted(e);
         }
      }
   }





   private JobNode findJobNode(AnalysisJob job) {
      for(int i = 0, size = root.getChildCount(); i < size; i++) {
         JobNode n = (JobNode) root.getChildAt(i);
         if(n.job == job) {
            return n;
         }
      }
      return null;
   }



   public void getJobsFromServer(String server, String username) {
      try {
         AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(server, username);
         AnalysisJob[] jobs = proxy.getJobs();
         for(int i = 0; i < jobs.length; i++) {
            JobNode node = new JobNode(jobs[i]);
            node.getOutputFiles();
            root.add(node);
         }
      } catch(Exception e) {
         e.printStackTrace();
      }
   }


   public Class getColumnClass(int column) {
      return columnClasses[column];
   }


   public Object getRoot() {
      return root;
   }



   public static JobModel getInstance() {
      return instance;
   }



   public int getColumnCount() {
      return columnNames.length;
   }


   public String getColumnName(int column) {
      return columnNames[column];
   }


   public Object getValueAt(Object node, int column) {
      if(node instanceof ServerFileNode) {
         ServerFileNode f = (ServerFileNode) node;
         switch (column) {
          case 0:
             return f.name;
          default:
             return null;
         }
      } else if(node instanceof JobNode) {
         JobNode j = (JobNode) node;
         switch (column) {
          case 0:
             return j.toString();
          default:
             JobInfo jobInfo = j.job.getJobInfo();
             if(!j.complete) {
                return jobInfo.getStatus();
             }

             Date d = jobInfo.getDateCompleted();
             return
                   java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(d);
         }
      }
      return null;
   }



   /**
    *  Description of the Class
    *
    * @author    Joshua Gould
    */
   public static class ServerFileNode extends DefaultMutableTreeNode implements Comparable {
      public final String name;
      public final int index;


      public ServerFileNode(String name, int index) {
         this.name = name;
         this.index = index;
      }



      public void download(File destination) throws IOException {
         JobNode parent = (JobNode) getParent();
         AnalysisJob job = parent.job;
         FileOutputStream fos = null;
         InputStream is = null;
         try {
            HttpURLConnection connection = (HttpURLConnection) parent.getURL(name).openConnection();
            if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
               throw new java.io.FileNotFoundException();
            }

            is = connection.getInputStream();
            byte[] b = new byte[100000];
            int bytesRead = 0;
            fos = new FileOutputStream(destination);
            while((bytesRead = is.read(b)) != -1) {
               fos.write(b, 0, bytesRead);
            }
         } finally {
            if(is != null) {
               try {
                  is.close();
               } catch(IOException x) {}
            }
            if(fos != null) {
               try {
                  fos.close();
               } catch(IOException x) {}
            }
         }
      }


      public String toString() {
         return name;
      }


      public int compareTo(Object other) {
         ServerFileNode node = (ServerFileNode) other;
         return this.name.compareTo(node.name);
      }


      public String getServerName() {
         JobNode parent = (JobNode) getParent();
         return parent.job.getJobInfo().getParameterInfoArray()[index].getValue();
      }


      public boolean getAllowsChildren() {
         return false;
      }


      public boolean isLeaf() {
         return true;
      }

   }


   /**
    *  Description of the Class
    *
    * @author    Joshua Gould
    */
   public static class PipelineNode extends DefaultMutableTreeNode {
      String pipelineName;


      public PipelineNode(String pipelineName, AnalysisJob[] jobs) {
         this.pipelineName = pipelineName;
         children = new Vector();
         for(int i = 0; i < jobs.length; i++) {
            children.add(new JobNode(jobs[i]));
         }

      }


      public String toString() {
         return pipelineName;
      }


      public boolean getAllowsChildren() {
         return true;
      }


      public boolean isLeaf() {
         return false;
      }

   }


   /**
    *  Description of the Class
    *
    * @author    Joshua Gould
    */
   public static class JobNode extends DefaultMutableTreeNode {

      public final AnalysisJob job;
      boolean complete = false;

      public boolean isComplete() {
         return complete;   
      }
      
      public JobNode(AnalysisJob job) {
         this.job = job;
      }


      public String toString() {
         return job.getTaskName() + " (" + job.getJobInfo().getJobNumber() + ")";
      }


      /**
       *  Returns the url to download the given file name.
       *
       * @param  fileName  The file name.
       * @return           The url to retrieve the file from.
       */
      public URL getURL(String fileName) {
         try {
            return new URL(job.getServer() + "/gp/retrieveResults.jsp?job=" + job.getJobInfo().getJobNumber() + "&filename=" + URLEncoder.encode(fileName, "UTF-8"));
         } catch(MalformedURLException x) {
            throw new Error(x);
         } catch(java.io.UnsupportedEncodingException uee) {
            throw new Error("Unable to encode " + fileName);
         }
      }


      public int getOutputFiles() {
         complete = true;
         int count = 0;
         ParameterInfo[] jobParameterInfo = job.getJobInfo().getParameterInfoArray();
         for(int j = 0; j < jobParameterInfo.length; j++) {
            if(jobParameterInfo[j].isOutputFile()) {
               String fileName = jobParameterInfo[j].getValue();
               int index1 = fileName.lastIndexOf('/');
               int index2 = fileName.lastIndexOf('\\');
               int index = (index1 > index2 ? index1 : index2);
               if(index != -1) {
                  fileName = fileName.substring(index + 1, fileName.length());
               }
               add(new ServerFileNode(fileName, j));
               count++;
            }
         }
         Collections.sort(children);// sort files alphabetically
         return count;
      }


      public boolean getAllowsChildren() {
         return true;
      }


      public boolean isLeaf() {
         return false;
      }

   }


   private static class TaskDateComparator implements Comparator {
      boolean ascending;


      public TaskDateComparator(boolean ascending) {
         this.ascending = ascending;
      }


      public int compare(Object obj1, Object obj2) {
         JobNode node1 = null;
         JobNode node2 = null;
         if(ascending) {
            node1 = (JobNode) obj1;
            node2 = (JobNode) obj2;
         } else {
            node1 = (JobNode) obj2;
            node2 = (JobNode) obj1;
         }

         if(!node1.complete && !node2.complete) {
            return 0;//node1.job.getJobInfo().getDateSubmitted().compareTo(node2.job.getJobInfo().getDateSubmitted());
         }
         if(node1.complete && !node2.complete) {
            return 1;
         }
         if(!node1.complete && node2.complete) {
            return -1;
         }
         return node1.job.getJobInfo().getDateCompleted().compareTo(node2.job.getJobInfo().getDateCompleted());
      }


      public boolean equals(Object obj1, Object obj2) {
         JobNode node1 = (JobNode) obj1;
         JobNode node2 = (JobNode) obj2;
         return node1.job.getJobInfo().getDateCompleted().equals(node2.job.getJobInfo().getDateCompleted());
      }
   }


   private static class TaskNameComparator implements Comparator {
      boolean ascending;


      public TaskNameComparator(boolean ascending) {
         this.ascending = ascending;
      }


      public int compare(Object obj1, Object obj2) {
         JobNode node1 = (JobNode) obj1;
         JobNode node2 = (JobNode) obj2;
         if(ascending) {
            return node1.job.getTaskName().compareTo(node2.job.getTaskName());
         }
         return node2.job.getTaskName().compareTo(node1.job.getTaskName());
      }


      public boolean equals(Object obj1, Object obj2) {
         JobNode node1 = (JobNode) obj1;
         JobNode node2 = (JobNode) obj2;
         return node1.job.getTaskName().equals(node2.job.getTaskName());
      }
   }


   private static class RootNode extends DefaultMutableTreeNode {
      public Vector getChildren() {
         return children;
      }
   }

}
