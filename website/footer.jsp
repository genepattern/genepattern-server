<%@ page contentType="text/html" language="Java" session="false" %>
<% { %>
<!-- begin footer.jsp -->
<br>
<div class="navbar">
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>


<table width="100%">
	<tr >
		<td  align="left" valign="top">
			<a href="index.jsp" class="navbarlink">
			</a> <%=messages.getProperty("copyright"," &copy; 2003-2005 <a href='http://www.broad.mit.edu' class='navbarlink'>Broad Institute, MIT</a>") %>		

</td>
		<td  align="right" valign="top">
			<a href="<%=messages.getProperty("bugReportLinkTarget")%>" class="navbarlink"><nobr>report bugs</nobr></a> |
			<a href="<%=messages.getProperty("helpRequestLinkTarget")%>" class="navbarlink"><nobr>request help</nobr></a>
		</td>
	</tr>
</table>
</div>
<!-- end footer.jsp -->
<% } %>
