/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webapp.jsf;

import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.EncryptionUtil;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.genomespace.GenomeSpaceException;
import org.genepattern.server.genomespace.GenomeSpaceLoginManager;
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

    public String resetPassword() {
        HttpServletRequest request = UIBeanHelper.getRequest();
        try {
            GenomeSpaceLoginManager.resetPassword(request);
            UIBeanHelper.setInfoMessage("A temporary password was sent to your registered email.");
            return "success";
        }
        catch (GenomeSpaceException e) {
            UIBeanHelper.setErrorMessage(e.getMessage());
            return "failure";
        }
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
