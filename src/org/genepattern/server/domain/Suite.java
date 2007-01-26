/* Auto generated file */

package org.genepattern.server.domain;

import java.net.MalformedURLException;
import java.util.*;

import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.LSID;

import static org.genepattern.util.GPConstants.*;

/**
 * A hibernate mapped POJO representing a Suite. This class is a near copy of SuiteInfo. Both are kept for an interim
 * period as we transition to Hibernate.
 * 
 * @author jrobinso
 * 
 */
public class Suite implements java.io.Serializable {

    private String lsid;

    private String name;

    private String author;

    private String contact;

    private String description;

    private String userId;

    private Integer accessId;

    private List<String> modules;

    private boolean selected = false;

    private boolean expanded = true;

    public String getLsid() {
        return this.lsid;
    }

    public void setLsid(String value) {
        this.lsid = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String value) {
        this.author = value;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String value) {
        this.userId = value;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public Integer getAccessId() {
        return this.accessId;
    }

    public void setAccessId(Integer value) {
        this.accessId = value;
    }

    public List<String> getModules() {
        return modules;
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public String getVersion() {
        try {
            LSID lsidObject = new LSID(getLsid());
            return lsidObject.getVersion();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    /**
     * Test for "ownership". Ownership in this context means that the current user installed the suite on the server.
     * THis is a new concept in GP 3.0, and is supported by a new field (userId). Suites from previous versions of GP
     * will have a null value for the userId field. A value of true is returned for these instances, it is interpreted
     * to be a public suite irrespective of access value.  Otherwise a private suite with null user_id would not be
     * accessible by anyone.
     * 
     * @return true if the suite is owned by the current user
     */
    public boolean isOwnedByUserOrPublic() {
        String user = UIBeanHelper.getUserId();
        return (accessId == ACCESS_PUBLIC) || (userId == null) || (userId.length() == 0) || (userId.equals(user));
    }
}
