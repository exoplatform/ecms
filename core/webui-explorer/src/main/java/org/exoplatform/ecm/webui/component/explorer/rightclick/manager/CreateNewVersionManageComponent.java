package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsContainBinaryFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotEditingDocumentFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsVersionableFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.reader.ContentReader;
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

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/6/15
 * Create a new version of document
 */
@ComponentConfig(
        events = {
                @EventConfig(listeners = CreateNewVersionManageComponent.CreateNewVersionActionListener.class)
        }
)
public class CreateNewVersionManageComponent extends UIAbstractManagerComponent {

  private static Log log = ExoLogger.getExoLogger(CreateNewVersionManageComponent.class);

  private static final List<UIExtensionFilter> FILTERS = Arrays.asList(new UIExtensionFilter[]{
          new IsVersionableFilter(),
          new IsNotInTrashFilter(),
          new IsNotEditingDocumentFilter(),
          new IsContainBinaryFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }

  public static class CreateNewVersionActionListener extends UIWorkingAreaActionListener<CreateNewVersionManageComponent> {
    @Override
    public void processEvent(Event<CreateNewVersionManageComponent> event) throws Exception {
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      if (nodePath == null) {
        nodePath = uiExplorer.getCurrentWorkspace() + ':' + uiExplorer.getCurrentPath();
      }
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
      String wsName = null;
      if (matcher.find()) {
        wsName = matcher.group(1);
        nodePath = matcher.group(2);
      } else {
        throw new IllegalArgumentException("The ObjectId is invalid '"+ nodePath + "'");
      }
      Session session = uiExplorer.getSessionByWorkspace(wsName);
      // Use the method getNodeByPath because it is link aware
      Node node = uiExplorer.getNodeByPath(nodePath, session);
      AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
      autoVersionService.autoVersion(node);
      String msg = event.getRequestContext().getApplicationResourceBundle().getString("DocumentAuto.message");
      msg = msg.replace("{0}", ContentReader.simpleEscapeHtml("<span style='font-weight:bold;'>" + node.getName() + "</span>"));
      event.getRequestContext().getJavascriptManager().require("SHARED/wcm-utils", "wcm_utils")
              .addScripts("eXo.ecm.WCMUtils.showNotice(\""+msg+"\", 'true'); ");
    }
  }
}
