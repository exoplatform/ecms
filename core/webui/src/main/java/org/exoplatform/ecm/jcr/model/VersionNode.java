/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.jcr.model;

import java.util.*;

import javax.jcr.*;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class VersionNode {
  private static final String EXO_LAST_MODIFIED_DATE = "exo:lastModifiedDate";

  private boolean           isExpanded     = true;

  private List<VersionNode> children_      = new ArrayList<VersionNode>();

  private static final Log  LOG            = ExoLogger.getLogger(VersionNode.class.getName());

  private Calendar          createdTime_;

  private String            name_          = "";

  private String            displayName         = "";

  private String            path_          = "";

  private String            ws_            = "";

  private String            uuid_;

  private String[]          versionLabels_ = new String[] {};

  private String            author_        = "";

  public VersionNode(Node node, Session session) {
    Version version = node instanceof Version ? (Version) node : null;
    try {
      createdTime_ = getProperty(node, EXO_LAST_MODIFIED_DATE).getDate();
      name_ = version == null ? "" : version.getName();
      path_ = node.getPath();
      ws_ = node.getSession().getWorkspace().getName();
      uuid_ = node.getUUID();
      Property prop = getProperty(node, Utils.EXO_LASTMODIFIER);
      author_ = prop == null ? null : prop.getString();
      if (version == null) {
        if (node.isNodeType(Utils.MIX_VERSIONABLE)) {
          addVersions(node, session);
        }
      } else {
        addVersions(node, session);
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  private void addVersions(Node node, Session session) throws RepositoryException {
    if(node instanceof Version) {
      Version version = (Version) node;
      versionLabels_ = version.getContainingHistory().getVersionLabels(version);
    } else {
      int maxVersion = 0;
      Map<String, String> mapVersionName = new HashMap<String, String>();
      if(node.isNodeType(VersionHistoryUtils.MIX_DISPLAY_VERSION_NAME)){
        //maxVersion of root version
        if(node.hasProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY)){
          maxVersion = (int) node.getProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY).getLong();
        }
        //list of version name entries (jcrID, display version)
        if(node.hasProperty(VersionHistoryUtils.LIST_VERSION_PROPERTY)){
          Value[] values = node.getProperty(VersionHistoryUtils.LIST_VERSION_PROPERTY).getValues();
          for (Value value : values){
            mapVersionName.put(value.getString().split(VersionHistoryUtils.VERSION_SEPARATOR)[0],
                    value.getString().split(VersionHistoryUtils.VERSION_SEPARATOR)[1]);
          }
        }
      }
      Version rootVersion = node.getVersionHistory().getRootVersion();
      VersionIterator allVersions = node.getVersionHistory().getAllVersions();
      int maxIndex = 0;
      while (allVersions.hasNext()) {
        Version version = allVersions.nextVersion();
        String versionOffset = mapVersionName.get(version.getName());

        if(version.getUUID().equals(rootVersion.getUUID())) {
          continue;
        }
        int versionIndex = Integer.parseInt(version.getName()) -1;
        maxIndex = Math.max(maxIndex , versionIndex);
        VersionNode versionNode = new VersionNode(version, session);
        if(versionOffset != null) {
          versionNode.setDisplayName(String.valueOf(versionOffset));
        }else{
          versionNode.setDisplayName(String.valueOf(versionIndex));
        }
        children_.add(versionNode);
      }
      name_ = String.valueOf(maxIndex + 1);
      displayName = maxVersion > 0 ?  String.valueOf(maxVersion) : String.valueOf(maxIndex +1);
      versionLabels_ = node.getVersionHistory().getVersionLabels(rootVersion);
    }
  }

  private Property getProperty(Node node, String propName) throws RepositoryException {
    Property property = null;
    if (node.hasProperty(propName)) {
      property = node.getProperty(propName);
    } else if (node.hasNode(Utils.JCR_FROZEN) && node.getNode(Utils.JCR_FROZEN).hasProperty(propName)) {
      property = node.getNode(Utils.JCR_FROZEN).getProperty(propName);
    }
    return property;
  }

  public boolean isExpanded() {
    return isExpanded;
  }

  public void setExpanded(boolean isExpanded) {
    this.isExpanded = isExpanded;
  }

  public String getName() {
    return name_;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName_) {
    this.displayName = displayName_;
  }

  public String getWs() {
    return ws_;
  }

  public String getPath() {
    return path_;
  }

  public int getChildrenSize() {
    return children_.size();
  }

  public List<VersionNode> getChildren() {
    return children_;
  }

  public Calendar getCreatedTime() {
    return createdTime_;
  }

  public String getAuthor() {
    return author_;
  }

  public String[] getVersionLabels() {
    return versionLabels_;
  }

  public Node getNode(String nodeName) throws RepositoryException {
    DMSConfiguration dmsConf = WCMCoreUtils.getService(DMSConfiguration.class);
    String systemWS = dmsConf.getConfig().getSystemWorkspace();
    ManageableRepository repo = WCMCoreUtils.getRepository();
    SessionProvider provider = systemWS.equals(ws_) ? WCMCoreUtils.getSystemSessionProvider()
                                                   : WCMCoreUtils.getUserSessionProvider();
    return ((Node) provider.getSession(ws_, repo).getItem(path_)).getNode(nodeName);
  }

  public boolean hasNode(String nodeName) throws Exception {
    DMSConfiguration dmsConf = WCMCoreUtils.getService(DMSConfiguration.class);
    String systemWS = dmsConf.getConfig().getSystemWorkspace();
    ManageableRepository repo = WCMCoreUtils.getRepository();
    SessionProvider provider = systemWS.equals(ws_) ? WCMCoreUtils.getSystemSessionProvider()
                                                   : WCMCoreUtils.getUserSessionProvider();
    return ((Node) provider.getSession(ws_, repo).getItem(path_)).hasNode(nodeName);
  }

  public String getUUID() {
    return uuid_;
  }

  public VersionNode findVersionNode(String path) throws RepositoryException {
    if (path_.equals(path))
      return this;
    VersionNode node = null;
    Iterator<VersionNode> iter = children_.iterator();
    while (iter.hasNext()) {
      VersionNode child = (VersionNode) iter.next();
      node = child.findVersionNode(path);
      if (node != null)
        return node;
    }
    return null;
  }

  public void removeVersionInChild(VersionNode versionNode1, VersionNode versionNodeRemove) throws RepositoryException {
    if (versionNode1.getChildren().contains(versionNodeRemove))
      versionNode1.getChildren().remove(versionNodeRemove);
    else {
      for (VersionNode vsN : versionNode1.getChildren()) {
        removeVersionInChild(vsN, versionNodeRemove);
      }
    }
  }

}
