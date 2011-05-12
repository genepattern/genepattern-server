package org.genepattern.server.webapp.uploads;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.UploadFile;
import org.genepattern.server.domain.UploadFileDAO;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webapp.jsf.UsersAndGroupsBean;

/**
 * Created for debugging, 
 * Display direct uploaded files for current user.
 * @author pcarr
 *
 */
public class UploadFilesBean {
    private static Logger log = Logger.getLogger(UsersAndGroupsBean.class);
    
/*
 * JSF usage:
   #{uploadFileBean.user}
   #{uploadFileBean.files}"

      #{file.fileLength}
      #{file.lastModified}
      #{file.name}
      #{file.path}
*/
    private String currentUser;
    private List<UploadFile> files;

    public String getCurrentUser() {
        if (currentUser == null) {
            currentUser = UIBeanHelper.getUserId();
        }
        return currentUser;
    }

    public List<UploadFile> getFiles() {
        if (files == null) {
            initFiles();       
        }
        return files;
    }
    
    private void initFiles() {
        currentUser = UIBeanHelper.getUserId();
        List<UploadFile> tmp = new UploadFileDAO().findByUserId(currentUser);
        files = Collections.unmodifiableList(tmp);
    }
}

