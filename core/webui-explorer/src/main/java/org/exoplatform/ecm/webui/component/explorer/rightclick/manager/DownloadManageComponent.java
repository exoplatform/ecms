package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNtFileFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

import javax.jcr.Node;
import javax.jcr.Session;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

@ComponentConfig(
        events = {
                @EventConfig(listeners = DownloadManageComponent.DownloadActionListener.class)
        }
)
public class DownloadManageComponent extends UIAbstractManagerComponent {

  private static final Log LOG = ExoLogger.getLogger(DownloadManageComponent.class.getName());

  private static final List<UIExtensionFilter> FILTERS
          = Arrays.asList(new UIExtensionFilter[]{new IsNtFileFilter(),
  });

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

  /**
   * Parse node path with syntax [workspace:node path] to workspace name and path separately
   *
   * @param nodePath node path with syntax [workspace:node path]
   * @return array of String. element with index 0 is workspace name, remaining one is node path
   */
  private String[] parseWorkSpaceNameAndNodePath(String nodePath) {
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
    if (!matcher.find())
      return null;
    String[] workSpaceNameAndNodePath = new String[2];
    workSpaceNameAndNodePath[0] = matcher.group(1);
    workSpaceNameAndNodePath[1] = matcher.group(2);
    return workSpaceNameAndNodePath;
  }

  /**
   * Gets user session from a specific workspace.
   *
   * @param workspaceName
   * @return session
   * @throws Exception
   */
  private Session getSession(String workspaceName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    return sessionProvider.getSession(workspaceName, WCMCoreUtils.getRepository());
  }

  public static class DownloadActionListener extends UIWorkingAreaActionListener<DownloadManageComponent> {
    public void processEvent(Event<DownloadManageComponent> event) throws Exception {
      DownloadManageComponent downloadManageComponent = event.getSource();
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      String[] workspaceAndPath = downloadManageComponent.parseWorkSpaceNameAndNodePath(nodePath);
      Node downloadedNode = (Node) WCMCoreUtils.getService(NodeFinder.class)
              .getItem(downloadManageComponent.getSession(workspaceAndPath[0]), workspaceAndPath[1], true);

      String downloadLink = Utils.getDownloadLink(downloadedNode);
      event.getRequestContext().getJavascriptManager().require("SHARED/jquery", "gj")
              .addScripts("setTimeout(\"window.location.assign('" + downloadLink + "');\", 1000);");
    }
  }
}
