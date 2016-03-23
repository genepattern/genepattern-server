/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

/**
 *
 */
package org.genepattern.server.webapp.jsf;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.util.MailSender;

/**
 * This class is a JSF backing bean for the Contact Page, linked to from the 'Contact Us' link.
 * By default it will send an email to 'gp-help@broadinstitute.org' using the 'smtp.broadinstitute.org'
 * mail server.
 * These default values can be set via standard GP server configuration settings, e.g.
 * <pre>
 * contact.us.email=gp-help@broadinstitute.org
 * smtp.server=smtp.broadinstitute.org
 * </pre>
 * 
 * One way to change the default values is by adding or editing a custom property from the 'Server Settings > Custom' page.
 * 
 * @author pcarr
 *
 */
public class ContactUsBean {
    public static final String PROP_CONTACT_EMAIL="contact.us.email";
    public static final String DEFAULT_CONTACT_EMAIL="gp-help@broadinstitute.org";

    private String subject;

    private String replyTo;

    private String message;

    private boolean sent = false;

    private static Logger log = Logger.getLogger(ContactUsBean.class);

    public static final String EMAIL_REGEXP_PATTERN = "^[\\w-\\.]+@([\\w-]+\\.)([\\w-]+\\.[\\w-]+)*[\\w-]{2,4}$";

    public ContactUsBean() {
        User user = new UserDAO().findById(UIBeanHelper.getUserId());
        if (user != null) {
            replyTo = user.getEmail();
        }
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String send() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext gpContext=GpContext.getServerContext();
        final String contactUsEmail = gpConfig.getGPProperty(gpContext, PROP_CONTACT_EMAIL, DEFAULT_CONTACT_EMAIL);
        final MailSender m=new MailSender.Builder(gpConfig, gpContext)
            // set from
            .replyTo(replyTo)
            // set to
            .sendToAddress(contactUsEmail)
            // set subject
            .subject(subject)
            // set message
            .message(message)
        .build();
        try {
            m.sendMessage();
            this.sent=true;
            return "success";
        }
        catch (Exception e) {
            UIBeanHelper.setErrorMessage("An error occurred while sending the email.");
            return "failure";
        }
    }

    public boolean isSent() {
        return sent;
    }

    public String getInfoMessages() {
        if (!sent)
            return "An error occurred while sending the email.";
        return "Your request has been sent!";
    }

    public void validateEmail(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        String message = "The return address entered is not valid.";
        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);

        if (value == null) {
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
        String address = (String) value;
        try {
            InternetAddress emailAddr = new InternetAddress(address);
            if (!address.matches(EMAIL_REGEXP_PATTERN)) {
                ((UIInput) component).setValid(false);
                throw new ValidatorException(facesMessage);
            }
        } catch (AddressException ex) {
            ((UIInput) component).setValid(false);
            throw new ValidatorException(facesMessage);
        }
    }
}
