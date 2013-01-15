package org.genepattern.server.webapp.jsf;

import java.io.Serializable;
import java.util.Date;

import javax.faces.event.ActionEvent;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.message.SystemAlertFactory;
import org.genepattern.server.message.SystemMessage;

/**
 * Session scope bean for displaying the system alert message to the current user.
 * This bean keeps track of the view state of the message.
 * <pre>
       NEW -(open)--> OPEN -(close)--> CLOSED -(open)--> ...
 * </pre>
 * 
 * @author pcarr
 */
public class SystemMessageBean implements Serializable {
    private static Logger log = Logger.getLogger(SystemMessageBean.class);    

    //the current system message, queried from the database
    private SystemMessage systemMessage = null;

    //to prevent doing a database query for each page load, change the delay to some value other than zero.
    private static long delay = 1000; //number of  millisecs to wait before querying the database again
    private long lastLookup = System.currentTimeMillis() - delay;

    private boolean isNew = true;
    private boolean isOpen = true;
    //if this is true the getMessageAsJavaScriptString will
    private boolean enableJavaScriptAlert = false;
    
    /**
     * Initialize.
     */
    public SystemMessageBean() {
        lookupSystemMessage(new Date());
        enableJavaScriptAlert = Boolean.valueOf(System.getProperty("systemMessage.enableJavaScriptAlert"));
    }

    /**
     * Load system message from database.
     */
    private void checkSystemMessage() {
        Date now = new Date();
        if ((now.getTime() - delay) > lastLookup) {
            lookupSystemMessage(now);
        }
    }
    private void lookupSystemMessage(Date now) {
        SystemMessage prevMessage = this.systemMessage;
        try {
            systemMessage = SystemAlertFactory.getSystemAlert().getSystemMessage(now);
        }
        catch (Exception e) {
            log.error(e);
        }
        lastLookup = now.getTime();
        
        if (prevMessage == null && systemMessage == null) {
            return;
        }
        if (prevMessage != null && systemMessage != null && 
                prevMessage.getId().equals(systemMessage.getId()) && 
                prevMessage.getMessage().equals(systemMessage.getMessage())) {
            return;
        }
        //if the message has changed, clear flags
        isNew = true;
        isOpen = true;
    }

    /**
     * @return the current SystemMessage.
     * @throws Exception
     */
    public SystemMessage getSystemMessage() throws Exception {
        checkSystemMessage();
        return systemMessage;
    }

    public boolean getEnableJavaScriptAlert() {
        return this.enableJavaScriptAlert;
    }

    /**
     * @return format the system message for display in a javascript alert window.
     */
    public String getMessageAsJavaScriptString() {
        if (!enableJavaScriptAlert) {
            return "";
        }
        checkSystemMessage();
        if  (systemMessage == null) {
            return "";
        }
        //a little trick, the state changes the first time the javascript text is accessed
        isNew = false; 
        String msg = systemMessage.getMessage();
        msg = msg.replaceAll("\\n", "\\\\n");
        msg = msg.replaceAll("\\r", "");
        return msg;
    }

    /**
     * @return format the system message for display in a web page.
     */
    public String getMessageAsHtmlString() {
        checkSystemMessage();
        if (systemMessage == null) {
            return "";
        }
        String msg = systemMessage.getMessage();
        msg = msg.replaceAll("\\n", "<br />");
        msg = msg.replaceAll("\\r", "");
        return msg;
    }
    
    public boolean getIsNew() {
        checkSystemMessage();
        return isNew;
    }
    
    public boolean getIsOpen() {
        checkSystemMessage();
        return isOpen;
    }

    public void hide(ActionEvent evt) {
        this.isOpen = false;
    }
    
    public void show(ActionEvent evt) {
        this.isOpen = true;
    }
    
    public String getSkin() {
        String env = ServerConfiguration.instance().getGPProperty(UIBeanHelper.getUserContext(), "display.skin", ".");
        return env;
    }
}
