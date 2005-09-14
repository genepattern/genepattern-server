package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeNode;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessage;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.*;
import org.genepattern.gpge.ui.menu.*;
import org.genepattern.gpge.ui.table.*;
import org.genepattern.io.*;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.*;

/**
 * Utility methods for semantic information
 * 
 * @author Joshua Gould
 */
public class SemanticUtil {

	public static class ModuleMenuItemAction extends MenuItemAction implements
			ActionListener, GPGEMessageListener {
		/** Currently selected node */
		TreeNode node;

		/** Kind of selected node */
		String kind;

		/** The <tt>AnalysisService</tt> that this menu item represents */
		AnalysisService svc;

		public ModuleMenuItemAction(AnalysisService svc) {
			super(svc.getTaskInfo().getName());
			MessageManager.addGPGEMessageListener(this);
			this.svc = svc;
			addActionListener(this);
		}

		public void setTreeNode(TreeNode node, String kind) {
			this.node = node;
			this.kind = kind;
		}

		public void actionPerformed(ActionEvent e) {
			MessageManager.notifyListeners(new ChangeViewMessageRequest(this,
					ChangeViewMessageRequest.SHOW_RUN_TASK_REQUEST, svc));
		}

		public void receiveMessage(GPGEMessage message) {

			if (message instanceof ChangeViewMessage && message.getSource()==this) {
				ChangeViewMessage cvm = (ChangeViewMessage) message;
				if(cvm.getType()!=ChangeViewMessage.RUN_TASK_SHOWN) {
					return;
				}
				ChangeViewMessage changeViewMessage = (ChangeViewMessage) message;
				final TaskDisplay analysisServiceDisplay = (TaskDisplay) changeViewMessage
						.getComponent();

				final List matchingInputParameters = new ArrayList();
				Iterator typesIterator = analysisServiceDisplay.getInputFileTypes();
				for (Iterator it = analysisServiceDisplay
						.getInputFileParameters(); it.hasNext();) {
					String name = (String) it.next();
					String[] types = (String[])  typesIterator.next();
					if (isCorrectKind(types, kind)) {
						matchingInputParameters.add(name);
					}
				}
				if (matchingInputParameters.size() == 1) {
					String inputParameter = (String) matchingInputParameters
							.get(0);
					analysisServiceDisplay.sendTo(inputParameter, (Sendable) node);
				} else if (matchingInputParameters.size() > 1) {
					final JDialog d = new CenteredDialog(GenePattern
							.getDialogParent());
					TableModel model = new AbstractTableModel() {
						public int getColumnCount() {
							return 1;
						}

						public int getRowCount() {
							return matchingInputParameters.size();
						}

						public String getColumnName(int j) {
							return "Parameter";
						}

						public Class getColumnClass(int j) {
							return String.class;
						}

						public Object getValueAt(int r, int c) {
							String p = (String) matchingInputParameters
									.get(r);
							return p;
						}
					};
					final JButton ok = new JButton("OK");
					final JButton cancel = new JButton("Cancel");
					final JTable t = new AlternatingColorTable(model);

					t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					t.setRowSelectionInterval(0, 0);
					d.setTitle("Send " + node.toString() + " To");
					d.getContentPane().add(new JScrollPane(t));

					final ActionListener listener = new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if (e.getSource() == ok) {
								int row = t.getSelectedRow();
								if (row < 0) {
									return;
								}
								String inputParameter = (String) matchingInputParameters
										.get(row);
								analysisServiceDisplay.sendTo(
										inputParameter, (Sendable) node);
							}
							d.dispose();
						}
					};
					t.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent e) {
							if (e.getClickCount() == 2) {
								listener.actionPerformed(new ActionEvent(ok,
										ActionEvent.ACTION_PERFORMED, ""));
							}
						}
					});
					ok.addActionListener(listener);
					cancel.addActionListener(listener);
					JPanel buttonPanel = new JPanel();
					buttonPanel.add(cancel);
					buttonPanel.add(ok);
					d.getRootPane().setDefaultButton(ok);
					d.getContentPane().add(BorderLayout.SOUTH, buttonPanel);
					d.setSize(300, 200);
					d.show();
				}
			}
		}
	}

	private SemanticUtil() {
	}

	public static String getType(File file) {
		String name = file.getName();
		int dotIndex = name.lastIndexOf(".");
		String extension = null;
		if (dotIndex > 0) {
			extension = name.substring(dotIndex + 1, name.length());
		}
		if (extension.equalsIgnoreCase("odf")) {
			OdfParser parser = new OdfParser();
			OdfHandler handler = new OdfHandler();
			FileInputStream fis = null;
			parser.setHandler(handler);
			try {
				fis = new FileInputStream(file);
				parser.parse(fis);
			} catch (Exception e) {
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (IOException x) {
				}
			}
			return handler.model;
		} else {
			return extension.toLowerCase();
		}
	}

	private static class OdfHandler implements IOdfHandler {
		String model;

		public void endHeader() throws ParseException {
			throw new ParseException("");
		}

		public void header(String key, String[] values) throws ParseException {

		}

		public void header(String key, String value) throws ParseException {
			if (key.equals("Model")) {
				model = value;
				throw new ParseException("");
			}
		}

		public void data(int row, int column, String s) throws ParseException {
			throw new ParseException("");
		}
	}

	/**
	 * Returns <code>true</code> if the given kind is an acceptable input file
	 * format for the given input parameter
	 */
	public static boolean isCorrectKind(String[] inputTypes, String kind) {
		if (inputTypes == null || inputTypes.length==0) {
			return false;
		}
		if(kind == null || kind.equals("")) {
			return true;
		}
		return Arrays.asList(inputTypes).contains(kind);
	}

	private static Map _getInputTypeToMenuItemsMap(Map inputTypeToModulesMap) {
		Map inputTypeToMenuItemMap = new HashMap();
		for (Iterator it = inputTypeToModulesMap.keySet().iterator(); it
				.hasNext();) {
			String type = (String) it.next();
			List modules = (List) inputTypeToModulesMap.get(type);

			if (modules == null) {
				continue;
			}
			ModuleMenuItemAction[] m = new ModuleMenuItemAction[modules.size()];
			java.util.Collections.sort(modules,
					AnalysisServiceUtil.CASE_INSENSITIVE_TASK_NAME_COMPARATOR);
			for (int i = 0; i < modules.size(); i++) {
				final AnalysisService svc = (AnalysisService) modules.get(i);
				m[i] = new ModuleMenuItemAction(svc);
			}
			inputTypeToMenuItemMap.put(type, m);
		}
		return inputTypeToMenuItemMap;
	}

	/**
	 * Gets a map which maps the input type as a string to an array of
	 * ModuleMenuItemAction instances
	 */
	public static Map getInputTypeToMenuItemsMap(Collection analysisServices) {
		Map inputTypeToModulesMap = SemanticUtil
				.getInputTypeToModulesMap(analysisServices);
		return SemanticUtil._getInputTypeToMenuItemsMap(inputTypeToModulesMap);
	}

	/**
	 * Gets a map which maps the input type as a string to a list of analysis
	 * services that take that input type as an input parameter
	 */
	private static Map getInputTypeToModulesMap(Collection analysisServices) {
		Map map = new HashMap();
		for (Iterator it = analysisServices.iterator(); it.hasNext();) {
			AnalysisService svc = (AnalysisService) it.next();
			addToInputTypeToModulesMap(map, svc);
		}
		return map;
	}

	private static void addToInputTypeToModulesMap(Map map, AnalysisService svc) {
		TaskInfo taskInfo = svc.getTaskInfo();
		ParameterInfo[] p = taskInfo.getParameterInfoArray();
		if (p != null) {
			for (int i = 0; i < p.length; i++) {
				if (p[i].isInputFile()) {
					ParameterInfo info = p[i];
					String fileFormatsString = (String) info.getAttributes()
							.get(GPConstants.FILE_FORMAT);
					if (fileFormatsString == null
							|| fileFormatsString.equals("")) {
						continue;
					}
					StringTokenizer st = new StringTokenizer(fileFormatsString,
							GPConstants.PARAM_INFO_CHOICE_DELIMITER);
					while (st.hasMoreTokens()) {
						String type = st.nextToken();
						List modules = (List) map.get(type);
						if (modules == null) {
							modules = new ArrayList();
							map.put(type, modules);
						}
						if (!modules.contains(svc)) {
							modules.add(svc);
						}

					}
				}
			}
		}
	}

}
