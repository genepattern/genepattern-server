package org.genepattern.gpge.ui.tasks;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Category;
import org.genepattern.webservice.AnalysisJob;

/**
 * HistoryTableModel.java
 *
 * <p>Description: Table model for the HistoryPanel</p>
 * @author Hui Gong
 * @version $Revision$
 */

public class HistoryTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Job ID", "Type", "Time Submitted", "Time Completed", "Status","Service Site"};
    private Vector _jobs;
    private Vector original_jobs;
    private String site_name;
    private static Category cat = Category.getInstance(HistoryTableModel.class.getName());

     /**
     * Constructor
     * @param jobs a list of JobInfo objects
     */
    public HistoryTableModel(Vector jobs) {
        this(jobs, null);   
    }
    /**
     * Constructor
     * @param jobs a list of JobInfo objects
     * @param site_name the name of the site whos jobs will be displayed or null
     *        to display all
     */
    public HistoryTableModel(Vector jobs, final String site_name) {
        this.site_name = site_name;
        if( site_name != null )
            resetData(jobs);
        else {
            _jobs = new Vector(0);
            original_jobs = jobs;
        }
    }
	 
	 public void addJob(AnalysisJob job) {
			_jobs.add(job); 
	 }

    /**
     * resets the data inside the table model.
     * @param jobs a list of JobInfo objects
     */
    public void resetData (Vector jobs){
        //System.out.println("****** resetting jobs "+jobs.size()+" *******");
        original_jobs = jobs;
        if( site_name == null ) 
            this._jobs = jobs;
        else {
            final int cnt = jobs.size();
            _jobs = new Vector(cnt);
            
            for(int i = 0; i < cnt; i++) {
                final AnalysisJob job = (AnalysisJob)jobs.elementAt(i);
                System.out.println("Job "+job);
                System.out.println("site_name='"+job.getServer()+"'");
                if( site_name.equalsIgnoreCase(job.getServer()) )
                    _jobs.add(job);
            }
        }
        cat.debug("reseting history table data model");
        this.fireTableDataChanged();
    }
    /** sets the site name and resets the table */
    public void setSiteName(final String site_name) {
        this.site_name = site_name;
        resetData(original_jobs);
    }

    /**
     * Gets the number of column
     */
    public int getColumnCount() {
        return columnNames.length;
    }


    /**
     * gets the number of rows.
     */
    public int getRowCount() {
        return _jobs.size();
    }

    /**
     * Gets the name of a column.
     */
    public String getColumnName(int col) {
        return columnNames[col];
    }

    /**
     * Gets the value of the table at certain row and column.
     */
    public Object getValueAt(int row, int col) {
        AnalysisJob job = (AnalysisJob)this._jobs.get(row);
        Object info;
        if(col==0)
            info = String.valueOf(job.getJobInfo().getJobNumber());
        else if(col==1)
            info = job.getTaskName();
        else if(col == 2)
            info = job.getJobInfo().getDateSubmitted();
        else if(col == 3)
            info = job.getJobInfo().getDateCompleted();
        else if(col == 4)
            info = job.getJobInfo().getStatus();
        else
            info = job.getServer();
        return info;
    }
	 
	 public void remove(int[] selectedRows) {
		 for(int i = 0; i < selectedRows.length; i++) {
				_jobs.remove(selectedRows[i]-i); // subtract i b/c remove shifts elements to the left
		 }
	 }

}
