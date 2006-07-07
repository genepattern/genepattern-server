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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.gpge.GenePattern;
import org.genepattern.gpge.ui.maindisplay.GPGE;
import org.genepattern.io.expr.gct.GctWriter;
import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.bioobjects.markers.DSGeneMarker;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.events.ProjectEvent;

@AcceptTypes( { DSMicroarraySet.class })
public class GPGEPlugin extends JPanel implements VisualPlugin {

    private GPGE instance;

    private File projectDirectory;

    public GPGEPlugin() {
        projectDirectory = new File("geWorkbench");
        projectDirectory.mkdirs();
        instance = GPGE.getInstance();
        HiddenFrame f = new HiddenFrame();
        GenePattern.setDialogParent(f);
        instance.setFrame(f);
        instance.startUp(false);
        setLayout(new BorderLayout());
        add(instance.getFrame().getContentPane());
        add(instance.getFrame().getJMenuBar(), BorderLayout.NORTH);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                File[] files = projectDirectory.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".gct");
                    }
                });
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        files[i].delete();
                    }
                }
            }
        });

    }

    public Component getComponent() {
        return this;
    }

    @Subscribe
    public void receive(ProjectEvent event, Object source) {
        DSDataSet dataSet = event.getDataSet();

        if (dataSet instanceof DSMicroarraySet) {
            if (!instance.getProjectDirectoryModel().contains(projectDirectory)) {
                instance.getProjectDirectoryModel().add(projectDirectory);
            }

            DSMicroarraySet microarraySet = (DSMicroarraySet) dataSet;
            toGct(microarraySet);
            instance.getProjectDirectoryModel().refresh(projectDirectory);
        }
    }

    void toGct(final DSMicroarraySet microarraySet) {

        IExpressionData data = new IExpressionData() {

            public String getValueAsString(int row, int column) {
                return String.valueOf(microarraySet.getValue(row, column));
            }

            public boolean containsData(String name) {
                return false;
            }

            public Object getData(int row, int column, String name) {
                return null;
            }

            public boolean containsRowMetadata(String name) {
                return false;
            }

            public boolean containsColumnMetadata(String name) {
                return false;
            }

            public String getRowMetadata(int row, String name) {
                return null;
            }

            public String getColumnMetadata(int column, String name) {
                return null;
            }

            public double getValue(int row, int column) {
                return microarraySet.getValue(row, column);
            }

            public String getRowName(int row) {
                Object obj = microarraySet.getMarkers().get(row);
                if (obj instanceof DSGeneMarker) {
                    DSGeneMarker marker = (DSGeneMarker) obj;
                    return marker.getLabel();
                }
                return obj.toString();
            }

            public int getRowCount() {
                return microarraySet.getMarkers().size();
            }

            public int getColumnCount() {
                return microarraySet.size();
            }

            public String getColumnName(int column) {
                return microarraySet.get(column).toString();
            }

            public int getRowIndex(String rowName) {
                return 0;
            }

            public int getColumnIndex(String columnName) {
                return 0;
            }

        };
        GctWriter writer = new GctWriter();
        OutputStream os = null;
        try {
            File f = microarraySet.getFile();
            String name = f.getName();
            int dotIndex = name.lastIndexOf(".");
            if (dotIndex > 0) {
                name = name.substring(0, dotIndex);
            }
            os = new BufferedOutputStream(new FileOutputStream(new File(
                    projectDirectory, name + ".gct")));
            writer.write(data, os);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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