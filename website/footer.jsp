<%@ page contentType="text/html" language="Java" session="false" %>
<% { %>
<!-- begin footer.jsp -->
<br>
<div class="navbar">
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>


<table width="100%">
	<tr>
		<td class="navbar" align="left" valign="top">
			<a href="index.jsp" class="navbar">
			<img src="skin/logoSmall.gif" border="0"></a> <%=messages.getProperty("copyright"," &copy; 2003-2005 <a href='http://www.broad.mit.edu' class='navbar'>Broad Institute, MIT</a>")%>
		</td>
		<td class="navbar" align="right" valign="top">
			<a href="<%=messages.getProperty("bugReportLinkTarget")%>" class="navbar"><nobr>report bugs</nobr></a> |
			<a href="<%=messages.getProperty("helpRequestLinkTarget")%>" class="navbar"><nobr>request help</nobr></a>
		</td>
	</tr>
</table>
</div>
<!-- end footer.jsp -->
<% } %>
