/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.util.MailSender;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
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

  
    protected  boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    private String resetPasswordDefault() {
        UserDAO userDao = new UserDAO(HibernateUtil.instance());
        User user = userDao.findById(username);
        
        if ((user == null) && (isValidEmailAddress(username))) {
            List<User> usersForEmail = userDao.getUsersByEmail(username);
            if (usersForEmail.size() == 1) {
                user = usersForEmail.get(0);
            } else if  (usersForEmail.size() > 1) {
               ArrayList<String> usernames = new ArrayList<String>(usersForEmail.size()); 
              
               int i=0;
               for (User aUser: usersForEmail) {
                  
                   usernames.add(aUser.getUserId());
               }
               try {
                   sendCantResetPasswordSelectOneUsername(username, usernames);
                   setInfoIfJSF("An email has been sent to " + username + ".");
                   return "success";
               }
               catch (Exception e) {
                   log.error(e);
                   setErrorIfJSF("Unable to send email to '"+username+"': " + e.getLocalizedMessage() + ". " +
                           "Contact the GenePattern server administrator for help.");
                   return "failure";
               }
            }
        }
        
        
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
    
    
    protected void sendCantResetPasswordSelectOneUsername(final String to, List<String> allUsernames) throws Exception {
        StringBuffer allUsernamesFormatted = new StringBuffer();
        
        for (String username : allUsernames) {
            allUsernamesFormatted.append("\t");
            allUsernamesFormatted.append(username);
            allUsernamesFormatted.append(",\n");
        }
        // remove that last ',' which is not needed, but leave the newline
        allUsernamesFormatted.deleteCharAt(allUsernamesFormatted.length()-3);  
        
        // forgot password link like https://cloud.genepattern.org/gp/pages/forgotPassword.jsf
        GpConfig gpConfig =  ServerConfigurationFactory.instance();
        String forgotPassUrl = gpConfig.getGpUrl() + "/pages/forgotPassword.jsf";
        
        final MailSender m = new MailSender.Builder()
            .to(to)
            .subject("To reset your GenePattern Password")
            .message("Your GenePattern password has not been been reset because there are multiple accounts using the same email address. The usernames associated with this email address are: \n\n"
                            + allUsernamesFormatted.toString()
                            + "\nPlease select one and re-enter it in the GenePattern forgot password page ("
                            + forgotPassUrl +") to update your password.")
        .build();
        m.sendMessage();
    }
    
}
