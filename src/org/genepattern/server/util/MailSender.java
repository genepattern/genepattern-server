package org.genepattern.server.util;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;

import com.google.common.base.Strings;

/**
 * Generic java based mail sender class, based on the Contact Us Bean.
 * Created as a generic replacement for both the ContactUsBean and the EmailNotificationManager.
 * @author pcarr
 *
 */
public class MailSender {
    private static final Logger log = Logger.getLogger(MailSender.class);
    
    /** @deprecated pass in a valid GpConfig and GpContext */
    public static String getFromAddress() {
        return getFromAddress(ServerConfigurationFactory.instance(), GpContext.getServerContext());
    }
    
    public static String getFromAddress(final GpConfig gpConfig, final GpContext gpContext) {
        final String from = gpConfig.getGPProperty(
            gpContext, PROP_SMTP_FROM_EMAIL, DEFAULT_SMTP_FROM_EMAIL);
        return from;
    }

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
     * Set the 'mail.smtp.localhost' property as a workaround for the 
     * 'Helo command rejected: need fully-qualified hostname' error.
     *  Default: <null>
     */
    public static final String PROP_MAIL_SMTP_LOCALHOST="mail.smtp.localhost";

    /**
     * optionally set the 'smtp.auth.username'
     * When set, use this username to authenticate the smtp.server.
     */
    public static final String PROP_SMTP_AUTH_USERNAME="smtp.auth.username";

    /**
     * optionally set the 'smtp.auth.password', default=""
     */
    public static final String PROP_SMTP_AUTH_PASSWORD="smtp.auth.password";
    
    /**
     * optionally set the 'javax.mail.Session.properties' 
     * to fine tune the JavaMail configuration. 
     * Must be set as a map of <string,string> in the config file, e.g.
<pre>
    "javax.mail.Session.properties": {
        #"mail.host": "example.com",
        #"mail.smtp.host": "example.com",
        "mail.smtp.port": "587",
        "mail.smtp.auth", "true",
        "mail.smtp.starttls.enable": "true"
        "mail.debug.auth": "true",
        "mail.smtp.localhost": "example.com",
    }
    # Note: 'mail.smtp.host' is also set by the 'smtp.server' property, e.g.
    smtp.server: "example.com"
</pre>
     *
     */
    public static final String PROP_MAIL_SESSION_PROPERTIES="javax.mail.Session.properties";

    /**
     * Helper class that could be used as an alternative to calling:
     * <pre>
     *   Transport.send(msg, smtpAuthUsername, smtpAuthPassword);
     * </pre>
     */
    protected static final class SmtpAuthenticator extends javax.mail.Authenticator {
        final String smtp_auth_user;
        final String smtp_auth_pass;

        public SmtpAuthenticator(final String smtp_auth_user, final String smtp_auth_pass) {
            this.smtp_auth_user=smtp_auth_user;
            this.smtp_auth_pass=smtp_auth_pass;
        }

        public PasswordAuthentication getPasswordAuthentication() {
           return new PasswordAuthentication(smtp_auth_user, smtp_auth_pass);
        }
        
        /**
         * Example usage
         */
        protected static Session initSmtpSession(final Properties sessionProperties) {
            final String smtpAuthUsername="example_user";
            final String smtpAuthPassword="example_password";
            final javax.mail.Authenticator auth;
            if (Strings.isNullOrEmpty(smtpAuthUsername)) {
                auth=null;
            }
            else {
                auth=new SmtpAuthenticator(smtpAuthUsername, smtpAuthPassword);
            }
            final Session mailSession;
            if (auth==null) {
                mailSession = Session.getInstance(sessionProperties);
            }
            else {
                mailSession = Session.getInstance(sessionProperties, auth);
            }
            return mailSession;
        }
    }
    
    private final Properties sessionProperties;
    private final String smtpAuthUsername;
    private final String smtpAuthPassword;
    private final String from; // aka replyTo
    private final String to; // aka sendToAddress
    private final String subject;
    private final String message;

    private MailSender(Builder b) {
        this.sessionProperties=b.sessionProperties;
        this.smtpAuthUsername=b.smtpAuthUsername;
        this.smtpAuthPassword=b.smtpAuthPassword;
        this.from=b.from;
        this.to=b.to;
        this.subject=b.subject;
        this.message=b.message;
    }
    
    public Session initMailSession() {
        if (log.isDebugEnabled()) {
            log.debug("initializing mailSession ...");
            log.debug("              mail.host="+sessionProperties.getProperty("mail.host"));
            log.debug("         mail.smtp.host="+sessionProperties.getProperty("mail.smtp.host"));
            log.debug("    mail.smtp.localhost="+sessionProperties.getProperty("mail.smtp.localhost"));
            log.debug("    from: "+from);
            log.debug("    to: "+to);
            sessionProperties.put("mail.debug.auth", "true");
        }
        final Session mailSession = Session.getInstance(sessionProperties);
        if (log.isDebugEnabled()) {
            mailSession.setDebug(true);
        }
        return mailSession;
    }
    
    /**
     * Send the message
     * @throws Exception if there was a problem sending the message
     */
    public void sendMessage() throws Exception {
        final Session mailSession = initMailSession();
        final MimeMessage msg = new MimeMessage(mailSession);
        final InternetAddress fromAddr=new InternetAddress(from);
        final InternetAddress toAddr=new InternetAddress(to);
        if (log.isDebugEnabled()) {
            log.debug("fromAddr: "+fromAddr.toString());
            log.debug("toAddr: "+toAddr.toString());
        }
        msg.setSubject(subject);
        msg.setFrom(fromAddr);
        msg.addRecipient(Message.RecipientType.TO, toAddr);
        msg.setSentDate(new Date());
        msg.setText(message);
        sendMessage(msg);
    }
    
    public void sendMessage(final Message msg) throws MessagingException {
        try {
            Transport.send(msg, smtpAuthUsername, smtpAuthPassword);
        }
        catch (MessagingException e) {
            log.error(e);
            throw e;
        }
    } 
    
    public void sendMessage(final Message msg, final Address[] addresses) throws MessagingException {
        try {
            Transport.send(msg, addresses, smtpAuthUsername, smtpAuthPassword);
        }
        catch (MessagingException e) {
            log.error(e);
            throw e;
        }
    }

    public static final class Builder {
        private Properties sessionProperties=new Properties();
        private String smtpAuthUsername=null;
        private String smtpAuthPassword="";
        private String from; // aka replyTo
        private String to;   // aka sendToAddress, aka recipient
        private String subject;
        private String message;
        
        public Builder() {
            this(ServerConfigurationFactory.instance(), 
                 GpContext.getServerContext());
        }
        
        /**
         * Initialize default values from the server configuration file.
         */
        public Builder(final GpConfig gpConfig, final GpContext gpContext) {
            // initialize session properties from config file
            sessionProperties.setProperty("mail.smtp.host", 
                gpConfig.getGPProperty(gpContext, PROP_SMTP_SERVER, DEFAULT_SMTP_SERVER ));
            this.smtpAuthUsername = gpConfig.getGPProperty(gpContext,
                PROP_SMTP_AUTH_USERNAME );
            this.smtpAuthPassword = gpConfig.getGPProperty(gpContext,
                PROP_SMTP_AUTH_PASSWORD, "" );
            this.from = gpConfig.getGPProperty(gpContext, 
                PROP_SMTP_FROM_EMAIL, DEFAULT_SMTP_FROM_EMAIL );
            final String mailSmtpLocalhost = gpConfig.getGPProperty(gpContext, PROP_MAIL_SMTP_LOCALHOST);
            if (!Strings.isNullOrEmpty(mailSmtpLocalhost)) {
                sessionProperties.setProperty(PROP_MAIL_SMTP_LOCALHOST, mailSmtpLocalhost);
            }
            
            final Value propsFromConfig=gpConfig.getValue(gpContext, "javax.mail.Session.properties");
            if (propsFromConfig != null && propsFromConfig.isMap()) {
                try {
                    sessionProperties.putAll(propsFromConfig.getMap());
                }
                catch (Throwable t) {
                    log.error("error parsing 'javax.mail.Session.properties' from config_yaml file: "+t.getMessage(), t);
                }
            }
        }
        
        public Builder smtpServer(final String smtpServer) {
            sessionProperties.setProperty("mail.smtp.host", smtpServer);
            return this;
        }
        
        public Builder addProperty(final String key, final String value) {
            sessionProperties.put(key, value);
            return this;
        }
        
        public Builder smtpAuthUsername(final String smtpAuthUsername) {
            this.smtpAuthUsername=smtpAuthUsername;
            return this;
        }
        
        public Builder smtpAuthPassword(final String smtpAuthPassword) {
            this.smtpAuthPassword=smtpAuthPassword;
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
