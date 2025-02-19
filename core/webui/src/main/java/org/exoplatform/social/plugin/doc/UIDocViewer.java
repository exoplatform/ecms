/*
* Copyright (C) 2003-2010 eXo Platform SAS.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
*/

package org.exoplatform.social.plugin.doc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

/**
 * UIDocViewer
 * <p>
 * </p>
 *
 * @author Zuanoc
 * @since Aug 10, 2010
 */

@ComponentConfig(lifecycle = Lifecycle.class, events = {
  @EventConfig(listeners = UIDocViewer.DownloadActionListener.class),
})
public class UIDocViewer extends UIBaseNodePresentation {

  private static final String UIDocViewerPopup = "UIDocViewerPopup";

  /**
   * The logger.
   */
  private static final Log    LOG              = ExoLogger.getLogger(UIDocViewer.class);

  protected Node              originalNode;

  public String               docPath;

  public String               repository;

  public String               workspace;

  /**
   * Sets the original node.
   *
   * @param originalNode
   */
  public void setOriginalNode(Node originalNode) {
    this.originalNode = originalNode;
  }

  /**
   * Gets the original node.
   *
   * @return
   * @throws Exception
   */
  public Node getOriginalNode() throws Exception {
    return getNode();
  }

  @Override
  public Node getNode() throws Exception {
    NodeLocation nodeLocation = new NodeLocation(repository, workspace, docPath);
    return NodeLocation.getNodeByLocation(nodeLocation);
  }

  /**
   * Sets the node.
   *
   * @param node
   */
  public void setNode(Node node) {
    originalNode = node;
  }

  public String getTemplate() {
    return null;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation#
   * getTemplatePath()
   */
  public String getTemplatePath() throws Exception {
    return getRepository();
  }

  public String getNodeType() {
    return null;
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(Utils.FILE_VIEWER_EXTENSION_TYPE);
    Map<String, Object> context = new HashMap<String, Object>();
    context.put(Utils.MIME_TYPE, mimeType);
    for (UIExtension extension : extensions) {
      UIComponent uiComponent = manager.addUIExtension(extension, context, this);
      if (uiComponent != null) {
        return uiComponent;
      }
    }
    return null;
  }

  public String getRepositoryName() {
    return UIDocActivity.REPOSITORY_NAME;
  }

  public static class DownloadActionListener extends EventListener<UIDocViewer> {
    public void execute(Event<UIDocViewer> event) throws Exception {
      UIDocViewer uiComp = event.getSource();
      String downloadLink = uiComp.getDownloadLink(org.exoplatform.wcm.webui.Utils.getFileLangNode(uiComp.getNode()));
      JavascriptManager jsManager = event.getRequestContext().getJavascriptManager();
      downloadLink = downloadLink.replaceAll("&amp;", "&");
      jsManager.addJavascript("window.location.href = " + downloadLink + ";");
    }
  }

}
