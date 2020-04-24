<%
	String appId = ((String[])request.getAttribute("appId"))[0];
	String query = ((String[])request.getAttribute("query"))[0];
	String folder = ((String[])request.getAttribute("folder"))[0];
	String type = ((String[])request.getAttribute("type"))[0];
	String limit = System.getProperty("exo.dw.page.snapshot.itemsLimit", "10");
%>
<div class="VuetifyApp">
	<div id="<%=appId%>" app-id="<%=appId%>">
		<script>
			require(['SHARED/documentsBundle'],
				app => app.init("<%=appId%>", "<%=query%>", "<%=folder%>", "<%=type%>", "<%=limit%>")
			);
		</script>
	</div>
</div>