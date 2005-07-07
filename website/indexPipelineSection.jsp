<script language="Javascript">

function exportPipeline(button) {
	var width = 250;
	var height = 200;
	var exportWindow = window.open('askInclude.jsp?name=' + 
		    button.form.pipeline.options[button.form.pipeline.selectedIndex].value + 
		    '<%= LSID.DELIMITER %>' + 
		    button.form.pipelineVersion.options[button.form.pipelineVersion.selectedIndex].value +
		    '&title=' + button.form.pipeline.options[button.form.pipeline.selectedIndex].text, 
		    'export',
		    'alwaysRaised=yes,height=' + height + ',width=' + width + ',menubar=no,location=no,resizable=yes,scrollbars=yes,directories=no,status=no,toolbar=no');
	exportWindow.focus();
}


</script>

    <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading" colspan="2">Pipelines</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>

			<td valign="top" align="right">
			<%= taskCatalog(tmTasks, latestTaskMap, "pipeline", "pipeline catalog", GPConstants.TASK_TYPE_PIPELINE, userID, true) %>
			<nobr>version <select name="pipelineVersion"></select></nobr>
			</td>

		</tr>
		<tr>
			<td valign="top" align="center">
				<input type="button" value="run" name="runpipeline" class="button" onclick="jmp(this, 'runTask.jsp?cmd=run&name=', document.forms['index'].pipeline, document.forms['index'].pipelineVersion)">
				<input type="button" value="view" name="viewpipeline" class="button" onclick="jmp(this, 'viewPipeline.jsp?name=', document.forms['index'].pipeline, document.forms['index'].pipelineVersion)">
				<input type="button" value="edit" name="editpipeline" class="button" onclick="jmp(this, 'pipelineDesigner.jsp?name=', document.forms['index'].pipeline, document.forms['index'].pipelineVersion)">
				<input type="button" value="export" name="exportpipeline" class="button" onclick="exportPipeline(this)">
			</td>
		</tr>

		<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>

		<tr>
			<td colspan="2" align="center" valign="top">
				<input type="button" value="create" class="button" onclick="javascript: window.location='pipelineDesigner.jsp'">
				<input type="button" value="import..." class="button" onclick="javascript:window.location='addZip.jsp'">
			</td>
		</tr>
	</table> <!-- end pipeline cell -->
    </td>

  