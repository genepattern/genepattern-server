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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.PropertyManager;
import org.genepattern.gpge.message.MessageManager;
import org.genepattern.gpge.message.PreferenceChangeMessage;
import org.genepattern.gpge.ui.preferences.PreferenceKeys;
import org.genepattern.gpge.ui.util.GUIUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class TaskNamePanel extends JPanel {

    public TaskNamePanel(TaskInfo taskInfo, final int type) {
        this(taskInfo, type, null);
    }

    /**
     * 
     * @param taskInfo
     * @param type
     *            the type of AnalysisServiceMessage to fire when the user
     *            selects a version from the versions combo box
     * @param bottomComponent
     *            the component to display at the bttom of this panel
     */
    public TaskNamePanel(TaskInfo taskInfo, final int type,
            Component bottomComponent) {
        super();
        String taskName = taskInfo.getName();
        if (taskName.endsWith(".pipeline")) {
            taskName = taskName.substring(0, taskName.length()
                    - ".pipeline".length());
        }

        JComboBox versionComboBox = new VersionComboBox((String) taskInfo
                .getTaskInfoAttributes().get(GPConstants.LSID), type);

        Component taskNameComponent = new JLabel(taskName);
        taskNameComponent.setFont(taskNameComponent.getFont().deriveFont(
                java.awt.Font.BOLD));

        Component description = GUIUtil.createWrappedLabel(taskInfo
                .getDescription());
        if (bottomComponent == null) {
            bottomComponent = createBottomComponent();
        }

        CellConstraints cc = new CellConstraints();
        JPanel temp = new JPanel(new FormLayout(
                "left:pref:none, 3dlu, pref, 3dlu, right:pref:none", "pref"));
        // icon, title, version

        temp.add(new JLabel(GenePattern.getSmallIcon()), cc.xy(1, 1));
        temp.add(taskNameComponent, cc.xy(3, 1));
        temp.add(versionComboBox, cc.xy(5, 1));

        JPanel temp2 = new JPanel(new BorderLayout());
        setLayout(new BorderLayout());
        temp2.add(temp, BorderLayout.NORTH);
        temp2.add(description, BorderLayout.SOUTH);
        add(temp2, BorderLayout.CENTER);
        add(bottomComponent, BorderLayout.SOUTH);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
    }

    protected Component createBottomComponent() {
        final JCheckBox showDescriptionsCheckBox = new JCheckBox(
                "Show Parameter Descriptions");
        boolean showDescriptions = Boolean
                .valueOf(
                        PropertyManager
                                .getProperty(PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS))
                .booleanValue();

        showDescriptionsCheckBox.setSelected(showDescriptions);
        showDescriptionsCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean showDescriptions = showDescriptionsCheckBox
                        .isSelected();
                PropertyManager.setProperty(
                        PreferenceKeys.SHOW_PARAMETER_DESCRIPTIONS, String
                                .valueOf(showDescriptions));
                MessageManager.notifyListeners(new PreferenceChangeMessage(
                        this,
                        PreferenceChangeMessage.SHOW_PARAMETER_DESCRIPTIONS,
                        showDescriptions));
            }
        });
        return showDescriptionsCheckBox;

    }

}
