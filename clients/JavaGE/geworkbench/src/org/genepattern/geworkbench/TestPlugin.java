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
import javax.xml.namespace.QName;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;

@AcceptTypes( { DSMicroarraySet.class })
public class TestPlugin extends JLabel implements VisualPlugin {

    public TestPlugin() {
        try {
            String endpoint = "http://localhost:8080/gp/services/Admin";
            Service service = new Service();
            Call call = (Call) service.createCall();

            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName(new QName(
                    "http://webservice.genepattern.org", "getServiceInfo"));

            Object ret = call.invoke(new Object[0]);
            setText("got '" + ret + "'");
        } catch (Exception e) {
            e.printStackTrace();
            setText(e.toString());
        }
    }

    public Component getComponent() {
        return this;
    }

    public static void main(String[] args) {
        new TestPlugin();
    }
}
