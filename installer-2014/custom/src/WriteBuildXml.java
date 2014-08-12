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

 * 
 *

 * Created on January 14, 2004, 6:54 AM

 */

package custom;


import com.zerog.ia.api.pub.*;

import java.net.*;

import java.io.*;

/**
 * custom class for calculating a shutdown port for the GP server
 * expects
 * GENEPATTERN_PORT = integer port for web server
 *
 * @author Liefeld
 */

public class WriteBuildXml extends CustomCodeAction {


    /**
     * Creates a new instance of RegisterGenePattern
     */

    public WriteBuildXml() {
    }

    public String getInstallStatusMessage() {
        return "WriteBuildXml ";
    }

    public String getUninstallStatusMessage() {
        return "";
    }


    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        try {

            Object obj = ip.getVariable("USER_INSTALL_DIR");
            System.out.println("OBJ=" + obj.getClass().getName());

            String instDirName = (ip.getVariable("USER_INSTALL_DIR")).toString();
            String zipDirName = (ip.getVariable("OLD_TASKS_ZIP_DIR")).toString();


            File tomDir = new File(instDirName + "/Tomcat");
            // if it is there, put a build.xml into it
            if (tomDir.exists()) {
                File buildxml = new File(tomDir, "zip.xml");
                BufferedWriter buff = new BufferedWriter(new FileWriter(buildxml));
                buff.write(contents(zipDirName));
                buff.flush();
                buff.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String contents(String zipDirName) {
        StringBuffer buff = new StringBuffer();

        buff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buff.append("<project default=\"zip\" name=\"zip\">");
        buff.append("<target name=\"zip\">\n");
        buff.append("<java classname=\"edu.mit.genome.gp.server.ZipTask\" fork=\"true\">\n");
        buff.append("<jvmarg value=\"-Dgenepattern.properties=${basedir}/../resources\"/>\n");
        buff.append("<jvmarg value=\"-Dlog4j.configuration=${basedir}/webapps/gp/WEB-INF/classes/log4j.properties\"/>\n");
        buff.append("<arg value=\"${basedir}/../" + zipDirName + "\"/>\n");
        buff.append("<classpath>\n");
        buff.append("<fileset dir=\"${basedir}/webapps/gp/WEB-INF/lib\">\n");
        buff.append("<include name=\"**/*.jar\"/>\n");
        buff.append("</fileset>\n");
        buff.append("</classpath>\n");
        buff.append("</java>\n");
        buff.append("</target>\n");
        buff.append("</project>\n");
        buff.append("\n");

        return buff.toString();

    }


    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {
        // do nothing on uninstall

    }

}
