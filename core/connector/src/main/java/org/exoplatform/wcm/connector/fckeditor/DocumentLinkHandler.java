/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.wcm.connector.fckeditor;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.connector.fckeditor.FCKFileHandler;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMConfigurationService;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Sep 26, 2008
 */
/**
 * The Class DocumentLinkHandler.
 */
public class DocumentLinkHandler extends FCKFileHandler {

  /** The base uri. */
  private String baseURI;

  /** The current portal. */
  private String currentPortal;

  /**
   * Instantiates a new document link handler.
   *
   * @param container the container
   */
  public DocumentLinkHandler() {
    super(ExoContainerContext.getCurrentContainer());
  }

  /**
   * Sets the base uri.
   *
   * @param baseURI the new base uri
   */
  public void setBaseURI(String baseURI) {
    this.baseURI = baseURI;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.connector.fckeditor.FCKFileHandler#getFileURL(javax.jcr.Node)
   */
  public String getFileURL(final Node node) throws Exception {
    String accessMode = "private";
    AccessControlList acl = ((ExtendedNode) node).getACL();
    for (AccessControlEntry entry : acl.getPermissionEntries()) {
      if (entry.getIdentity().equalsIgnoreCase(IdentityConstants.ANY)
          && entry.getPermission().equalsIgnoreCase(PermissionType.READ)) {
        accessMode = "public";
        break;
      }
    }
    String repository = ((ManageableRepository) node.getSession().getRepository()).getConfiguration()
                                                                                  .getName();
    String workspace = node.getSession().getWorkspace().getName();
    String nodePath = node.getPath();
    StringBuilder builder = new StringBuilder();
    if (node.isNodeType(NodetypeConstant.NT_FILE)) {
      if ("public".equals(accessMode)) {
        return builder.append(baseURI)
                      .append("/jcr/")
                      .append(repository)
                      .append("/")
                      .append(workspace)
                      .append(nodePath)
                      .toString();
      }
      return builder.append(baseURI)
                    .append("/private/jcr/")
                    .append(repository)
                    .append("/")
                    .append(workspace)
                    .append(nodePath)
                    .toString();
    }
    WCMConfigurationService configurationService = (WCMConfigurationService) ExoContainerContext
        .getCurrentContainer().getComponentInstanceOfType(WCMConfigurationService.class);
    String parameterizedPageViewerURI = configurationService.
        getRuntimeContextParam(WCMConfigurationService.PARAMETERIZED_PAGE_URI);
    return baseURI.replace("/rest", "") + "/" + accessMode + "/" + currentPortal
        + parameterizedPageViewerURI + "/" + repository + "/" + workspace + nodePath;
  }

  /**
   * Sets the current portal.
   *
   * @param currentPortal the new current portal
   */
  public void setCurrentPortal(String currentPortal) {
    this.currentPortal = currentPortal;
  }

  /**
   * Gets the current portal.
   *
   * @return the current portal
   */
  public String getCurrentPortal() {
    return this.currentPortal;
  }
}
