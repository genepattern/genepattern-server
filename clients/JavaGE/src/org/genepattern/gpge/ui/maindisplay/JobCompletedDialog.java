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


package org.genepattern.gpge.ui.maindisplay;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.genepattern.gpge.ui.table.*;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;
import org.genepattern.gpge.PropertyManager;

public class JobCompletedDialog {
	JDialog dialog;

	AlternatingColorTable table;

	MyTableModel tableModel = new MyTableModel();
   JMenuItem menuItem;
   JCheckBox alertOnJobCompletionCheckBox; 
   
	public void setShowDialog(boolean showDialog) {
      PropertyManager.setProperty(
				PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG, String.valueOf(showDialog));
      
      menuItem.setSelected(showDialog);
      alertOnJobCompletionCheckBox.setSelected(showDialog);
	}

	/**
	 * Returns <code>true</code> if the dialog will show when a job completes,
	 * <code>false</code> otherwise.
	 * 
	 * @return whether the dialog shows when a job completes
	 */
	public boolean isShowingDialog() {
		final boolean showDialog = Boolean.valueOf(
		PropertyManager
						.getProperty(PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG)).booleanValue();
		return showDialog;
	}

	public JobCompletedDialog(Frame parent, JMenuItem _menuItem) {
		dialog = new CenteredDialog(parent);
      this.menuItem = _menuItem;
		dialog.setTitle("Recently Completed Jobs");
		table = new AlternatingColorTable(tableModel);
      table.setShowCellFocus(false);
		Container contentPane = dialog.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(new JScrollPane(table), BorderLayout.CENTER);

		alertOnJobCompletionCheckBox = new JCheckBox(
				"Show this dialog when a job completes");
		alertOnJobCompletionCheckBox.setSelected(true);
		alertOnJobCompletionCheckBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
            boolean showDialog = alertOnJobCompletionCheckBox.isSelected();
				PropertyManager.setProperty(
				PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG, String.valueOf(showDialog));
            menuItem.setSelected(showDialog);
			}
		});

		JPanel tempPanel = new JPanel() {
			public Dimension getMinimumSize() {
				return new Dimension(400, 20);
			}
		};
		tempPanel.add(alertOnJobCompletionCheckBox);
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		tempPanel.add(closeButton);

		contentPane.add(tempPanel, BorderLayout.SOUTH);
		dialog.setSize(400, 200);
      dialog.getRootPane().setDefaultButton(closeButton);
		dialog.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent we) {
				close();
			}
		});
	}

	void close() {
		dialog.setVisible(false);
		tableModel.clear();
	}

	public void add(final int jobNumber, final String taskName,
			final String status) {
		final boolean showDialog = Boolean.valueOf(
		   PropertyManager
						.getProperty(PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG)).booleanValue();
		if (!showDialog) {
			return;
		}
		if (SwingUtilities.isEventDispatchThread()) {
			tableModel.add(new MyJobInfo(jobNumber, taskName, status));
			dialog.setVisible(true);
		} else {
			SwingUtilities.invokeLater(new Thread() {
				public void run() {
					tableModel.add(new MyJobInfo(jobNumber, taskName, status));
					dialog.setVisible(true);
				}
			});
		}
	}

	static class MyJobInfo {
		Integer jobNumber;

		String status;

		String taskName;

		MyJobInfo(int jobNumber, String taskName, String status) {
			this.jobNumber = new Integer(jobNumber);
			this.status = status;
			this.taskName = taskName;
		}
	}

	static class MyTableModel extends AbstractTableModel {
		java.util.List jobs = new ArrayList();

		void clear() {
			jobs.clear();
			fireTableDataChanged();
		}

		public Object getValueAt(int r, int c) {
			switch (c) {
			case 0:
				return ((MyJobInfo) (jobs.get(r))).jobNumber;
			case 1:
				return ((MyJobInfo) (jobs.get(r))).taskName;
			case 2:
				return ((MyJobInfo) (jobs.get(r))).status;
			default:
				return null;
			}
		}

		void add(MyJobInfo jobInfo) {
			jobs.add(jobInfo);
			fireTableRowsInserted(jobs.size() - 1, jobs.size() - 1);
		}

		public Class getColumnClass(int column) {
			return String.class;
		}

		public int getRowCount() {
			return jobs.size();
		}

		public int getColumnCount() {
			return 2;
		}

		public String getColumnName(int col) {
			switch (col) {
			case 0:
				return "Job Number";
			case 1:
				return "Task";
			case 2:
				return "Status";
			default:
				return null;
			}
		}

	}
}