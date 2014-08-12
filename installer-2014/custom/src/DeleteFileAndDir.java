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
 * RegisterGenePattern.java
 *
 * Created on January 14, 2004, 6:54 AM
 */
package custom;

import com.zerog.ia.api.pub.*;

import java.net.*;
import java.io.*;

/**
 * custom class for calculating a shutdown port for the GP server
 * <p/>
 * expects
 * GENEPATTERN_PORT = integer port for web server
 *
 * @author Liefeld
 */

public class DeleteFileAndDir extends CustomCodeAction {


    /**
     * Creates a new instance of RegisterGenePattern
     */

    public DeleteFileAndDir() {
    }


    public String getInstallStatusMessage() {
        return "DeleteFileAndDir ";
    }


    public String getUninstallStatusMessage() {
        return "";
    }


    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        System.out.println("==========================================");

        String filename = (String) ip.getVariable("FILE_TO_DELETE");
        String dirname = (String) ip.getVariable("DIR_TO_DELETE");

        if (filename != null) {
            System.out.println("Deleting file: " + filename);
            File file = new File(filename);
            if (file.exists()) {
                boolean success = file.delete();
                ip.setVariable("FILE_DELETED", (new Boolean(success)).toString());
                System.out.println("Deleting file: " + success);

            }
        }

        if (dirname != null) {
            File dir = new File(dirname);
            System.out.println("Deleting dir: " + dirname);

            if (dir.exists()) {
                boolean success = dir.delete();
                ip.setVariable("DIR_DELETED", (new Boolean(success)).toString());
                System.out.println("Deleting dir: " + success);

            }
        }
        System.out.println("==========================================");

    }


    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        System.out.println("==========================================");


        String filename = (String) ip.substitute("$FILE_TO_DELETE$");
        String dirname = (String) ip.substitute("$DIR_TO_DELETE$");
        System.out.println("Deleting requested: " + dirname + " -- " + filename);

        if (filename != null) {
            File file = new File(filename);
            if (file.exists()) {
                boolean success = file.delete();
                ip.setVariable("FILE_DELETED", (new Boolean(success)).toString());
                System.out.println("Deleting file: " + filename + " " + success);
            }
        }
        if (dirname != null) {
            File dir = new File(dirname);
            if (dir.exists()) {
                boolean success = recursiveDelete(dir);
                ip.setVariable("DIR_DELETED", (new Boolean(success)).toString());
                System.out.println("Deleting dir: " + dirname + " " + success);

            }
        }
        System.out.println("==========================================");
    }

    public boolean recursiveDelete(File dir) {

        File[] children = dir.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) recursiveDelete(child);
            else child.delete();
        }
        return dir.delete();
    }

}
