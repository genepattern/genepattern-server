package org.genepattern.server.webapp.uploads;

import org.genepattern.server.webapp.jsf.UIBeanHelper;

public class JumploaderBean {
    /**
     * Need this method to both 
     *     1) display the target directory for the uploaded file to the end user, and
     *     2) set the target directory, via the 'uploadPath' session parameter, for the UploadReceiver
     * 
     * Note: this is a non-standard use of JSF, I couldn't figure out a better way to set a session parameter
     *     from a request parameter.
     * We are directly loading the jumploader applet page with a JavaScript client call to open a new window.
     * @return
     */
    public String getDirName() {
        final String dirName = UIBeanHelper.getRequest().getParameter("dirName"); //aka uploadPath
        if (dirName != null) {
            UIBeanHelper.getSession().setAttribute("uploadPath", dirName);
            //special-case, for GUI, replace './' or '.' with empty string
            if ("./".equals(dirName) || ".".equals(dirName)) {
                return "";
            }
            return dirName;
        }
        return "Not set: missing required request parameter, 'dirName'";
    }

}
