/**
 * 
 */
package org.genepattern.server.webapp.jsf;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;



public class ContactUsBean {
	
	private String replyTo;
	private String message;
	private boolean sent=false;
	
	private static Logger log = Logger.getLogger(ContactUsBean.class);
	
	/**
	 * @return
	 */
	public String getReplyTo() {
		return replyTo;
	}
	
	/**
	 * @param replyTo
	 */
	public void setReplyTo(String replyTo) {
		this.replyTo=replyTo;
	}
	
	/**
	 * @return
	 */
	public String getMessage() {
		return message;
	}
	
	/**
	 * @param message
	 */
	public void setMessage(String message) {
		this.message=message;
	}
	
	/**
	 * @return
	 */
	public String send() {
		Properties p = new Properties();
        String mailServer = System.getProperty("smtp.server", "imap.broad.mit.edu");

        p.put("mail.host", mailServer);
 
        Session mailSession = Session.getDefaultInstance(p, null);
        mailSession.setDebug(false);
        MimeMessage msg = new MimeMessage(mailSession);
        
        try {
            msg.setSubject("Contact from GP portal!");
            msg.setText("Reply to "+replyTo+"!\n"+message);
            msg.setFrom(new InternetAddress("user@genepattern.org"));
            msg.setSentDate(new Date());
            String email = "gp-help@broad.mit.edu";
            
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
                    email));
            Transport.send(msg);
        } catch (MessagingException e) {
            log.error(e);
            return "failure";
        }
        sent = true;
        return "success";
		
	}
	
	/**
	 * @return
	 */
	public boolean isSent() {
		return sent;
	}
	
	/**
	 * @return
	 */
	public String getInfoMessages() {
		if (!sent)
			return "An error occurred while sending the email.";
		return "Your request has been sent!";
	}

	
}
