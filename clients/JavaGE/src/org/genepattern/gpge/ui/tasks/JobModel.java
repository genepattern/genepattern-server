/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.gpge.ui.tasks;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.httpclient.methods.GetMethod;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.AscendingComparator;
import org.genepattern.gpge.ui.maindisplay.FileInfoUtil;
import org.genepattern.gpge.ui.maindisplay.SendableTreeNode;
import org.genepattern.gpge.ui.table.SortEvent;
import org.genepattern.gpge.ui.treetable.AbstractSortableTreeTableModel;
import org.genepattern.util.JobDownloader;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Job model
 * 
 * @author Joshua Gould
 */
public class JobModel extends AbstractSortableTreeTableModel {
    String[] columnNames = { "Name", "Kind", "Completed" };

    Class[] columnClasses = { org.jdesktop.swing.treetable.TreeTableModel.class, String.class, String.class };

    static JobModel instance = new JobModel();

    RootNode root = new RootNode();

    private int sortColumn = 0;

    private final JobNodeComparator TASK_NAME_COMPARATOR = new JobNodeComparator(
            "org.genepattern.gpge.ui.tasks.JobModel$TaskNameComparator", true);

    private final JobNodeComparator TASK_DATE_COMPARATOR = new JobNodeComparator(
            "org.genepattern.gpge.ui.tasks.JobModel$TaskCompletedDateComparator", true);

    private JobNodeComparator jobComparator = TASK_NAME_COMPARATOR;

    private final FileComparator FILE_NAME_COMPARATOR = new ServerFileNameComparator(true);

    private final FileComparator FILE_KIND_COMPARATOR = new ServerFileKindComparator(true);

    private FileComparator fileComparator = FILE_NAME_COMPARATOR;

    private JobModel() {
    }

    public static String getJobResultFileName(ServerFileNode node) {
        return getJobResultFileName(((JobNode) node.getParent()).job, node.index);
    }

    private static int getJobCreationJobNumber(ServerFileNode node) {
        return getJobCreationJobNumber(((JobNode) node.getParent()).job, node.index);
    }

    public static boolean isComplete(AnalysisJob job) {
        return job.getJobInfo().getStatus().equals(JobStatus.FINISHED)
                || job.getJobInfo().getStatus().equals(JobStatus.ERROR);
    }

    public static String getJobResultFileName(AnalysisJob job, int parameterInfoIndex) {
        String fileName = job.getJobInfo().getParameterInfoArray()[parameterInfoIndex].getValue();
        int index1 = fileName.lastIndexOf('/');
        int index2 = fileName.lastIndexOf('\\');
        int index = (index1 > index2 ? index1 : index2);
        if (index != -1) {
            fileName = fileName.substring(index + 1, fileName.length());

        }
        return fileName;
    }

    public static int getJobCreationJobNumber(AnalysisJob job, int parameterInfoIndex) {
        int jobNumber = job.getJobInfo().getJobNumber();
        String fileName = job.getJobInfo().getParameterInfoArray()[parameterInfoIndex].getValue();
        int index1 = fileName.lastIndexOf('/');
        int index2 = fileName.lastIndexOf('\\');
        int index = (index1 > index2 ? index1 : index2);
        if (index != -1) {
            jobNumber = Integer.parseInt(fileName.substring(0, index));

        }
        return jobNumber;
    }

    public static void downloadJobResultFile(AnalysisJob job, int parameterInfoIndex, File destination)
            throws IOException {
        String jobNumber = String.valueOf(job.getJobInfo().getJobNumber());

        String fileName = job.getJobInfo().getParameterInfoArray()[parameterInfoIndex].getValue();
        int index1 = fileName.lastIndexOf('/');
        int index2 = fileName.lastIndexOf('\\');
        int index = (index1 > index2 ? index1 : index2);
        if (index != -1) {
            jobNumber = fileName.substring(0, index);
            fileName = fileName.substring(index + 1, fileName.length());

        }
        new JobDownloader(job.getServer(), AnalysisServiceManager.getInstance().getUsername(), AnalysisServiceManager
                .getInstance().getUsername()).download(Integer.parseInt(jobNumber), fileName, destination);

    }

    /**
     * Removes the given file from the model
     * 
     * @param serverFile
     *            Description of the Parameter
     */
    public void remove(ServerFileNode serverFile) {
        JobNode node = (JobNode) serverFile.getParent();
        int serverFileIndex = node.getIndex(serverFile);
        node.remove(serverFileIndex);
        nodesWereRemoved(node, new int[] { serverFileIndex }, new Object[] { serverFile });

    }

    /**
     * Deletes all the jobs from server
     * 
     */
    public void deleteAll() throws WebServiceException {
        List children = root.getChildren();
        if (children != null) {
            AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(AnalysisServiceManager.getInstance()
                    .getServer(), AnalysisServiceManager.getInstance().getUsername(), AnalysisServiceManager
                    .getInstance().getPassword());

            for (int i = 0, size = children.size(); i < size; i++) {
                JobNode node = (JobNode) children.remove(0);
                JobInfo jobInfo = node.job.getJobInfo();
                proxy.deleteJob(jobInfo.getJobNumber());
            }
        }
        nodeStructureChanged(root);
    }

    /**
     * Deletes the given file from the server
     * 
     * @param serverFile
     *            Description of the Parameter
     */
    public void delete(ServerFileNode serverFile) throws WebServiceException {
        JobNode node = (JobNode) serverFile.getParent();

        JobInfo jobInfo = node.job.getJobInfo();
        AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(node.job.getServer(), jobInfo.getUserId(),
                AnalysisServiceManager.getInstance().getPassword());

        proxy.deleteJobResultFile(jobInfo.getJobNumber(), jobInfo.getParameterInfoArray()[serverFile.index].getValue());

        int serverFileIndex = node.getIndex(serverFile);
        node.remove(serverFileIndex);
        nodesWereRemoved(node, new int[] { serverFileIndex }, new Object[] { serverFile });

    }

    /**
     * Deletes the all the output files for a job from the server
     * 
     * @param node
     *            Description of the Parameter
     */
    public void delete(JobNode node) throws WebServiceException {
        JobInfo jobInfo = node.job.getJobInfo();
        AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(node.job.getServer(), jobInfo.getUserId(),
                AnalysisServiceManager.getInstance().getPassword());
        proxy.deleteJob(jobInfo.getJobNumber());
        int index = root.getIndex(node);
        root.remove(index);
        nodesWereRemoved(root, new int[] { index }, new Object[] { node });
    }

    /**
     * Removes the job from the model
     * 
     * @param jobNumber
     *            the job number to remove
     */
    public void remove(int jobNumber) throws WebServiceException {
        JobNode node = findJobNode(jobNumber);
        if (node != null) {
            int index = root.getIndex(node);
            root.remove(index);
            nodesWereRemoved(root, new int[] { index }, new Object[] { node });
        }
    }

    /**
     * Invoked when job is initially submiited
     */
    public void add(AnalysisJob job) {
        if (!isSameServerAndUsername(job)) {
            return;
        }
        if (!job.isClientJob()) {
            JobNode child = new JobNode(job);
            int insertionIndex = 0;
            List children = root.getChildren();
            if (children != null) {
                insertionIndex = Collections.binarySearch(children, child, jobComparator);
            }
            if (insertionIndex < 0) {
                insertionIndex = -insertionIndex - 1;
            }

            root.insert(child, insertionIndex);

            nodesWereInserted(root, new int[] { insertionIndex });

        }
        MessageManager.notifyListeners(new JobMessage(this, JobMessage.JOB_SUBMITTED, job));
    }

    private boolean isSameServerAndUsername(AnalysisJob job) {
        String server = AnalysisServiceManager.getInstance().getServer();
        String jobServer = job.getServer();
        String username = AnalysisServiceManager.getInstance().getUsername();
        if (job.getJobInfo() == null) {
            return false;
        }
        String jobUsername = job.getJobInfo().getUserId();
        return server.equals(jobServer) && username.equals(jobUsername);
    }

    public void addChildOutputFiles(AnalysisJob job) {
        addOutputs(job, false);

    }

    public void jobCompleted(AnalysisJob job) {
        addOutputs(job, true);
    }

    private void addOutputs(AnalysisJob job, boolean jobCompleted) {
        if (!isSameServerAndUsername(job)) {
            return;
        }

        JobNode jobNode = findJobNode(job);
        if (jobNode == null) {
            add(job);
            jobNode = findJobNode(job);
        } else if (jobCompleted && jobComparator == TASK_DATE_COMPARATOR) { // remove
            // job
            // and
            // insert
            // it
            // in
            // correct
            // position
            int insertionIndex = 0;
            int removedNodeIndex = 0;
            List children = root.getChildren();
            if (children != null) {
                removedNodeIndex = children.indexOf(jobNode);
                root.remove(removedNodeIndex);
                insertionIndex = Collections.binarySearch(children, jobNode, jobComparator);
            }

            if (insertionIndex < 0) {
                insertionIndex = -insertionIndex - 1;
            }
            root.insert(jobNode, insertionIndex);
            try {
                nodesWereRemoved(root, new int[] { removedNodeIndex }, new Object[] { jobNode });
                nodesWereInserted(root, new int[] { insertionIndex });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        int outputFiles = jobNode.getOutputFiles();

        if (outputFiles > 0) {
            // int[] newIndexs = new int[outputFiles];
            // for (int i = 0; i < outputFiles; i++) {
            // newIndexs[i] = i;
            // }
            nodeStructureChanged(jobNode);
            // nodesWereInserted(jobNode, newIndexs);
        }

        if (jobCompleted) {
            MessageManager.notifyListeners(new JobMessage(this, JobMessage.JOB_COMPLETED, job));
        }
    }

    public void jobStatusChanged(AnalysisJob job) {
        if (!isSameServerAndUsername(job)) {
            return;
        }
        JobNode jobNode = findJobNode(job);
        if (jobNode == null) {
            add(job);
            jobNode = findJobNode(job);
        }
        nodeChanged(jobNode);
        MessageManager.notifyListeners(new JobMessage(this, JobMessage.JOB_STATUS_CHANGED, job));
    }

    public void sortOrderChanged(SortEvent e) {
        int column = e.getColumn();
        boolean ascending = e.isAscending();
        sortColumn = column;
        List children = root.getChildren();

        if (column == 0) {

            TASK_NAME_COMPARATOR.setAscending(ascending);
            jobComparator = TASK_NAME_COMPARATOR;

            FILE_NAME_COMPARATOR.setAscending(ascending);
            fileComparator = FILE_NAME_COMPARATOR;

        } else if (column == 1) { // kind
            TASK_NAME_COMPARATOR.setAscending(ascending);
            jobComparator = TASK_NAME_COMPARATOR;

            FILE_NAME_COMPARATOR.setAscending(ascending); // file name is used
            // in case of tie
            // when comparing by
            // kind
            FILE_KIND_COMPARATOR.setAscending(ascending);
            fileComparator = FILE_KIND_COMPARATOR;

        } else if (column == 2) { // date
            TASK_DATE_COMPARATOR.setAscending(ascending);
            jobComparator = TASK_DATE_COMPARATOR;

            FILE_NAME_COMPARATOR.setAscending(ascending);
            fileComparator = FILE_NAME_COMPARATOR;
        }

        if (children != null) {
            Collections.sort(children, jobComparator);
            for (int i = 0; i < children.size(); i++) {
                JobNode node = (JobNode) children.get(i);
                if (node.getChildren() != null) {
                    Collections.sort(node.getChildren(), fileComparator);
                }
            }
            nodeStructureChanged(root);
        }

    }

    private JobNode findJobNode(int jobNumber) {
        for (int i = 0, size = root.getChildCount(); i < size; i++) {
            JobNode n = (JobNode) root.getChildAt(i);
            if (n.job.getJobInfo().getJobNumber() == jobNumber) {
                return n;
            }
        }
        return null;
    }

    private JobNode findJobNode(AnalysisJob job) {
        for (int i = 0, size = root.getChildCount(); i < size; i++) {
            JobNode n = (JobNode) root.getChildAt(i);
            if (n.job == job) {
                return n;
            }
        }
        return null;
        // List children = root.getChildren();
        // int index = -1;
        // if (children != null) {
        // index = Collections.binarySearch(children, new JobNode(job),
        // jobComparator);
        // }

    }

    public void getJobsFromServer() throws WebServiceException {
        root.removeAllChildren();
        nodeStructureChanged(root);
        String server = AnalysisServiceManager.getInstance().getServer();
        String username = AnalysisServiceManager.getInstance().getUsername();
        AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(server, username, AnalysisServiceManager
                .getInstance().getPassword());
        JobInfo[] jobs = proxy.getJobs(username, false);

        if (jobs != null) {
            for (int i = 0; i < jobs.length; i++) {
                AnalysisJob job = new AnalysisJob(server, jobs[i]);
                JobNode child = new JobNode(job);
                boolean waitUntilCompletion = false;
                if (job.getJobInfo().getStatus().equals(JobStatus.FINISHED)
                        || job.getJobInfo().getStatus().equals(JobStatus.ERROR)) {
                    child.getOutputFiles();
                } else {
                    waitUntilCompletion = true;
                }

                int insertionIndex = 0;
                List children = root.getChildren();
                if (children != null) {
                    insertionIndex = Collections.binarySearch(children, child, jobComparator);
                }
                if (insertionIndex < 0) {
                    insertionIndex = -insertionIndex - 1;
                }

                root.insert(child, insertionIndex);

                if (waitUntilCompletion) {
                    TaskLauncher.waitUntilCompletionInNewThread(job);
                }
            }

        }
        nodeStructureChanged(root);
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
        if (node instanceof ServerFileNode) {
            ServerFileNode f = (ServerFileNode) node;
            switch (column) {
            case 0:
                return f.name;
            case 1:
                return f.getFileInfo() != null ? f.getFileInfo().getKind() : "";
            default:
                return null;
            }
        } else if (node instanceof JobNode) {
            JobNode j = (JobNode) node;
            switch (column) {
            case 0:
                return j.toString();
            case 1:
                return "Job";
            case 2:
                JobInfo jobInfo = j.job.getJobInfo();
                if (!j.isComplete()) {
                    String status = jobInfo.getStatus();
                    if (status.equals(JobStatus.NOT_STARTED)) {
                        status = "Pending";
                    }
                    return status;
                }

                Date d = jobInfo.getDateCompleted();
                return java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
                        .format(d);
            }
        }
        return null;
    }

    /**
     * Description of the Class
     * 
     * @author Joshua Gould
     */
    public static class ServerFileNode extends SendableTreeNode implements Comparable {
        /** The name of this output file */
        public final String name;

        /**
         * The string for this node to display before the <tt>name</tt>.
         * <tt>Null</tt> when the output file was not produced by a child job.
         * When the output was produces by a pipeline, the name is prefixed with
         * the step number in the pipeline (e.g.
         * 1.PreprocessDataset:all_aml_preprocess.gct).
         */
        private final String displayPrefix;

        /**
         * The index in the <tt>ParameterInfo</tt> array of this job result
         * file
         */
        public final int index;

        private FileInfoUtil.FileInfo fileInfo;

        public ServerFileNode(String displayPrefix, String name, int index) {
            this.displayPrefix = displayPrefix;
            this.name = name;
            this.index = index;
        }

        public boolean equals(Object obj) {
            if (obj instanceof ServerFileNode) {
                ServerFileNode n = (ServerFileNode) obj;
                return this.toString().equals(n.toString());
            }
            return false;
        }

        public int hashCode() {
            return toString().hashCode();
        }

        void updateFileInfo() {
            String displayString = displayPrefix != null ? displayPrefix + name : name;
            AnalysisServiceManager asm = AnalysisServiceManager.getInstance();
            String username = asm.getUsername();
            String password = asm.getPassword();
            String server = asm.getServer();
            GetMethod get = null;
            try {
                get = new JobDownloader(server, username, password).getGetMethod(getJobNumber(), name);
                fileInfo = FileInfoUtil.getInfo(get.getResponseBodyAsStream(), displayString, get.getResponseContentLength());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (get != null) {
                    get.releaseConnection();
                }
            }
        }

        public FileInfoUtil.FileInfo getFileInfo() {
            return fileInfo;
        }

        public String getParameterValue() {
            JobNode parent = (JobNode) getParent();
            return parent.job.getJobInfo().getParameterInfoArray()[index].getValue();
        }

        /**
         * Gets the job number that created this file
         * 
         * @return
         */
        public int getJobNumber() {
            return getJobCreationJobNumber(this);
        }

        /**
         * Gets the file name on the server for this output file
         * 
         * @return
         */
        public String getFileName() {
            return name;
        }

        /**
         * Returns the url to download the file from
         * 
         * @return The url to retrieve the file from.
         */
        public URL getURL() {
            try {
                JobNode parent = (JobNode) getParent();
                AnalysisJob job = parent.job;
                int jobNumber = getJobCreationJobNumber(this);
                return new URL(job.getServer() + "/gp/jobResults/" + jobNumber + "/" + URLEncoder.encode(name, "UTF-8"));
            } catch (MalformedURLException x) {
                throw new Error(x);
            } catch (java.io.UnsupportedEncodingException uee) {
                throw new Error("Unable to encode " + name);
            }
        }

        public void download(File destination) throws IOException {
            JobNode parent = (JobNode) getParent();
            AnalysisJob job = parent.job;
            downloadJobResultFile(job, index, destination);
        }

        public String toString() {
            return displayPrefix != null ? displayPrefix + name : name;
        }

        public int compareTo(Object other) {
            ServerFileNode node = (ServerFileNode) other;
            if (this.displayPrefix != null && node.displayPrefix == null) {
                return -1;
            } else if (this.displayPrefix == null && node.displayPrefix != null) {
                return 1;
            } else if (this.displayPrefix != null && node.displayPrefix != null) {
                int num1 = Integer.parseInt(displayPrefix.substring(0, displayPrefix.indexOf(".")));
                int num2 = Integer.parseInt(node.displayPrefix.substring(0, node.displayPrefix.indexOf(".")));
                if (num1 < num2) {
                    return -1;
                } else if (num2 > num1) {
                    return 1;
                } else {
                    this.name.compareToIgnoreCase(node.name);
                }
            }
            return this.name.compareToIgnoreCase(node.name);
        }

        public boolean getAllowsChildren() {
            return false;
        }

        public boolean isLeaf() {
            return true;
        }

        public String toUIString() {
            return "job #" + this.getJobNumber() + ", " + this.getFileName();
        }

    }

    public static String jobToString(AnalysisJob job) {
        return job.getTaskName() + " (" + job.getJobInfo().getJobNumber() + ")";
    }

    /**
     * Description of the Class
     * 
     * @author Joshua Gould
     */
    public static class JobNode extends DefaultMutableTreeNode {

        public final AnalysisJob job;

        Vector getChildren() {
            return children;
        }

        public boolean isComplete() {
            return JobModel.isComplete(job);
        }

        public JobNode(AnalysisJob job) {
            this.job = job;
        }

        public String toString() {
            return jobToString(job);
        }

        private int addOutputFiles(ParameterInfo[] jobParameterInfo, JobInfo[] childJobs) {
            int numOutputFiles = 0;

            for (int j = 0; j < jobParameterInfo.length; j++) {
                if (jobParameterInfo[j].isOutputFile()) {
                    String displayPrefix = null;
                    String fileName = jobParameterInfo[j].getValue();
                    int index1 = fileName.lastIndexOf('/');
                    int index2 = fileName.lastIndexOf('\\');
                    int index = (index1 > index2 ? index1 : index2);
                    if (index != -1) {
                        int paramJobNumber = Integer.parseInt(fileName.substring(0, index));
                        if (childJobs != null) {
                            for (int k = 0; k < childJobs.length; k++) {
                                if (childJobs[k].getJobNumber() == paramJobNumber) {
                                    displayPrefix = (k + 1) + "." + childJobs[k].getTaskName() + ":";
                                    break;
                                }
                            }
                        }
                        fileName = fileName.substring(index + 1, fileName.length());

                    }

                    ServerFileNode child = new ServerFileNode(displayPrefix, fileName, j);
                    if (children == null || !children.contains(child)) {
                        this.add(child);
                        child.updateFileInfo();
                        numOutputFiles++;
                    }
                }
            }
            return numOutputFiles;
        }

        public int getOutputFiles() {
            int numOutputFiles = 0;
            ParameterInfo[] jobParameterInfo = job.getJobInfo().getParameterInfoArray();
            int jobNumber = job.getJobInfo().getJobNumber();

            AnalysisWebServiceProxy proxy = null;
            int[] childJobNumbers = null;
            try {
                proxy = new AnalysisWebServiceProxy(AnalysisServiceManager.getInstance().getServer(),
                        AnalysisServiceManager.getInstance().getUsername(), AnalysisServiceManager.getInstance()
                                .getPassword());
                childJobNumbers = proxy.getChildren(jobNumber);
            } catch (WebServiceException wse) {
                wse.printStackTrace();
            }

            if (childJobNumbers != null && childJobNumbers.length > 0) {
                try {
                    JobInfo[] children = new JobInfo[childJobNumbers.length];
                    for (int i = 0; i < children.length; i++) {
                        children[i] = proxy.checkStatus(childJobNumbers[i]);
                    }
                    numOutputFiles += addOutputFiles(jobParameterInfo, children);
                } catch (WebServiceException wse) {
                    wse.printStackTrace();
                    this.removeAllChildren();
                    numOutputFiles = addOutputFiles(jobParameterInfo, null);
                }

            } else {
                numOutputFiles = addOutputFiles(jobParameterInfo, null);
            }
            if (children != null) {
                Collections.sort(children, JobModel.getInstance().fileComparator);
            }

            return numOutputFiles;
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public boolean isLeaf() {
            return false;
        }

    }

    private static class ServerFileKindComparator implements FileComparator {
        boolean ascending;

        public ServerFileKindComparator(boolean ascending) {
            this.ascending = ascending;
        }

        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public String toString() {
            return "File Kind " + ascending;
        }

        public int compare(Object obj1, Object obj2) {
            ServerFileNode sfn1 = null;
            ServerFileNode sfn2 = null;
            if (ascending) {
                sfn1 = (ServerFileNode) obj1;
                sfn2 = (ServerFileNode) obj2;
            } else {
                sfn1 = (ServerFileNode) obj2;
                sfn2 = (ServerFileNode) obj1;
            }
            String kind1 = sfn1.getFileInfo() != null ? sfn1.getFileInfo().getKind() : "";

            String kind2 = sfn2.getFileInfo() != null ? sfn2.getFileInfo().getKind() : "";

            int result = kind1.compareToIgnoreCase(kind2);
            if (result == 0) {
                return JobModel.getInstance().FILE_NAME_COMPARATOR.compare(obj1, obj2);
            }
            return result;
        }

    }

    private static class ServerFileNameComparator implements FileComparator {
        boolean ascending;

        public ServerFileNameComparator(boolean ascending) {
            this.ascending = ascending;
        }

        public String toString() {
            return "File Name " + ascending;
        }

        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(Object obj1, Object obj2) {
            ServerFileNode sfn1 = null;
            ServerFileNode sfn2 = null;
            if (ascending) {
                sfn1 = (ServerFileNode) obj1;
                sfn2 = (ServerFileNode) obj2;
            } else {
                sfn1 = (ServerFileNode) obj2;
                sfn2 = (ServerFileNode) obj1;
            }

            return sfn1.compareTo(sfn2);
        }

    }

    private static class JobNodeComparator implements AscendingComparator {

        AscendingComparator c;

        public void setAscending(boolean ascending) {
            c.setAscending(ascending);
        }

        public JobNodeComparator(String className, boolean ascending) {
            try {
                c = (AscendingComparator) Class.forName(className).newInstance();
                c.setAscending(ascending);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public int compare(Object obj1, Object obj2) {
            JobNode node1 = (JobNode) obj1;
            JobNode node2 = (JobNode) obj2;
            return c.compare(node1.job, node2.job);

        }

        public String toString() {
            return c.toString();
        }

    }

    public static class TaskSubmittedDateComparator implements JobComparator {
        boolean ascending;

        public String toString() {
            return "Date Submitted " + ascending;
        }

        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public int compare(Object obj1, Object obj2) {
            AnalysisJob job1 = null;
            AnalysisJob job2 = null;

            if (ascending) {
                job1 = (AnalysisJob) obj1;
                job2 = (AnalysisJob) obj2;
            } else {
                job1 = (AnalysisJob) obj2;
                job2 = (AnalysisJob) obj1;
            }

            return job1.getJobInfo().getDateSubmitted().compareTo(job2.getJobInfo().getDateSubmitted());
        }

    }

    public static class TaskCompletedDateComparator implements JobComparator {
        boolean ascending;

        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public String toString() {
            return "Date Completed " + ascending;
        }

        public int compare(Object obj1, Object obj2) {
            AnalysisJob job1 = null;
            AnalysisJob job2 = null;
            if (ascending) {
                job1 = (AnalysisJob) obj1;
                job2 = (AnalysisJob) obj2;
            } else {
                job1 = (AnalysisJob) obj2;
                job2 = (AnalysisJob) obj1;
            }
            boolean job1Complete = isComplete(job1);
            boolean job2Complete = isComplete(job2);
            if (!job1Complete && !job2Complete) {
                return 0;// node1.job.getJobInfo().getDateSubmitted().compareTo(node2.job.getJobInfo().getDateSubmitted());
            }
            if (job1Complete && !job2Complete) {
                return 1;
            }
            if (!job1Complete && job2Complete) {
                return -1;
            }
            return job1.getJobInfo().getDateCompleted().compareTo(job2.getJobInfo().getDateCompleted());
        }
    }

    public static class TaskNameComparator implements JobComparator {
        boolean ascending;

        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        public String toString() {
            return "Task Name " + ascending;
        }

        public int compare(Object obj1, Object obj2) {
            AnalysisJob ajob1 = (AnalysisJob) obj1;
            AnalysisJob ajob2 = (AnalysisJob) obj2;
            String job1 = ajob1.getTaskName();
            String job2 = ajob2.getTaskName();

            if (job1.equals(job2)) {
                Integer jobNumber1 = new Integer(ajob1.getJobInfo().getJobNumber());
                Integer jobNumber2 = new Integer(ajob2.getJobInfo().getJobNumber());
                return ascending ? jobNumber1.compareTo(jobNumber2) : jobNumber2.compareTo(jobNumber1);
            }
            return ascending ? job1.compareToIgnoreCase(job2) : job2.compareToIgnoreCase(job1);
        }

    }

    private static class RootNode extends DefaultMutableTreeNode {
        public Vector getChildren() {
            return children;
        }
    }

    private static interface FileComparator extends AscendingComparator {
    }

    private static interface JobComparator extends AscendingComparator {
    }

}