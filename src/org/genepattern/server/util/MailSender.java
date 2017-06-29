package org.genepattern.server.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;

/**
 * Generic java based mail sender class, based on the Contact Us Bean.
 * Created as a generic replacement for both the ContactUsBean and the EmailNotificationManager.
 * @author pcarr
 *
 */
public class MailSender {
    private static final Logger log = Logger.getLogger(MailSender.class);

    /**
     * Set the 'smtp.server' property to change the mail.host that the javax.mail.Transport class uses to 
     * send email messages.
     */
    public static final String PROP_SMTP_SERVER="smtp.server";
    /** The default smtp.server=smtp.broadinstitute.org */
    public static final String DEFAULT_SMTP_SERVER="smtp.broadinstitute.org";
    
    /** 
     * Set the 'smtp.from.email' for notifications sent to end-users, such as from the 'forgot your password' link.
     */
    public static final String PROP_SMTP_FROM_EMAIL="smtp.from.email";
    /** The default smtp.from.email=no-reply@genepattern.org */
    public static final String DEFAULT_SMTP_FROM_EMAIL="no-reply@genepattern.org";
    
    /**
     * Set the 'mail.smtp.localhost' property to the fully qualified hostname for the server.
     * This is only needed as a workaround for the 'Helo command rejected: need fully-qualified hostname' error. 
     */
    public static final String PROP_MAIL_SMTP_LOCALHOST="mail.smtp.localhost";
    
    private final String smtpServer;
    private final String mailSmtpLocalhost;
    private final String from; // aka replyTo
    private final String to; // aka sendToAddress
    private final String subject;
    private final String message;
    private MailSender(Builder b) {
        this.smtpServer=b.smtpServer;
        this.mailSmtpLocalhost=b.mailSmtpLocalhost;
        this.from=b.from;
        this.to=b.to;
        this.subject=b.subject;
        this.message=b.message;
    }
    
    /**
     * Send the message
     * @throws Exception if there was a problem sending the message
     */
    public void sendMessage() throws Exception {
        final Properties p = new Properties();
        p.put("mail.host", smtpServer);
        if (mailSmtpLocalhost != null) {
            p.put(PROP_MAIL_SMTP_LOCALHOST, mailSmtpLocalhost);
        }
        if (log.isDebugEnabled()) {
            log.debug("initializing mailSession ...");
            log.debug("    mail.host="+smtpServer);
            log.debug("    mail.smtp.localhost="+mailSmtpLocalhost);
            log.debug("from: "+from);
            log.debug("to: "+to);
        }
        final Session mailSession = Session.getInstance(p);
        if (log.isDebugEnabled()) {
            mailSession.setDebug(true);
        }

        final MimeMessage msg = new MimeMessage(mailSession);
        try {
            final InternetAddress fromAddr=new InternetAddress(from);
            final InternetAddress toAddr=new InternetAddress(to);
            if (log.isDebugEnabled()) {
                log.debug("fromAddr: "+fromAddr.toString());
                log.debug("toAddr: "+toAddr.toString());
            }
            msg.setSubject(subject);
            msg.setText(message);
            msg.setFrom(fromAddr);
            msg.setSentDate(new Date());
            msg.addRecipient(Message.RecipientType.TO, toAddr);
            Transport.send(msg);
        } 
        catch (MessagingException e) {
            log.error(e);
            throw e;
        }
    }
    
    public static final class Builder {
        private String smtpServer;
        private String mailSmtpLocalhost;
        private String from; // aka replyTo
        private String to;   // aka sendToAddress, aka recipient
        private String subject;
        private String message;
        
        public Builder() {
            this(ServerConfigurationFactory.instance(), 
                 GpContext.getServerContext());
        }
        /**
         * Pass in config objects to initialize default values for the 'smtp.server'
         * @param gpConfig, the server configuration instance
         * @param gpContext, the server context
         */
        public Builder(final GpConfig gpConfig, final GpContext gpContext) {
            this.smtpServer = gpConfig.getGPProperty(gpContext, PROP_SMTP_SERVER, DEFAULT_SMTP_SERVER);
            this.from = gpConfig.getGPProperty(gpContext, PROP_SMTP_FROM_EMAIL, DEFAULT_SMTP_FROM_EMAIL);
            this.mailSmtpLocalhost = gpConfig.getGPProperty(gpContext, PROP_MAIL_SMTP_LOCALHOST);
        }
        
        public Builder smtpServer(final String smtpServer) {
            this.smtpServer=smtpServer;
            return this;
        }
        
        public Builder from(final String from) {
            this.from=from;
            return this;
        }
        
        public Builder to(final String to) {
            this.to=to;
            return this;
        }
        
        public Builder subject(final String subject) {
            this.subject=subject;
            return this;
        }
        
        public Builder message(final String message) {
            this.message=message;
            return this;
        }
        
        public MailSender build() {
            return new MailSender(this);
        }
    }
}
