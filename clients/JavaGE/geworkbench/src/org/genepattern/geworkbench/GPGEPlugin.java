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
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

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

    private GPGE instance;

    private GeWorkbenchProject workbenchProject;

    public GPGEPlugin() {
        try {
            instance = GPGE.getInstance();
            instance.setFrame(new HiddenFrame());
            instance.startUp(false);
            setLayout(new BorderLayout());
            add(instance.getFrame().getContentPane());
            add(instance.getFrame().getJMenuBar(), BorderLayout.NORTH);

        } catch (Throwable t) {
            t.printStackTrace();
        }
        workbenchProject = new GeWorkbenchProject();
        instance.getProjectDirectoryModel().add(workbenchProject);
    }

    public Component getComponent() {
        return this;
    }

    @Subscribe
    public void receive(ProjectEvent event, Object source) {
        DSDataSet dataSet = event.getDataSet();
        // We will act on this object if it is a DSMicroarraySet
        if (dataSet instanceof DSMicroarraySet) {
            DSMicroarraySet microarraySet = (DSMicroarraySet) dataSet;
            workbenchProject.add(microarraySet);
            instance.getProjectDirectoryModel().refresh(workbenchProject);
        }
    }

    private static class HiddenFrame extends JFrame {
        public void setVisible(boolean b) {
        }

        public void show() {
        }
    }

    static class GeWorkbenchProject extends File {
        static String name = "geWorkbench Files";

        List<File> children = new ArrayList<File>();

        private long lastModified;

        public GeWorkbenchProject() {
            super(name);
            lastModified = System.currentTimeMillis();
        }

        public void add(DSMicroarraySet microarraySet) {
            System.out.println("got data, file " + microarraySet.getFile()
                    + " label " + microarraySet.getLabel());
            if (!children.contains(microarraySet.getFile())) {
                children.add(microarraySet.getFile());
            }
        }

        public boolean isDirectory() {
            return true;
        }

        public boolean exists() {
            return true;
        }

        public String getName() {
            return name;
        }

        @Override
        public File[] listFiles(FileFilter fileFilter) {
            return children.toArray(new File[0]);
        }

        public long lastModified() {
            return lastModified;
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