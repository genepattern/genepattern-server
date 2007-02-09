/**
 *
 */
package org.genepattern.server.webapp.jsf;

import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;



public class ContactUsBean {
    	private String subject;
	private String replyTo;
	private String message;
	private boolean sent=false;

	private static Logger log = Logger.getLogger(ContactUsBean.class);

	public static final String EMAIL_REGEXP_PATTERN = "^[\\w-\\.]+@([\\w-]+\\.)([\\w-]+\\.[\\w-]+)*[\\w-]{2,4}$";

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
	public String getSubject() {
		return subject;
	}


	/**
	 * @param subject
	 */
	public void setSubject(String subject) {
		this.subject=subject;
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
                msg.setSubject(subject);
                msg.setText("Reply to "+replyTo+"!\n"+message);
                msg.setFrom(new InternetAddress(replyTo));
                msg.setSentDate(new Date());

                String email = "gp-help@broad.mit.edu";

                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
            	    email));
                Transport.send(msg);
            } catch (MessagingException e) {
                log.error(e);
                UIBeanHelper
                .setErrorMessage("An error occurred while sending the email.");
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

	public void validateEmail(FacesContext context, UIComponent component, Object value)
        throws ValidatorException {
	    String message = "The return address entered is not valid.";
	    FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);

	    if (value == null) {
		((UIInput) component).setValid(false);
		throw new ValidatorException(facesMessage);
	    }
	    String address = (String)value;
	    try {
	      InternetAddress emailAddr = new InternetAddress(address);
	      if ( ! address.matches(EMAIL_REGEXP_PATTERN) ) {
		  ((UIInput) component).setValid(false);
		  throw new ValidatorException(facesMessage);
	      }
	    }catch (AddressException ex){
		((UIInput) component).setValid(false);
		throw new ValidatorException(facesMessage);
	    }
        }
}
