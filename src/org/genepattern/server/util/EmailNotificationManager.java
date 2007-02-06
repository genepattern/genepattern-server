package org.genepattern.server.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.genepattern.server.webapp.DNSClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.webservice.JobInfo;

public class EmailNotificationManager {

    protected static EmailNotificationManager instance = null;

    protected ArrayList<JobWaitThread> threads = new ArrayList<JobWaitThread>();

    protected EmailNotificationManager() {

    }

    public static EmailNotificationManager getInstance() {
        if (instance == null) {
            instance = new EmailNotificationManager();
        }
        return instance;
    }

    public void addWaitingUser(String user, String jobID) {
        JobWaitThread waiter = new JobWaitThread(user, jobID);
        threads.add(waiter);
        waiter.start();
    }

    public void removeWaitingUser(String user, String jobID) {
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

    public void sendJobCompletionEmail(String user, String jobID) {
        String status = "status unknown";
        String task = "";
        try {
            LocalAnalysisClient analysis = new LocalAnalysisClient(user);
            JobInfo jobInfo = analysis.checkStatus(Integer.parseInt(jobID));
            status = jobInfo.getStatus();
            task = jobInfo.getTaskName();
        } catch (Exception e) {
            // swallow it and send the link without status
            status = "Finished";
        }

        String addresses = user;
        String from = "GenePattern@" + System.getProperty("gpServerHostAddress");
        String subject = "Job " + jobID + " - " + task + " - " + status;
        StringBuffer msg = new StringBuffer();
        msg.append("The results for job " + jobID + "," + task + " , are available on the <br>");
        // msg.append("<a href='http://gp21e-789.broad.mit.edu:8080/gp/pages/jobResults.jsf?jobID=jobID'>GenePattern
        // JobResults Page</a>");
        msg.append("<a href='" + System.getProperty("GenePatternURL")
                + "/pages/jobResults.jsf?jobID=jobID'>GenePattern JobResults Page</a>");

        emailToAddresses(addresses, from, subject, msg.toString());

    }

    public HashMap emailToAddresses(String addresses, String from, String subject, String msg) {
        DNSClient dnsClient = new DNSClient();

        StringTokenizer stTo = new StringTokenizer(addresses, ",; ");
        String to = null;
        String host = null;
        HashMap<String, String> failures = new HashMap<String, String>();
        while (stTo.hasMoreTokens()) {

            try {
                to = stTo.nextToken();
                int at = to.indexOf("@");
                if (at == -1) {
                    failures.put(to, "Missing '@' in recipient " + to);
                    continue;
                }
                String domain = to.substring(at + 1);
                String mailServer = System.getProperty("smtp.server");
                TreeMap tmHosts = null;
                if (mailServer == null) {
                    tmHosts = dnsClient.findMXServers(domain);
                } else {
                    tmHosts.put(new Integer(1), mailServer);
                }
                if (tmHosts == null || tmHosts.size() == 0) {
                    failures.put(to, "No MX servers for recipient " + to + ".  Bad domain name?");
                    continue;
                }

                // Get system properties
                Properties props = System.getProperties();

                boolean success = false;
                StringBuffer sb = new StringBuffer();
                for (Iterator eHosts = tmHosts.entrySet().iterator(); eHosts.hasNext();) {
                    // get the next MX server name, in priority order
                    host = (String) ((Map.Entry) eHosts.next()).getValue(); // eg.
                    // "genome.wi.mit.edu";
                    // Setup mail server
                    props.put("mail.smtp.host", host);

                    // Get session
                    Session theSession = Session.getDefaultInstance(props, null);

                    // Define message
                    MimeMessage message = new MimeMessage(theSession);
                    message.setFrom(new InternetAddress(from));
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
                    message.setSubject(subject);
                    message.setSentDate(new Date());
                    message.addHeader("X-Sent-By", "GenePattern ");
                    message.setContent(msg, "text/html");

                    // Send message
                    try {
                        Transport.send(message);
                        System.out.println("Message sent to " + to);
                        success = true;
                        break;
                    } catch (javax.mail.SendFailedException sfe) {
                        // underlying errors are defined in RFC821
                        Exception underlyingException = sfe.getNextException();
                        if (underlyingException == null)
                            underlyingException = sfe;
                        String ueMessage = underlyingException.getMessage();
                        String intro = "javax.mail.SendFailedException: ";
                        int i = ueMessage.indexOf(intro);
                        if (i != -1)
                            ueMessage = ueMessage.substring(i + intro.length());
                        sb.append("" + ueMessage + " while attempting to send to " + to + " via " + host);
                    } catch (Exception e) {
                        sb.append("" + e + " while attempting to send to " + to + " via " + host);
                    }
                }
                if (!success) {
                    failures.put(to, "Unable to send message to " + to + "\n" + sb.toString());
                }
            } catch (Exception e) {
                System.out.println(e + " while attempting to send to " + to + " via " + host + "<br>");
                e.printStackTrace();
            }
        }// end while more addresses
        if (failures.size() > 0)
            return failures;
        else
            return null;

    }// end emailAddresses

}

class JobWaitThread extends Thread {
    String user = null;

    int jobID;

    int initialSleep = 5000;

    int maxTries = 100;

    boolean stopQuietly = false;

    public JobWaitThread(String user, String jobID) {
        this.user = user;
        this.jobID = Integer.parseInt(jobID);
    }

    public void quietStop() {
        stopQuietly = true;
    }

    public void run() {
        try {
            LocalAnalysisClient analysis = new LocalAnalysisClient(user);

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
                info = analysis.checkStatus(jobID);
                status = info.getStatus();
                sleep = incrementSleep(initialSleep, maxTries, count);

            }
            // the job is done. Send an email to the user
            if (!stopQuietly) {
                EmailNotificationManager.getInstance().sendJobCompletionEmail(user, "" + jobID);
            }
        } catch (Exception e) {
            // problem getting status. Send an email indicating this and
            // end the thread
            e.printStackTrace();

            EmailNotificationManager em = EmailNotificationManager.getInstance();
            String addresses = user;
            String from = "GenePattern@" + System.getProperty("gpServerHostAddress");
            String subject = "Job " + jobID + " - status unavailable";
            StringBuffer msg = new StringBuffer();
            msg.append("There was a problem getting the status for job " + jobID);
            msg.append("\nThe job may or may not be finished.  When it is complete you will be able to");
            msg.append("get the results from here:\n ");
            msg
                    .append("<a href='http://gp21e-789.broad.mit.edu:8080/gp/pages/jobResults.jsf?jobID=jobID'>GenePattern Results Page</a>");
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
     *            Description of the Parameter
     * @param maxTries
     *            Description of the Parameter
     * @param count
     *            Description of the Parameter
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
