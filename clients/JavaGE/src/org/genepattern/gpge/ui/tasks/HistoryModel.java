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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.GPGE.JobNumberComparator;
import org.genepattern.gpge.ui.table.ColumnSorter;
import org.genepattern.gpge.ui.table.SortEvent;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.AnalysisWebServiceProxy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * Maintains list of all jobs on server
 *  
 * @author Joshua Gould
 *
 */
public class HistoryModel extends AbstractTableModel implements
		GPGEMessageListener, ColumnSorter {
	static HistoryModel instance = new HistoryModel();

	ArrayList historyList = new ArrayList();

	JobNumberComparator jobNumberComparator = new JobNumberComparator();

	/** current comparator */
	java.util.Comparator comparator = jobNumberComparator;

	private HistoryModel() {
		MessageManager.addGPGEMessageListener(this);
	}

	public static HistoryModel getInstance() {
		return instance;
	}

	public List getJobsByJobNumber() {
		List clonedList = (List) historyList.clone();
		Collections.sort(clonedList, jobNumberComparator);
		return clonedList;
	}

	public void receiveMessage(GPGEMessage message) {
		if (message instanceof JobMessage) {
			JobMessage je = (JobMessage) message;
			if (je.getType() == JobMessage.JOB_SUBMITTED) {
				add(je.getJob());
			} else if (je.getType() == JobMessage.JOB_STATUS_CHANGED
					|| je.getType() == JobMessage.JOB_COMPLETED) {
				fireTableStructureChanged();
			}
		}
	}

	private void add(AnalysisJob job) {
		int insertionIndex = Collections.binarySearch(historyList, job,
				comparator);

		if (insertionIndex < 0) {
			insertionIndex = -insertionIndex - 1;
		}

		historyList.add(insertionIndex, job);
		this.fireTableRowsInserted(insertionIndex, insertionIndex);
	}

	public void sortOrderChanged(SortEvent e) {
		int column = e.getColumn();
		boolean ascending = e.isAscending();

		if (column == 0) {
			JobModel.TaskNameComparator c = new JobModel.TaskNameComparator();
			c.setAscending(ascending);
			comparator = c;
		} else if (column == 1) {
			JobModel.TaskCompletedDateComparator c = new JobModel.TaskCompletedDateComparator();
			c.setAscending(ascending);
			comparator = c;
		} else {
			JobModel.TaskSubmittedDateComparator c = new JobModel.TaskSubmittedDateComparator();
			c.setAscending(ascending);
			comparator = c;
		}

		Collections.sort(historyList, comparator);
		fireTableStructureChanged();
	}

	public Object getValueAt(int r, int c) {
		AnalysisJob job = (AnalysisJob) historyList.get(r);
		JobInfo jobInfo = job.getJobInfo();
		boolean complete = JobModel.isComplete(job);
		switch (c) {
		case 0:
			return JobModel.jobToString(job);
		case 1:
			if (!complete) {
				return jobInfo.getStatus();
			}
			return java.text.DateFormat.getDateTimeInstance(
					java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
					.format(jobInfo.getDateCompleted());
		case 2:
			return java.text.DateFormat.getDateTimeInstance(
					java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
					.format(jobInfo.getDateSubmitted());
		default:
			return null;
		}
	}

	public Class getColumnClass(int j) {
		return String.class;
	}

	public int getRowCount() {
		return historyList.size();
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int c) {
		switch (c) {
		case 0:
			return "Name";
		case 1:
			return "Completed";
		case 2:
			return "Submitted";
		default:
			return null;
		}
	}

	public void updateHistory() throws WebServiceException {
		historyList.clear();
		String server = AnalysisServiceManager.getInstance().getServer();
		String username = AnalysisServiceManager.getInstance().getUsername();
		AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(server,
				username);
		JobInfo[] jobs = proxy.getJobs(username, true);
		for (int i = 0; i < jobs.length; i++) {
			historyList.add(new AnalysisJob(server, jobs[i]));
		}
		fireTableStructureChanged();
	}

	public void remove(int row) throws WebServiceException {
		AnalysisWebServiceProxy proxy = new AnalysisWebServiceProxy(
				AnalysisServiceManager.getInstance().getServer(),
				AnalysisServiceManager.getInstance().getUsername());
		proxy.purgeJob(getJob(row).getJobInfo().getJobNumber());
		historyList.remove(row);
		this.fireTableRowsDeleted(row, row);

	}

	public AnalysisJob getJob(int row) {
		return (AnalysisJob) historyList.get(row);
	}

}
