/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

import com.google.common.base.Strings;

public class EmailNotificationManager {
    private static final Logger log = Logger.getLogger(EmailNotificationManager.class);

    private static EmailNotificationManager instance = null;

    private ArrayList<JobWaitThread> threads = new ArrayList<JobWaitThread>();

    private EmailNotificationManager() {

    }

    public static EmailNotificationManager getInstance() {
	if (instance == null) {
	    instance = new EmailNotificationManager();
	}
	return instance;
    }

    public void addWaitingUser(String email, String user, String jobID) {
	JobWaitThread waiter = new JobWaitThread(email, user, jobID);
	threads.add(waiter);
	waiter.start();
    }

    public void removeWaitingUser(String email, String user, String jobID) {
	for (JobWaitThread aJobThread : threads) {
	    if (aJobThread.matchJobAndUser(user, jobID)) {
		aJobThread.quietStop();
		threads.remove(aJobThread);
		return;
	    }
	}
    }

    protected void threadFinished(JobWaitThread aThread) {
	threads.remove(aThread);
    }

    public void sendJobCompletionEmail(String email, String user, String jobId) {
        String status = "status unknown";
        String moduleName = "";
        try {
            HibernateUtil.beginTransaction();
            JobInfo jobInfo = (new AnalysisDAO()).getJobInfo(Integer.parseInt(jobId));
            HibernateUtil.commitTransaction();
            status = jobInfo.getStatus();
            moduleName = jobInfo.getTaskName();
        } catch (Exception e) {
            // swallow it and send the link without status
            status = "Finished";
        }

        String addresses = email;
        final String from=MailSender.getFromAddress();
        String subject = "Job " + jobId + " - " + moduleName + " - " + status;
        StringBuffer msg = new StringBuffer();
        msg.append("The results for job " + jobId + ", " + moduleName + ", are available on the ");
        msg.append("<a href=\"" + UIBeanHelper.getServer() + "/jobResults/" + jobId + "\">GenePattern Job Results Page</a>");
        emailToAddresses(addresses, from, subject, msg.toString());

    }

    protected List<String> splitAddresses(final String addresses) {
        final List<String> rval=new ArrayList<String>();
        final StringTokenizer stTo = new StringTokenizer(addresses, ",; ");
        while (stTo.hasMoreTokens()) {
            String to = stTo.nextToken();
            rval.add(to);
        }
        return rval;
    }

    public void emailToAddresses(final String addresses, String from, String subject, String msg) {
        final List<String> addressList=splitAddresses(addresses);
        emailToAddresses(addressList, from, subject, msg);
    }

    public void emailToAddresses(final List<String> addresses, String from, String subject, String msg) {
        for(final String to : addresses) {
            try {
                sendMessage(to, from, subject, msg);
            }
            catch (MessagingException e) {
                log.trace("error ignored, it's logged in sendMessage", e);
            }
        }
    }

    public void sendMessage(final String to, String from, String subject, String msg) 
    throws MessagingException
    {
        if (Strings.isNullOrEmpty(to)) {
            log.error("'recipient' not set");
            return;
        }
        else if (to.indexOf("@") == -1) {
            log.error("Missing '@' in recipient " + to);
            return;
        }
        
        final MailSender m = new MailSender.Builder()
            .build();
        // Get session
        final Session theSession = m.initMailSession();
        
        //final HashMap<String, String> failures = new HashMap<String, String>();
        //final String smtpServer = ServerConfigurationFactory.instance().getGPProperty(
        //    GpContext.getServerContext(), MailSender.PROP_SMTP_SERVER, MailSender.DEFAULT_SMTP_SERVER
        //);
        //final Properties p=new Properties();
        //p.put("mail.host", smtpServer);
        // Get session
        //final Session theSession = Session.getInstance(p);

        // Define message
        final MimeMessage message = new MimeMessage(theSession);
        message.setSubject(subject);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSentDate(new Date());
        message.addHeader("X-Sent-By", "GenePattern ");
        message.setContent(msg, "text/html");
        
        m.sendMessage(message);

//        boolean success = false;
        //StringBuffer sb = new StringBuffer();

        // Send message
        //try {
        //    Transport.send(message);
            //success = true;
            //break;
        //} 
//        catch (SendFailedException sfe) {
//            // underlying errors are defined in RFC821
//            Exception underlyingException = sfe.getNextException();
//            if (underlyingException == null) {
//                underlyingException = sfe;
//            }
//            String ueMessage = underlyingException.getMessage();
//            String intro = "javax.mail.SendFailedException: ";
//            int i = ueMessage.indexOf(intro);
//            if (i != -1) {
//                ueMessage = ueMessage.substring(i + intro.length());
//            }
//            sb.append("" + ueMessage + " while attempting to send to " + to + " via " + smtpServer);
//        } 
//        catch (Exception e) {
//            sb.append("" + e + " while attempting to send to " + to + " via " + smtpServer);
//        }
//        if (!success) {
//            failures.put(to, "Unable to send message to " + to + "\n" + sb.toString());
//        }
    }

}

class JobWaitThread extends Thread {
    String user = null;
    String email = null;

    int jobID;

    int initialSleep = 5000;

    int maxTries = 100;

    boolean stopQuietly = false;

    public JobWaitThread(String email, String user, String jobID) {
	this.user = user;
	this.email = email;
	this.jobID = Integer.parseInt(jobID);
    }

    public void quietStop() {
	stopQuietly = true;
    }

    public void run() {
	try {
	    String status = "";
	    JobInfo info = null;
	    int count = 0;
	    int sleep = initialSleep; // wouldn't be here if it was fast
	    while (!(status.equalsIgnoreCase("ERROR") || (status.equalsIgnoreCase("Finished") || stopQuietly))) {
		count++;
		try {
		    Thread.sleep(sleep);
		} catch (InterruptedException ie) {
		}

		HibernateUtil.beginTransaction();
		info = (new AnalysisDAO()).getJobInfo(jobID);
		HibernateUtil.commitTransaction();

		status = info.getStatus();
		sleep = incrementSleep(initialSleep, maxTries, count);

	    }
	    // the job is done. Send an email to the user
	    if (!stopQuietly) {
		EmailNotificationManager.getInstance().sendJobCompletionEmail(email, user, "" + jobID);
	    }
	} catch (Exception e) {
	    // problem getting status. Send an email indicating this and
	    // end the thread
	    e.printStackTrace();

	    EmailNotificationManager em = EmailNotificationManager.getInstance();
	    String addresses = email;
	    final String from=MailSender.getFromAddress();
	    String subject = "Job " + jobID + " - status unavailable";
	    StringBuffer msg = new StringBuffer();
	    msg.append("There was a problem getting the status for job " + jobID);
	    msg.append("\nThe job may or may not be finished.  When it is complete you will be able to");
	    msg.append("get the results from here:\n ");
	    msg.append("<a href=\"" + UIBeanHelper.getServer() + "/jobResults/"+jobID+"\">GenePattern Results Page</a>");
	    msg.append("\n\nSee the GenePattern logs for the error details.");

	    em.emailToAddresses(addresses, from, subject, msg.toString());
	} finally {
	    EmailNotificationManager.getInstance().threadFinished(this);
	}
    }

    public int getJobID() {
	return jobID;
    }

    public String getUser() {
	return user;
    }

    /**
     * make the sleep time go up as it takes longer to exec. eg for 100 tries of 1000ms (1 sec) first 20 are 1 sec each
     * next 20 are 2 sec each next 20 are 4 sec each next 20 are 8 sec each any beyond this are 16 sec each
     * 
     * @param init
     *                Description of the Parameter
     * @param maxTries
     *                Description of the Parameter
     * @param count
     *                Description of the Parameter
     * @return Description of the Return Value
     */
    private static int incrementSleep(int init, int maxTries, int count) {
	if (count < (maxTries * 0.2)) {
	    return init;
	}
	if (count < (maxTries * 0.4)) {
	    return init * 2;
	}
	if (count < (maxTries * 0.6)) {
	    return init * 4;
	}
	if (count < (maxTries * 0.8)) {
	    return init * 8;
	}
	return init * 16;
    }

    public boolean matchJobAndUser(String user, String job) {
	if (!user.equals(this.user))
	    return false;
	try {
	    int jobAsInt = Integer.parseInt(job);
	    return (jobAsInt == jobID);
	} catch (Exception e) {
	    return false;
	}
    }
}
