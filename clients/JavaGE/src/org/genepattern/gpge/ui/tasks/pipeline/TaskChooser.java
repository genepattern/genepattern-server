package org.genepattern.gpge.ui.tasks.pipeline;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.genepattern.gpge.ui.maindisplay.CenteredDialog;
import org.genepattern.gpge.ui.tasks.AnalysisServiceManager;
import org.genepattern.gpge.ui.tasks.AnalysisServiceUtil;
import org.genepattern.webservice.AnalysisService;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
/**
 * A dialog that lets users choose a task to add to a pipeline
 * @author Joshua Gould
 *
 */
public class TaskChooser extends CenteredDialog {
	private JScrollPane tasksScrollPane;

	private JButton okBtn;

	private PipelineEditor pipelineComponent;

	private int indexToAdd;

	private JList tasksList;

	private void ok() {
		setVisible(false);
		//getOwner().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		AnalysisServiceWrapper wrapper = (AnalysisServiceWrapper) tasksList
				.getSelectedValue();
		pipelineComponent.addTask(indexToAdd, wrapper.svc);
		//getOwner().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	public TaskChooser(Frame parent, String title,
			PipelineEditor _pipelineComponent, int _indexToAdd) {
		super(parent);
		setTitle(title);
		this.pipelineComponent = _pipelineComponent;
		this.indexToAdd = _indexToAdd;
		tasksScrollPane = new JScrollPane();
		final Map categoryToServicesMap = AnalysisServiceUtil
				.getCategoryToAnalysisServicesMap(AnalysisServiceManager
						.getInstance().getLatestAnalysisServices());
		Vector categories = new Vector();
		for (Iterator it = categoryToServicesMap.keySet().iterator(); it
				.hasNext();) {
			categories.add(it.next());
		}
		final JList categoryList = new JList(categories);

		categoryList.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				okBtn.setEnabled(false);
				List tasks = (List) categoryToServicesMap.get(categoryList
						.getSelectedValue());
				AnalysisServiceWrapper[] taskWrappers = new AnalysisServiceWrapper[tasks
						.size()];
				for (int i = 0; i < tasks.size(); i++) {
					taskWrappers[i] = new AnalysisServiceWrapper(
							(AnalysisService) tasks.get(i));
				}
				tasksList = new JList(taskWrappers);
				tasksList.addMouseListener(new MouseAdapter() {
				     public void mouseClicked(MouseEvent e) {
				         if (e.getClickCount() == 2) {
				            ok();
				          }
				     }
				 });
				 
				tasksList.addListSelectionListener(new ListSelectionListener() {

					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting()) {
							return;
						}
						okBtn.setEnabled(true);
					}
				});

				tasksScrollPane.setViewportView(tasksList);
				tasksScrollPane.setVisible(true);
				invalidate();
				validate();
				tasksScrollPane.invalidate();
				tasksScrollPane.validate();
			}

		});

		JScrollPane categoryScrollPane = new JScrollPane(categoryList);
		categoryScrollPane.setColumnHeaderView(new JLabel("Category"));

		Container cp = getContentPane();

		FormLayout fl = new FormLayout("pref, pref", "pref");
		JPanel middlePanel = new JPanel(new GridLayout(1, 2));
		middlePanel.add(categoryScrollPane, BorderLayout.WEST);
		middlePanel.add(tasksScrollPane, BorderLayout.EAST);
		tasksScrollPane.setVisible(false);
		tasksScrollPane.setColumnHeaderView(new JLabel("Task"));
		JPanel buttonPanel = new JPanel();
		final JButton cancelBtn = new JButton("Cancel");
		okBtn = new JButton("OK");
		okBtn.setEnabled(false);
		ActionListener btnListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == okBtn) {
					ok();
				}
				dispose();
			}

		};
		cancelBtn.addActionListener(btnListener);
		okBtn.addActionListener(btnListener);
		buttonPanel.add(cancelBtn);
		buttonPanel.add(okBtn);
		cp.add(middlePanel, BorderLayout.CENTER);
		cp.add(buttonPanel, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(okBtn);
		setSize(450, 250);
		show();
	}
	

	private static class AnalysisServiceWrapper {
		AnalysisService svc;

		public AnalysisServiceWrapper(AnalysisService service) {
			this.svc = service;
		}

		public String toString() {
			return svc.getTaskInfo().getName();
		}
	}

}
