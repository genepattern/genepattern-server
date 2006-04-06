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

package org.genepattern.geworkbench;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.events.ProjectEvent;

@AcceptTypes( { DSMicroarraySet.class })
public class GPGEPlugin extends JPanel implements VisualPlugin {

    private DSMicroarraySet microarraySet;

    private GPGE instance;

    public GPGEPlugin() {
        instance = GPGE.getInstance();
        instance.setFrame(new HiddenFrame());
        instance.startUp(false);
        setLayout(new BorderLayout());
        add(instance.getFrame().getContentPane());
        add(instance.getFrame().getJMenuBar(), BorderLayout.NORTH);
    }

    public Component getComponent() {
        return this;
    }

    @Subscribe
    public void receive(ProjectEvent event, Object source) {
        DSDataSet dataSet = event.getDataSet();
        // We will act on this object if it is a DSMicroarraySet
        if (dataSet instanceof DSMicroarraySet) {
            microarraySet = (DSMicroarraySet) dataSet;
        }
    }

    private static class HiddenFrame extends JFrame {
        public void setVisible(boolean b) {
        }

        public void show() {
        }
    }

    public static void main(String[] args) {
        GPGEPlugin plugin = new GPGEPlugin();
        JFrame mainFrame = new JFrame();
        mainFrame.getContentPane().add(plugin.getComponent());
        mainFrame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        mainFrame.setVisible(true);
    }

}