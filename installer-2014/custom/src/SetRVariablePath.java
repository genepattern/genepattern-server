/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/


/*
 * 
 *
 * Created on January 14, 2004, 6:54 AM
 */

package custom;


import com.zerog.ia.api.pub.*;

import java.net.*;
import java.rmi.server.UID;
import java.io.*;

/**
 * custom class for adding /resources to the value of $R25$
 *
 * @author Liefeld
 */

public class SetRVariablePath extends CustomCodeAction {


    /**
     * Creates a new instance of RegisterGenePattern
     */

    public SetRVariablePath() {
    }

    public String getInstallStatusMessage() {
        return "SetRVariablePath ";
    }

    public String getUninstallStatusMessage() {
        return "";
    }


    public void install(com.zerog.ia.api.pub.InstallerProxy ip) throws com.zerog.ia.api.pub.InstallException {
        try {

            String R_new = ip.substitute("$R25$");

            // bug 1963
            // check if we are looking at an install that already has R25 set
            // as when 3.1 is installed over 3.1
            if (R_new.startsWith("<java")) {

                R_new = ip.substitute("$R2.5_HOME$");

            }
            // bug 2002 preserve old def for 3.1 on 3.1 with non-standard R25
            String oldR25 = ip.substitute("$R2.5_HOME$");// R2.5_HOME already defined in preread genepattern.props
            if (oldR25 != null) {
                if (oldR25.trim().length() > 0) {
                    R_new = oldR25;
                }
            }
            ip.setVariable("R_new", R_new);
            ip.setVariable("oldR25", oldR25);

            String os = System.getProperty("os.name").toLowerCase();

            if (os.indexOf("mac") >= 0) {
                if (!R_new.endsWith("/Resources")) {
                    ip.setVariable("R25bin", R_new + "/Resources");
                } else {
                    ip.setVariable("R25bin", R_new);
                }
                ip.setVariable("R25base", R_new);
            } else {
                ip.setVariable("R25bin", R_new);
                ip.setVariable("R25base", R_new);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void uninstall(com.zerog.ia.api.pub.UninstallerProxy uninstallerProxy) throws com.zerog.ia.api.pub.InstallException {
        // do nothing on uninstall

    }

}
