/*******************************************************************************
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright (2003-2006) by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are
 * reserved.
 *  
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 *  
 *******************************************************************************/
package org.genepattern.gpge.ui.tasks;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeNode;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.message.ChangeViewMessage;
import org.genepattern.gpge.message.ChangeViewMessageRequest;
import org.genepattern.gpge.message.GPGEMessage;
import org.genepattern.gpge.message.GPGEMessageListener;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.ui.maindisplay.CenteredDialog;
import org.genepattern.gpge.ui.menu.MenuItemAction;
import org.genepattern.gpge.ui.table.AlternatingColorTable;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.AnalysisService;

public class ModuleMenuItemAction extends MenuItemAction implements
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

        if (message instanceof ChangeViewMessage && message.getSource() == this) {

            ChangeViewMessage cvm = (ChangeViewMessage) message;

            if (cvm.getType() != ChangeViewMessage.RUN_TASK_SHOWN) {

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

                String[] types = (String[]) typesIterator.next();

                if (SemanticUtil.isCorrectKind(types, kind)) {

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

                d.setVisible(true);

            }

        }

    }

}