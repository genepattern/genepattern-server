
     <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading" colspan="2">Tasks</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>

			<td valign="top" align="right">
			<%= taskCatalog(tmTasks, latestTaskMap, "task", "task catalog", null, userID, false) %>
			<nobr>version <select name="taskVersion"></select></nobr>
			</td>

		</tr>
		<tr>
			<td valign="top" align="center">
				<input type="button" value="run" name="runtask" class="button" onclick="jmp(this, 'runTask.jsp?name=', document.forms['index'].task, document.forms['index'].taskVersion)">
				<input type="button" value="view" name="viewtask" class="button" onclick="jmp(this, 'addTask.jsp?view=1&name=', document.forms['index'].task, document.forms['index'].taskVersion)">
				<input type="button" value="edit" name="edittask" class="button" onclick="jmp(this, 'addTask.jsp?name=', document.forms['index'].task, document.forms['index'].taskVersion)">
				<input type="button" value="export" name="exporttask" class="button" onclick="jmp(this, 'makeZip.jsp?name=', document.forms['index'].task, document.forms['index'].taskVersion)">
			</td>
		</tr>

		<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>

		<tr>
			<td colspan="2" align="center">
				<input type="button" value="create" class="button" onclick="javascript:window.location='addTask.jsp'">
				<input type="button" value="import..." class="button" onclick="javascript:window.location='addZip.jsp'">
			</td>
		</tr>
	</table> <!-- end task cell -->
    </td>
