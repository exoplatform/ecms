package org.exoplatform.wcm.webui.selector.content;

import javax.jcr.Node;
import javax.portlet.PortletPreferences;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.NodeIdentifier;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.NotInWCMPublicationException;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Author : TAN DUNG DANG
 *          dzungdev@gmail.com
 * Feb 12, 2009
 */

@ComponentConfig (
    template="classpath:groovy/wcm/webui/selector/content/UIContentResultViewer.gtmpl",
    events = {
        @EventConfig(listeners = UIContentResultViewer.SelectActionListener.class)
    }
)

public class UIContentResultViewer extends UIContainer {

  private NodeLocation presentNodeLocation;

  public String[] getActions() {
    return new String[] {"Select"};
  }

  public static class SelectActionListener extends EventListener<UIContentResultViewer> {
    public void execute(Event<UIContentResultViewer> event) throws Exception {
      UIContentResultViewer contentResultView = event.getSource();
      Node presentNode = NodeLocation.getNodeByLocation(contentResultView.presentNodeLocation);
      Node webContent = presentNode;
      NodeIdentifier nodeIdentifier = NodeIdentifier.make(webContent);
      PortletRequestContext pContext = (PortletRequestContext) event.getRequestContext();
      PortletPreferences prefs = pContext.getRequest().getPreferences();
      prefs.setValue("repository", nodeIdentifier.getRepository());
      prefs.setValue("workspace", nodeIdentifier.getWorkspace());
      prefs.setValue("nodeIdentifier", nodeIdentifier.getUUID());
      prefs.store();

      String remoteUser = Util.getPortalRequestContext().getRemoteUser();
      String currentSite = Util.getPortalRequestContext().getPortalOwner();

      WCMPublicationService wcmPublicationService = contentResultView.getApplicationComponent(WCMPublicationService.class);

      try {
        wcmPublicationService.isEnrolledInWCMLifecycle(webContent);
      } catch (NotInWCMPublicationException e){
        wcmPublicationService.unsubcribeLifecycle(webContent);
        wcmPublicationService.enrollNodeInLifecycle(webContent, currentSite, remoteUser);
      }
    }
  }
}
