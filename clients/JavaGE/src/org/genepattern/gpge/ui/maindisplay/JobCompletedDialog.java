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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.genepattern.gpge.ui.table.*;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;

public class JobCompletedDialog {
	JDialog dialog;

	JTable table;

	MyTableModel tableModel = new MyTableModel();

	public void setShowDialog(boolean showDialog) {
		org.genepattern.util.GPpropertiesManager.setProperty(
				PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG, String.valueOf(showDialog));
	}

	/**
	 * Returns <code>true</code> if the dialog will show when a job completes,
	 * <code>false</code> otherwise.
	 * 
	 * @return whether the dialog shows when a job completes
	 */
	public boolean isShowingDialog() {
		final boolean showDialog = Boolean.valueOf(
				org.genepattern.util.GPpropertiesManager
						.getProperty(PreferenceKeys.SHOW_JOB_COMPLETED_DIALOG)).booleanValue();
		return showDialog;
	}

	public JobCompletedDialog(Frame parent) {
		dialog = new CenteredDialog(parent);
		dialog.setTitle("Recently Completed Jobs");
		table = new AlternatingColorTable(tableModel);
		Container contentPane = dialog.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(new JScrollPane(table), BorderLayout.CENTER);

		JCheckBox checkBox = new JCheckBox(
				"Show this dialog when a job completes");
		checkBox.setSelected(true);
		checkBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				setShowDialog(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

		JPanel tempPanel = new JPanel() {
			public Dimension getMinimumSize() {
				return new Dimension(400, 20);
			}
		};
		tempPanel.add(checkBox);
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		tempPanel.add(closeButton);

		contentPane.add(tempPanel, BorderLayout.SOUTH);
		dialog.setSize(400, 200);

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
				org.genepattern.util.GPpropertiesManager
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