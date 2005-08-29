package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.maindisplay.CenteredDialog;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.gpge.ui.table.AlternatingColorTable;
import org.genepattern.gpge.ui.table.SortableHeaderRenderer;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.WebServiceException;

/**
 * A menu that displays the most recent jobs
 * 
 * @author Joshua Gould
 * 
 */
public class HistoryMenu extends JMenu {
	private ActionListener reloadJobActionListener;

	private JMenuItem historyMenuItem = new JMenuItem("View All");

	private static final int JOBS_IN_MENU = 10;

	private JDialog historyDialog;

	private HistoryModel model = HistoryModel.getInstance();

	public HistoryMenu() {
		super("History");
		model.addTableModelListener(new TableModelListener() {

			public void tableChanged(TableModelEvent e) {
				/*
				 * if (e.getType() == TableModelEvent.INSERT) {
				 *  } else if (e.getType() == TableModelEvent.UPDATE) {
				 *  } else if (e.getType() == TableModelEvent.DELETE) {
				 *  }
				 */
				createHistoryMenu();
			}

		});
		reloadJobActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AnalysisJobMenuItem menuItem = (AnalysisJobMenuItem) e
						.getSource();
				AnalysisJob job = menuItem.job;
				GPGE.getInstance().reload(job);
			}
		};

		historyMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				historyDialog.setVisible(true);
			}
		});

		historyDialog = new CenteredDialog((java.awt.Frame) GenePattern
				.getDialogParent());
		historyDialog.setTitle("History");
		final AlternatingColorTable table = new AlternatingColorTable(model);
		table.setShowCellFocus(false);

		JPanel toolBar = new JPanel();
		JButton reloadButton = new JButton("Reload");
		reloadButton.setToolTipText("Reload the job"); // 
		reloadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row = table.getSelectedRow();
				if (row == -1) {
					return;
				}
				AnalysisJob job = HistoryModel.getInstance().getJob(row);
				GPGE.getInstance().reload(job);
			}
		});

		JButton purgeButton = new JButton("Purge");
		purgeButton.setToolTipText("Purge the job from your history");
		purgeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row = table.getSelectedRow();
				if (row == -1) {
					return;
				}
				purge(row);
			}
		});

		toolBar.add(reloadButton);
		toolBar.add(purgeButton);
		table.setShowGrid(true);
		table.setShowVerticalLines(true);
		table.setShowHorizontalLines(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
					int row = table.getSelectedRow();
					if (row == -1) {
						return;
					}
					AnalysisJob job = HistoryModel.getInstance().getJob(row);
					GPGE.getInstance().reload(job);
				}
			}
		});
		new SortableHeaderRenderer(table, HistoryModel.getInstance());
		historyDialog.getContentPane().add(toolBar, BorderLayout.NORTH);
		historyDialog.getContentPane().add(new JScrollPane(table),
				BorderLayout.CENTER);
		historyDialog.pack();
		historyDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}

	private void createHistoryMenu() {
		super.removeAll();
		HistoryModel model = HistoryModel.getInstance();

		List jobs = model.getJobsByJobNumber();
		int length = Math.min(JOBS_IN_MENU, jobs.size());
		for (int i = 0; i < length; i++) {
			AnalysisJob job = (AnalysisJob) jobs.get(i);
			AnalysisJobMenuItem menuItem = new AnalysisJobMenuItem(job);
			menuItem.addActionListener(reloadJobActionListener);
			add(menuItem);
		}
		add(new JSeparator());
		add(historyMenuItem);
	}

	private void purge(int row) {
		AnalysisJob job = (AnalysisJob) HistoryModel.getInstance().getJob(row);
		String message = "Are you sure you want to purge job number "
				+ job.getJobInfo().getJobNumber() + "?";
		if (!GUIUtil.showConfirmDialog(historyDialog, "GenePattern", message)) {
			return;
		}
		try {
			HistoryModel.getInstance().remove(row);
			createHistoryMenu();
			JobModel.getInstance().remove(job.getJobInfo().getJobNumber());
		} catch (WebServiceException wse) {
			wse.printStackTrace();
			if (!GenePattern.disconnectedFromServer(wse)) {
				GenePattern
						.showErrorDialog("An error occurred while removing job number "
								+ job.getJobInfo().getJobNumber());
			}
		}
	}

	private static class AnalysisJobMenuItem extends JMenuItem {
		AnalysisJob job;

		public AnalysisJobMenuItem(AnalysisJob job) {
			super(job.getJobInfo().getTaskName() + " ("
					+ job.getJobInfo().getJobNumber() + ")");
			this.job = job;
		}

	}

}
