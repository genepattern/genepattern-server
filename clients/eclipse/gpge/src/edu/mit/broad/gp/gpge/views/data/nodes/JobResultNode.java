/*
 * Created on Jun 19, 2004
 * 
 * TODO To change the template for this generated file go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
package edu.mit.broad.gp.gpge.views.data.nodes;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.ParameterInfo;

public class JobResultNode implements TreeNode, Comparable {
    String server;

    int jobNumber;

    /** all the files this job created. They might not all exist on the server */
    String[] fileNames;

    List children;

    Date timeCompleted;

    String task;
    AnalysisJob job;
    TreeNode parent;
     
    public int compareTo(Object other) {
        JobResultNode node = (JobResultNode) other;
        return this.task.compareTo(node.task);
    }

    public void refresh() {
        for(int i = 0, size = children.size(); i < size; i++) {
            JobResultFileNode node = (JobResultFileNode) children.get(i);
            node.refresh();
        }
    }
    
    /** Remove deleted files from children */
    public void removeDeletedOutputFiles() {
        int numChildren = children.size();
        refresh();
        for(int i = 0, size = children.size(); i < size; i++) {
            JobResultFileNode node = (JobResultFileNode) children.get(i);
            if(!node.exists()) {
                children.remove(node);
            }
        }
        if(numChildren > 0 && children.size()==0) {
            // FIXME remove self from tree
        }
        
    }
    public JobResultNode(String taskName, AnalysisJob job) { // TreeNode parent) {
        this.job = job;
        //this.parent = parent;
        ArrayList jobParameters = new ArrayList();
        ParameterInfo[] jobParameterInfo = job.getJobInfo()
                .getParameterInfoArray();
        List resultFiles = new ArrayList();
        for (int j = 0; j < jobParameterInfo.length; j++) {
            if (jobParameterInfo[j].isOutputFile()) {
                String fileName = jobParameterInfo[j].getValue();
                int index = fileName.lastIndexOf('/');
                if (index == -1) {
                    index = fileName.lastIndexOf('\\');
                }
                if (index != -1) {
                    fileName = fileName.substring(index + 1, fileName.length());
                } 
                resultFiles.add(fileName);
                
            }
        }
        server = job.getSiteName();
        jobNumber = job.getJobInfo().getJobNumber();
        fileNames = (String[]) resultFiles.toArray(new String[0]);
        timeCompleted = job.getJobInfo().getDateCompleted();
        task = taskName;
        
        if (fileNames.length==0) {
            children = new ArrayList(0);
        } else {
            children = new ArrayList(fileNames.length);
            for (int i = 0, length = fileNames.length; i < length; i++) {
                children.add(new JobResultFileNode(this, fileNames[i]));
            }
        }
    }

    public TreeNode parent() {
        return parent;
    }

    public void setParent(TreeNode parent){
        this.parent = parent;
    }
    
    public TreeNode[] children() {
        return (TreeNode[]) children.toArray(new TreeNode[0]);
    }

    public String getColumnText(int column) {
        if (column == 0)
            return task + " (job " + jobNumber + ")";
        else if (column == 1) {
            return "Job";
        } else {
            return DateFormat.getInstance().format(timeCompleted);
        }
    }
    public int getJobNumber() {
        return jobNumber;
    }
    
    public String getServer() {
        return server;
    }

    /**
     * @param jrfn
     */
    public void remove(JobResultFileNode jrfn) {
        children.remove(jrfn);
    }
    
  /*  public void remove() {
        parent.remove(this);
    }
*/
    /**
     * @return
     */
    public AnalysisJob getAnalysisJob() {
        return job;
    }
}