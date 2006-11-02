package org.genepattern.server.webapp.jsf;

import java.io.IOException;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.apache.myfaces.custom.navmenu.jscookmenu.HtmlCommandJSCookMenu;

public class NavBarBean {
    private static Logger log = Logger.getLogger(NavBarBean.class);

    public void navigate(ActionEvent event) {
        try {
            HtmlCommandJSCookMenu m = (HtmlCommandJSCookMenu) event.getSource();
            String cp = UIBeanHelper.getRequest().getContextPath();
            String label = m.getValue().toString();
            if (label.equalsIgnoreCase("create pipeline")) {
                UIBeanHelper.getResponse().sendRedirect(
                        cp + "/pipelineDesigner.jsp");
            } else if (label.equalsIgnoreCase("task catalog")) {
                UIBeanHelper.getResponse().sendRedirect(cp + "/pages/taskCatalog.jsf");           
            } else if (label.equalsIgnoreCase("create task")) {
                UIBeanHelper.getResponse().sendRedirect(cp + "/addTask.jsp");
            } else if (label.equalsIgnoreCase("task documentation")) {
                UIBeanHelper.getResponse().sendRedirect(cp + "/getTaskDoc.jsp");
            } else if (label.equalsIgnoreCase("create suite")) {
                UIBeanHelper.getResponse().sendRedirect(cp + "/pages/createSuite.jsf");
            } else if (label.equalsIgnoreCase("suite catalog")) {
                UIBeanHelper.getResponse().sendRedirect(
                        cp + "/suiteCatalog.jsp");
            } else if (label.equalsIgnoreCase("delete tasks")) {
                UIBeanHelper.getResponse().sendRedirect(cp + "/deleteTask.jsp");
            } else if (label.equalsIgnoreCase("delete suites")) {
                UIBeanHelper.getResponse()
                        .sendRedirect(cp + "/deleteSuite.jsp");
            } else {
                log.error("Unknown value: " + label);
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

}
