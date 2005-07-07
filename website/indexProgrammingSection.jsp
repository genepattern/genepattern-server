
<script language="Javascript">

function getCode(url, pipeline, versionSelector, language, userID) {
	if (pipeline.selectedIndex != 0 && language.selectedIndex != 0) {
		var lsidVersion = versionSelector.options[versionSelector.selectedIndex].value;
		window.location= url + pipeline.options[pipeline.selectedIndex].value +
				 '<%= LSID.DELIMITER %>' + lsidVersion +
				 '&language=' + language.options[language.selectedIndex].value;
	} else {
		var missing_pipeline = (pipeline.selectedIndex == 0);
		var missing_language = (language.selectedIndex == 0);
		window.alert('Please select ' + (missing_pipeline ? "a pipeline" : "") +
			     (missing_pipeline && missing_language ? " and " : "") +
			     (missing_language ? 'a language' : "") + ".");
	}
}

</script>

 <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading" colspan="2">Programming</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>
			<td valign="top" align="left" colspan="2">
				Download GenePattern library:
			</td>
		</tr>

		<tr>
			<td valign="top" align="left">
				Java
			</td>
			<td valign="top" align="left">
				<a href="downloads/GenePattern.zip">.zip</a>
				<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/index.html?gp_tutorial_prog_java.html" target="_blank">doc</a>
			</td>

		</tr>

		<tr>
			<td valign="top" align="left">
				R
			</td>

			<td valign="top" align="left">
				<a href="downloads/GenePattern_0.1-0.zip">.zip</a> &nbsp;
				<a href="downloads/GenePattern_0.1-0.tar.gz">.tar.gz</a> &nbsp;
				<a href="GenePattern.R">source</a>
				<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/index.html?gp_tutorial_prog_R.html" target="_blank">doc</a>
			</td>
		</tr>
		<tr>
			<td valign="top" align="left">
				MATLAB
			</td>
			<td valign="top" align="left">
				<a href="downloads/GenePatternMatlab.zip">.zip</a>
				<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/index.html?gp_tutorial_prog_MATLAB.html" target="_blank">doc</a>
			</td>

		</tr>

<% if (userIDKnown) { %>
		<tr>
			<td valign="top" align="left" colspan="2">
					<hr noshade size="1">
					Download pipeline code:<br>
					<%= taskCatalog(tmTasks, latestTaskMap, "code", "pipeline", GPConstants.TASK_TYPE_PIPELINE, userID, true) %>
					<br>
					version: <select name="codeVersion">
					</select>
					<select name="pipelineLanguage">
					<option value="">language</option>
<%
					// discover (at runtime) all of the code generators that are available
					Collection cLanguages = AbstractPipelineCodeGenerator.getLanguages();
					for (Iterator itLanguages = cLanguages.iterator(); itLanguages.hasNext();  ) {
						String name = (String)itLanguages.next();
%>
						<option value="<%= name %>"><%= name %></option>
<%
					}
%>
					</select>
				<input type="button" value="code" class="button" onclick="getCode('getPipelineCode.jsp?download=1&name=', document.forms['index'].code, document.forms['index'].codeVersion, document.forms['index'].pipelineLanguage, '<%= userID %>')"><br>
			</td>
		</tr>
<% } %>

	</table> <!-- end programming cell -->
    </td>
