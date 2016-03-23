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
    
    private final String smtpServer;
    private final String replyTo; // aka replyTo
    private final String sendToAddress;
    private final String subject;
    private final String message;

    private MailSender(Builder b) {
        this.smtpServer=b.smtpServer;
        this.replyTo=b.replyTo;
        this.sendToAddress=b.sendToAddress;
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

        final Session mailSession = Session.getDefaultInstance(p, null);
        mailSession.setDebug(false);
        final MimeMessage msg = new MimeMessage(mailSession);

        try {
            msg.setSubject(subject);
            msg.setText(message);
            msg.setFrom(new InternetAddress(replyTo));
            msg.setSentDate(new Date());
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(sendToAddress));
            Transport.send(msg);
        } 
        catch (MessagingException e) {
            log.error(e);
            throw e;
        }
    }
    
    public static final class Builder {
        private String smtpServer;
        private String replyTo; // aka replyTo
        private String sendToAddress; // aka sendToAddress
        private String subject;
        private String message;
        
        public Builder() {
            this(ServerConfigurationFactory.instance(), 
                 GpContext.getServerContext());
        }
        /**
         * Pass in config objects to initialize default value for the 'smtp.server'
         * @param gpConfig, the server configuration instance
         * @param gpContext, the server context
         */
        public Builder(final GpConfig gpConfig, final GpContext gpContext) {
            this.smtpServer = gpConfig.getGPProperty(gpContext, PROP_SMTP_SERVER, DEFAULT_SMTP_SERVER);
        }
        
        public Builder smtpServer(final String smtpServer) {
            this.smtpServer=smtpServer;
            return this;
        }
        
        public Builder replyTo(final String replyTo) {
            this.replyTo=replyTo;
            return this;
        }
        
        public Builder sendToAddress(final String sendToAddress) {
            this.sendToAddress=sendToAddress;
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
