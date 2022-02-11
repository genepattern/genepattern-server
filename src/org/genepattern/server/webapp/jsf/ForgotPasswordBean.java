/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.util.MailSender;

import javax.servlet.http.HttpServletRequest;

public class ForgotPasswordBean {
    private static Logger log = Logger.getLogger(ForgotPasswordBean.class);

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private void setMessageIfJSF(String type, String message) {
        boolean isFacesRequest = UIBeanHelper.getFacesContext() != null;

        if (isFacesRequest && "error".equals(type)) {
            UIBeanHelper.setErrorMessage(message);
        }
        else if (isFacesRequest && "info".equals(type)) {
            UIBeanHelper.setInfoMessage(message);
        }
        else if (isFacesRequest) {
            UIBeanHelper.setErrorMessage(message);
        }
    }

    private void setErrorIfJSF(String message) {
        setMessageIfJSF("error", message);
    }

    private void setInfoIfJSF(String message) {
        setMessageIfJSF("info", message);
    }

  

    private String resetPasswordDefault() {
        final User user = new UserDAO(HibernateUtil.instance()).findById(username);
        if (user == null) {
            setErrorIfJSF("User not registered: " + username);
            return "failure";
        }
        final String email = user.getEmail();
        if (email == null || email.length() == 0) {
            setErrorIfJSF("No email address for username: " + username);
            return "failure";
        }

        final String newPassword = RandomStringUtils.randomNumeric(8);
        byte[] encryptedPassword;
        try {
            // try to encrypt the password before sending the email.
            encryptedPassword = EncryptionUtil.encrypt(newPassword);
        }
        catch (NoSuchAlgorithmException e) {
            log.error(e);
            setErrorIfJSF("Server configuration error: Unable to encrypt password. "
                    + "\nContact the GenePattern server administrator for help.");
            return "failure";
        }

        try {
            sendResetPasswordMessage(email, newPassword);
            user.setPassword(encryptedPassword);
            setInfoIfJSF("Your new password has been sent to " + email + ".");
            return "success";
        }
        catch (Exception e) {
            log.error(e);
            setErrorIfJSF("Unable to send email to '"+email+"': " + e.getLocalizedMessage() + ". " +
                    "Contact the GenePattern server administrator for help.");
            return "failure";
        }
    }

    public String resetPassword() {
        GpContext context = UIBeanHelper.getUserContext();
        
            return resetPasswordDefault();
       
    }

    protected void sendResetPasswordMessage(final String to, final String newPassword) throws Exception {
        final MailSender m = new MailSender.Builder()
            .to(to)
            .subject("Your GenePattern Password")
            .message("Your GenePattern password has been reset to "
                            + newPassword
                            + ".\nPlease sign in to GenePattern to update your password.")
        .build();
        m.sendMessage();
    }
    
}
