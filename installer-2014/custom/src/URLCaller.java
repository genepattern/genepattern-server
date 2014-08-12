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


/*
 * Created on Oct 10, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package custom;

import com.zerog.ia.api.pub.*;

import java.net.*;
import java.io.*;

/**
 * @author Liefeld
 *         <p/>
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class URLCaller extends CustomCodeAction {

    public URLCaller() {

    }


    public String getInstallStatusMessage() {

        return "CalculateShutdownPort";

    }


    public String getUninstallStatusMessage() {

        return "";

    }

    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {

        String running = "false";
        String message = "";

        try {
            String sport = (String) ip.getVariable("GENEPATTERN_PORT");

            URL url = new URL("http://localhost:" + sport + "/gp/index.jsp");
            System.out.println("Connecting to : " + url);

            HttpURLConnection uconn = (HttpURLConnection) url.openConnection();
            int rc = uconn.getResponseCode();


            if (rc < 400) {
                running = "true";
                message = "GenePattern appears to already be running on your system.";
            } else if (rc >= 400) {
                running = "true";
                message = "A web server appears to already be running on your system. ";
            }


            ip.setVariable("GENEPATTERN_RUNNING_TEST_RESPONSE", "" + rc);

        } catch (Exception e) {
            running = "false";
            ip.setVariable("GENEPATTERN_RUNNING_TEST_EXCEPTION", e.toString());
        }
        ip.setVariable("GENEPATTERN_RUNNING_TEST", running);
        ip.setVariable("GENEPATTERN_RUNNING_MESSAGE", message);

    }


    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        String running = "false";
        String message = "";
        try {
            String sport = (String) ip.getVariable("GENEPATTERN_PORT");

            URL url = new URL("http://localhost:" + sport + "/gp/index.jsp");
            System.out.println("Connecting to : " + url);

            HttpURLConnection uconn = (HttpURLConnection) url.openConnection();
            int rc = uconn.getResponseCode();
            if (rc < 400) {
                running = "true";
                message = "GenePattern appears to already be running on your system.";
            } else if (rc >= 400) {
                running = "true";
                message = "A web server appears to already be running on your system. ";
            }


            ip.setVariable("GENEPATTERN_RUNNING_TEST_RESPONSE", "" + rc);

        } catch (Exception e) {
            running = "false";
            ip.setVariable("GENEPATTERN_RUNNING_TEST_EXCEPTION", e.toString());
        }
        ip.setVariable("GENEPATTERN_RUNNING_TEST", running);
        ip.setVariable("GENEPATTERN_RUNNING_MESSAGE", message);


    }


/**
 * Call a URL from the command line and write the returned html to a file
 * passing along input parameters provided as name=value pairs
 * @param args
 */


}
