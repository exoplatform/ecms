<%@ page import="org.exoplatform.container.ExoContainerContext"%>
<%@ page import="org.exoplatform.portal.config.UserPortalConfigService"%>
<%@ page import="org.exoplatform.portal.resource.SkinService"%>
<%@ page import="org.exoplatform.portal.resource.SkinConfig"%>
<%
  SkinService skinService = ExoContainerContext.getService(SkinService.class);
  UserPortalConfigService userPortalConfigService = ExoContainerContext.getService(UserPortalConfigService.class);
  String skinName = userPortalConfigService.getDefaultPortalSkinName();
  SkinConfig skin = skinService.getSkin("calendar/CalendarPortlet", skinName);
  if (skin != null) {
%>
<link id="dw-events" rel="stylesheet" type="text/css" href="<%=skin.getCSSPath()%>" />
<%
  }
%>

<div class="VuetifyApp">
	<div id="digital-workplace-events">
		<script>
			require(['SHARED/digitalWorkplaceEventsBundle'], function(eventsApp) {
				eventsApp.init();
			});
		</script>
	</div>
</div>