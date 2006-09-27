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
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserHome;

public class ForgotPasswordBean {
    private String email;
    private String username;

    private static Logger log = Logger.getLogger(ForgotPasswordBean.class);

    public String resetPassword() {
        // String hql = "from org.genepattern.server.user.User where email =
        // :email";
        // Query query = HibernateUtil.getSession().createQuery(hql);
        // query.setString("email", email);
        // User user = (User) query.uniqueResult();
        User user = new UserHome().findById(username);
        if (user == null) {
            UIBeanHelper.setInfoMessage("No user with the given username exists.");
            return "failure";
        }
        Properties p = new Properties();
        String mailServer = System.getProperty("smtp.server"); // FIXME
        if (mailServer == null) {
            UIBeanHelper.setInfoMessage("No SMTP server found. Please notify your GenePattern administrator.");
            return "failure";
        }
        p.put("mail.host", mailServer);
        Session mailSession = Session.getDefaultInstance(p, null);
        mailSession.setDebug(false);
        MimeMessage msg = new MimeMessage(mailSession);
        String newPassword = RandomStringUtils.random(8);
        try {
            msg.setSubject("Your GenePattern Password");
            msg.setText("Your GenePattern password has been reset to " + newPassword
                    + ".\nPlease log into GenePattern to update your password.");
            msg.setFrom(new InternetAddress("no-reply@genepattern.org")); // FIXME
            msg.setSentDate(new Date());
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            Transport.send(msg);
            user.setPassword(EncryptionUtil.encrypt(newPassword));
        }
        catch (MessagingException e) {
            log.error(e);
            UIBeanHelper.setInfoMessage("An error occurred while sending the email.");
            return "failure";
        }
        catch (NoSuchAlgorithmException e) {
            log.error(e);
        }
        UIBeanHelper.setInfoMessage("Your new password has been sent to " + email + ".");
        return "success";

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
