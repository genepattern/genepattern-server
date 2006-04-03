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

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.geworkbench.bison.datastructure.biocollections.DSDataSet;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;
import org.geworkbench.engine.management.Subscribe;
import org.geworkbench.events.ProjectEvent;

@AcceptTypes( { DSMicroarraySet.class })
public class GPGEPlugin extends JPanel implements VisualPlugin {

    private DSMicroarraySet microarraySet;

    private JLabel infoLabel;

    public GPGEPlugin() {
        infoLabel = new JLabel("");
        add(infoLabel);
    }

    public Component getComponent() {
        // In this case, this object is also the GUI component.
        return this;
    }

    @Subscribe
    public void receive(ProjectEvent event, Object source) {
        DSDataSet dataSet = event.getDataSet();
        // We will act on this object if it is a DSMicroarraySet
        if (dataSet instanceof DSMicroarraySet) {
            microarraySet = (DSMicroarraySet) dataSet;
            // We just received a new microarray set,
            // so populate the info label with some basic stats.
            String htmlText = "<html><body>" + "<h3>"
                    + microarraySet.getLabel() + "</h3><br>" + "<table>"
                    + "<tr><td>Arrays:</td><td><b>" + microarraySet.size()
                    + "</b></td></tr>" + "<tr><td>Markers:</td><td><b>"
                    + microarraySet.getMarkers().size() + "</b></td></tr>"
                    + "</table>" + "</body></html>";
            infoLabel.setText(htmlText);
        }
    }
}