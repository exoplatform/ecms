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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.jcr.Node;

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.services.attachments.utils.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

import lombok.Getter;
import lombok.Setter;

@ComponentConfig
@Getter
@Setter
public class UIDocViewer extends UIContainer {

  private static final Log    LOG                        = ExoLogger.getLogger(UIDocViewer.class);

  private static final String FILE_VIEWER_EXTENSION_TYPE = "org.exoplatform.ecm.dms.FileViewer";

  private static final String MIME_TYPE                  = "mimeType";

  protected Node              originalNode;

  protected String            docPath;

  protected String            repository;

  protected String            workspace;

  public Node getOriginalNode() {
    return getNode();
  }

  public void setNode(Node node) {
    originalNode = node;
  }

  public Node getNode() {
    NodeLocation nodeLocation = new NodeLocation(repository, workspace, docPath);
    return NodeLocation.getNodeByLocation(nodeLocation);
  }

  public String getNodeType() {
    return null;
  }

  public boolean isNodeTypeSupported() {
    return false;
  }

  @Override
  public void processRender(WebuiRequestContext requestContext) throws Exception {
    String mimeType = Utils.getMimeType(getNode());
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(FILE_VIEWER_EXTENSION_TYPE);
    Map<String, Object> context = Collections.singletonMap(MIME_TYPE, mimeType);
    UIComponent uiComponent = extensions.stream()
                                        .map(extension -> {
                                          try {
                                            return manager.addUIExtension(extension, context, this);
                                          } catch (Exception e) {
                                            LOG.warn("Error while computing DocView using extension {} for node {}. Ignore extension and seek for compatible one",
                                                     extension.getClass().getName(),
                                                     getDocPath(),
                                                     e);
                                            return null;
                                          }
                                        })
                                        .filter(Objects::nonNull)
                                        .findFirst()
                                        .orElse(null);
    if (uiComponent != null) {
      uiComponent.processRender(requestContext);
    }
  }

}
