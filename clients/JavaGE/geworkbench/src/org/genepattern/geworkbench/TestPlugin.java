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

import org.apache.axis.EngineConfiguration;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.BasicClientConfig;
import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.engine.config.VisualPlugin;
import org.geworkbench.engine.management.AcceptTypes;

import javax.swing.JLabel;
import javax.xml.namespace.QName;
import java.awt.Component;

@AcceptTypes({DSMicroarraySet.class})
public class TestPlugin extends JLabel implements VisualPlugin {

    public TestPlugin() {
        try {
            String endpoint = "http://localhost:8080/gp/services/Admin";
            EngineConfiguration ec = new BasicClientConfig();
            Service service = new Service(ec);
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
