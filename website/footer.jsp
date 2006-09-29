<%String cp = request.getContextPath(); %>
</td>
	</tr>

	<tr>
		<td colspan="3" class="footer">
		<table width="100%" border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td><a href="<%=cp%>/pages/jobResults.jsf"> Job Results </a> | <a href="link">
				Create Pipeline </a> | <a href="link"> Documentation </a> | <a
					href="link"> Resources </a> | <a href="link"> Downloads </a> <br />
				<a href="link"> About GenePattern </a> | <a href="link"> Report
				Bugs </a> | <a href="link"> Request Help </a> | <a href="link">
				Contact Us </a> <br />
				</td>
				<td>
				<div align="right">&copy; 2003-2006 <a
					href="http://www.broad.mit.edu/"> Broad Institute, MIT </a></div>
				<!-- we will be adding the Broad logo here --></td>
				<td width="10">&#160;</td>
				<td width="27"><a href="http://www.broad.mit.edu/"> <img
					src="<%=cp%>/images/broad-symbol.gif"
					width="27" height="30" border="0" /> </a></td>
			</tr>
		</table>
		</td>
	</tr>
</table>
</div>
<!-- top of page login and search items -->
<%
String userId = request.getParameter("userID");
if(userId!=null) {
%>

	<table height="30" border="0" cellpadding="0" cellspacing="0"
		class="loginsettings">
		<tr valign="top">
			<td><a
				href="/genepattern/pages/accountInfo.jsf">
			My Settings </a> | <h:form style="display: inline;">
				<a href="logout.jsp"> Sign Out </a>
			</h:form> <%=userId %>&#160;&#160;&#160;</td>
			<td>
			<form name="form1" method="post" action=""><input
				name="textfield" type="text" size="15" /></form>
			</td>
			<td width="10"><img
				src="/genepattern/images/spacer.gif"
				width="10" height="1" /></td>
			<td><a href="search"> <img
				src="/genepattern/images/searchicon-1.gif"
				name="Image1" height="14" hspace="0" vspace="4" border="0"
				id="Image1"
				onmouseover="MM_swapImage('Image1','','/genepattern/images/searchicon-1-over.gif',1)"
				onmouseout="MM_swapImgRestore()" /> </a></td>
		</tr>
	</table>
<%} %>
