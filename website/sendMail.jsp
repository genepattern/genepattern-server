<!-- /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ -->


<%@ page import="javax.mail.*,
		 javax.mail.internet.*,
		 java.io.*,
		 java.net.*,
		 java.util.*,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.server.webapp.DNSClient" 
    session="false" contentType="text/html" language="Java" %>
<%
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>send email messages</title>
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<br>
<%
	String from = request.getParameter("from");
	String addressees = request.getParameter("to");
	if (addressees == null || addressees.length() == 0) {
		System.err.println("no recipients");
%>
		<script language="Javascript">
			self.window.close();
		</script>
<%
		return;
	}
	StringTokenizer stTo = new StringTokenizer(addressees, ",; ");
	String subject = request.getParameter("subject");
	String msg = request.getParameter("message");
	String mimeType = request.getParameter("mimeType");
	if (mimeType == null) {
		mimeType = "text/html";
	}
	DNSClient dnsClient = new DNSClient();
	String to = null;
	String host = null;
	boolean allOkay = true;
	StringBuffer failedRecipients = new StringBuffer();

	while (stTo.hasMoreTokens()) {
	    try {
		to = stTo.nextToken();
		int at = to.indexOf("@");
		if (at == -1) {
			out.println("<font color=\"red\">Missing '@' in recipient " + to + "</font><br>");
			if (failedRecipients.length() > 0) {
				failedRecipients.append("; ");
			}
			failedRecipients.append(to);
			allOkay = false;
			continue;
		}
		String domain = to.substring(at+1);
		String mailServer = System.getProperty("smtp.server");
		TreeMap tmHosts = null;
		if (mailServer == null) {
			tmHosts = dnsClient.findMXServers(domain);
		} else {
			tmHosts.put(new Integer(1), mailServer);
		}
		if (tmHosts == null || tmHosts.size() == 0) {
			out.println("<font color=\"red\">No MX servers for recipient " + to + ".  Bad domain name?</font><br>");
			allOkay = false;
			if (failedRecipients.length() > 0) {
				failedRecipients.append("; ");
			}
			failedRecipients.append(to);
			allOkay = false;
			continue;
		}
/*
		for (Iterator eHosts = tmHosts.keySet().iterator(); eHosts.hasNext(); ) {
			Object key = eHosts.next();
			host = (String)tmHosts.get(key);
			System.out.println("MX servers for " + domain + ": " + host + " at priority " + key);
		}
*/
		// Get system properties
		Properties props = System.getProperties();

		boolean success = false;
		StringBuffer sb = new StringBuffer();
		for (Iterator eHosts = tmHosts.entrySet().iterator(); eHosts.hasNext(); ) {
			// get the next MX server name, in priority order
			host = (String)((Map.Entry)eHosts.next()).getValue(); // eg. "genome.wi.mit.edu";
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
			message.addHeader("X-Sent-By", "GenePattern on " + request.getServerName() + ":" + request.getServerPort());
			message.setContent(msg, mimeType);

			// Send message
			try {
				Transport.send(message);
				out.println("Message sent to " + to + "<br>");
				success = true;
				break;
			} catch (javax.mail.SendFailedException sfe) {
				// underlying errors are defined in RFC821
				Exception underlyingException = sfe.getNextException();
				if (underlyingException == null) underlyingException = sfe;
				String ueMessage = underlyingException.getMessage();
				String intro = "javax.mail.SendFailedException: ";
				int i = ueMessage.indexOf(intro);
				if (i != -1) ueMessage = ueMessage.substring(i + intro.length());
			    	sb.append("<font color=\"red\">" + ueMessage + " while attempting to send to " + to + " via " + host + "</font><br>\n");
				allOkay = false;
			} catch (Exception e) {
			    	sb.append("<font color=\"red\">" + e + " while attempting to send to " + to + " via " + host + "</font><br>\n");
				allOkay = false;
			}
		}
		if (!success) {
			out.println("<font color=\"red\">Unable to send message to " + to + "</font><br>\n" + sb.toString() + "<br>");
			if (failedRecipients.length() > 0) {
				failedRecipients.append("; ");
			}
			failedRecipients.append(to);
			allOkay = false;
		}
	    } catch (Exception e) {
	    	out.println(e + " while attempting to send to " + to + " via " + host + "<br>");
		e.printStackTrace();
	    }
	} // end while more recipients on to: list
	if (allOkay) {
%>
		<br>
		<a href="Javascript:self.window.close()">close window</a>
		<br>
		<script language="Javascript">
			self.window.close();
		</script>
<%
	} else {
%>
		<br>
		<form method="post" action="sendMail.jsp">
		<table cols="2">
		<tr>
		<td valign="top" align="right">From:</td>
		<td valign="top"><input type="text" name="from" value="<%= StringUtils.htmlEncode(URLDecoder.decode(from)) %>" size="70"></td>
		</tr>
		<input type="hidden" name="subject" value="<%= StringUtils.htmlEncode(subject) %>">
		<input type="hidden" name="message" value="<%= StringUtils.htmlEncode(msg) %>">
		<tr>
		<td valign="top" align="right">Resend&nbsp;to:</td>
		<td valign="top"><input type="text" name="to" value="<%= StringUtils.htmlEncode(failedRecipients.toString()) %>" size="70"></td>
		</tr>
		<tr>
		<td></td>
		<td>
		<input type="submit" name="submit" value="resend">
		</td></tr>
		</table>
		</form>
		<a href="Javascript:self.window.close()">close window</a><br>
<%
	}
%>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
