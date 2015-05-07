/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;

public class ForgotPasswordBean {

    private String username;

    private static Logger log = Logger.getLogger(ForgotPasswordBean.class);

    public String resetPassword() {
        User user = new UserDAO().findById(username);
        if (user == null) {
            UIBeanHelper.setErrorMessage("User not registered: " + username);
            return "failure";
        }
        String email = user.getEmail();
        if (email == null || email.length() == 0) {
            UIBeanHelper.setErrorMessage("No email address for username: "
                    + username);
            return "failure";
        }

        Properties p = new Properties();
        String mailServer = System.getProperty("smtp.server", "smtp.broadinstitute.org");
        p.put("mail.host", mailServer);
        Session mailSession = Session.getDefaultInstance(p, null);
        mailSession.setDebug(false);
        MimeMessage msg = new MimeMessage(mailSession);
        try {
            // use numeric instead of alphabetic characters so we accidentally
            // generate a 'naughty' word
            String newPassword = RandomStringUtils.randomNumeric(8);
            msg.setSubject("Your GenePattern Password");
            msg
                    .setText("Your GenePattern password has been reset to "
                            + newPassword
                            + ".\nPlease sign in to GenePattern to update your password.");
            msg.setFrom(new InternetAddress("no-reply@genepattern.org"));
            msg.setSentDate(new Date());
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
                    email));
            // try to encrypt the password before sending the email.
            byte[] encryptedPassword = EncryptionUtil.encrypt(newPassword);
            // try to send the email before changing the password.
            Transport.send(msg);
            user.setPassword(encryptedPassword);
            UIBeanHelper.setInfoMessage("Your new password has been sent to "
                    + email + ".");
        } 
        catch (NoSuchAlgorithmException e) {
            log.error(e);
            UIBeanHelper
                    .setErrorMessage("Server configuration error: Unable to encrypt password. "
                            + "\nContact the GenePattern server administrator for help.");
            return "failure";
        } 
        catch (MessagingException e) {
            log.error(e);
            UIBeanHelper
                    .setErrorMessage("Server configuration error: Unable to send email."
                            + "\nContact the GenePattern server administrator for help.");
            return "failure";
        }
        return "success";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
