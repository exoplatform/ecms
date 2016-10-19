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
package org.exoplatform.ecm.webui.component.explorer.versions;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import java.text.DateFormat;
import java.util.*;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * May 3, 2007
 */

@ComponentConfig(
        template = "app:/groovy/webui/component/explorer/versions/UIDiff.gtmpl",
        events = {
                @EventConfig(listeners = UIDiff.CompareActionListener.class),
                @EventConfig(listeners = UIDiff.CloseCompareActionListener.class)
        }
)

public class UIDiff extends UIComponent {

  public static final String FROM_PARAM = "from";

  public static final String TO_PARAM = "to";

  private String rootAuthor_;
  private String rootVersionNum_;
  private String originNodePath_;

  //name, date, ws, path
  private String baseVersionName_;
  private String baseVersionDate_;
  private String baseVersionWs_;
  private String baseVersionPath_;
  private String baseVersionAuthor_;
  private String[] baseVersionLabels_;
  
  private String versionName_;
  private String versionDate_;
  private String versionWs_;
  private String versionPath_;
  private String versionAuthor_;
  private String[] versionLabels_;

  private boolean versionCompareable_ = true ;

  public void setRootAuthor(String author) {
    this.rootAuthor_ = author;
  }


  public void setVersions(Version baseVersion, Version version)
  throws Exception {
    baseVersionName_ = baseVersion.getName();
    baseVersionDate_ = formatDate(baseVersion.getCreated());
    baseVersionWs_ = baseVersion.getSession().getWorkspace().getName();
    baseVersionPath_ = baseVersion.getPath();
    if(baseVersion.hasNode(Utils.JCR_FROZEN) && baseVersion.getNode(Utils.JCR_FROZEN).hasProperty(Utils.EXO_LASTMODIFIER)) {
      baseVersionAuthor_ = baseVersion.getNode(Utils.JCR_FROZEN).getProperty(Utils.EXO_LASTMODIFIER).getString();
    }
    UIDocumentWorkspace uiDocumentWorkspace = getAncestorOfType(UIDocumentWorkspace.class);
    UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);
    if (uiVersionInfo.getCurrentNode().isNodeType(Utils.MIX_VERSIONABLE)) {
      baseVersionLabels_ = uiVersionInfo.getCurrentNode().getVersionHistory().getVersionLabels(baseVersion);
    }
    
    versionName_ = version.getName();
    versionDate_ = formatDate(version.getCreated());
    versionWs_ = version.getSession().getWorkspace().getName();
    versionPath_ = version.getPath();
    if(version.hasNode(Utils.JCR_FROZEN) && version.getNode(Utils.JCR_FROZEN).hasProperty(Utils.EXO_LASTMODIFIER)) {
      versionAuthor_ = version.getNode(Utils.JCR_FROZEN).getProperty(Utils.EXO_LASTMODIFIER).getString();
    }
    if (uiVersionInfo.getCurrentNode().isNodeType(Utils.MIX_VERSIONABLE)) {
      versionLabels_ = uiVersionInfo.getCurrentNode().getVersionHistory().getVersionLabels(version);
    }
    versionCompareable_ = true ;
  }
  
  public void setVersions(Version baseVersion, String versionName, 
                          Calendar versionCalendar, String versionWs, String versionPath) throws Exception {
    baseVersionName_ = baseVersion.getName();
    baseVersionDate_ = formatDate(baseVersion.getCreated());
    baseVersionWs_ = baseVersion.getSession().getWorkspace().getName();
    baseVersionPath_ = baseVersion.getPath();
    
    versionName_ = versionName;
    versionDate_ = formatDate(versionCalendar);
    versionWs_ = versionWs;
    versionPath_ = versionPath;
  }

  public String getText(Node node) throws Exception {
    if(node.hasNode("jcr:content")) {
      Node content = node.getNode("jcr:content");
      if(content.hasProperty("jcr:mimeType")){
        String mimeType = content.getProperty("jcr:mimeType").getString();
        if(content.hasProperty("jcr:data")) {
          if(mimeType.startsWith("text")) {
            return content.getProperty("jcr:data").getString();
          }
          DocumentReaderService readerService = getApplicationComponent(DocumentReaderService.class) ;
          try{
            return readerService.getDocumentReader(mimeType).
            getContentAsText(content.getProperty("jcr:data").getStream()) ;
          }catch (Exception e) {
            versionCompareable_ = false ;
          }
        }
      }
    }
    return null ;
  }

  public String getBaseVersionNum() throws Exception { return  baseVersionName_.contains("rootVersion") ? getRootVersionNum() : baseVersionName_; }
  public String getCurrentVersionNum() throws Exception {return versionName_.contains("rootVersion") ? getRootVersionNum() : versionName_; }

  public String getBaseVersionDate() throws Exception {
    return baseVersionDate_;
  }

  public String getCurrentVersionDate() throws Exception {
    return versionDate_;
  }

  public String getBaseVersionAuthor_() {
    return baseVersionAuthor_;
  }

  public String[] getBaseVersionLabels_() {
    return baseVersionLabels_;
  }

  public String getVersionAuthor_() {
    return versionAuthor_;
  }

  public String[] getVersionLabels_() {
    return versionLabels_;
  }

  private String formatDate(Calendar calendar) {
    Locale currentLocale = Util.getPortalRequestContext().getLocale();
    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, currentLocale);
    return dateFormat.format(calendar.getTime()) ;
  }

  public boolean isCompareable() { return versionCompareable_ ; }

  public String getDifferences() throws Exception {
    Node node1 = getNode(baseVersionWs_, baseVersionPath_);
    Node node2 = getNode(versionWs_, versionPath_);
    String previousText = null;
    String currentText = null;
    if (node1.hasNode("jcr:frozenNode")) {
      previousText = getText(node1.getNode("jcr:frozenNode"));
    } else {
      previousText = getText(getNode(baseVersionWs_, originNodePath_));
    }
    if (node2.hasNode("jcr:frozenNode")) {
      currentText = getText(node2.getNode("jcr:frozenNode"));
    } else {
      currentText = getText(getNode(versionWs_, originNodePath_));
    }
    if((previousText != null)&&(currentText != null)) {
      DiffService diffService = new DiffService();
      DiffResult diffResult = diffService.getDifferencesAsHTML(previousText, currentText, true);
      return (diffResult.getDiffHTML());
    }
    return "";
  }

  public boolean isImage() throws Exception {
    String baseMimetype = "";
    String mimeType = "";
    Node baseNode = getNode(baseVersionWs_, baseVersionPath_);
    Node baseContent = null;
    if (baseNode.hasNode("jcr:frozenNode")) {
      baseContent = baseNode.getNode("jcr:frozenNode").getNode("jcr:content");
    } else {
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      baseContent = getNode(baseVersionWs_, originNodePath_).getNode("jcr:content");
    }
    if(baseContent.hasProperty("jcr:mimeType")) {
      baseMimetype = baseContent.getProperty("jcr:mimeType").getString();
    }
    Node node = getNode(versionWs_, versionPath_);
    Node content = null;
    if (node.hasNode("jcr:frozenNode")) {
      content = node.getNode("jcr:frozenNode").getNode("jcr:content");
    } else {
      content = getNode(versionWs_, originNodePath_).getNode("jcr:content");
    }
    if(baseContent.hasProperty("jcr:mimeType")) {
      mimeType = content.getProperty("jcr:mimeType").getString();
    }
    return mimeType.startsWith("image") && baseMimetype.startsWith("image");
  }

  private String getPreviewImagePath(Node node, String workspace, String version) throws Exception {
    return  "/" + WCMCoreUtils.getPortalName() + "/" + WCMCoreUtils.getRestContextName()+ "/jcr/"
        + WCMCoreUtils.getRepository().getConfiguration().getName() + "/" + workspace + node.getPath()
        + "?version=" + version;
  }

  public String getBaseImage() throws Exception {
    Node node = org.exoplatform.wcm.webui.Utils.getRealNode(getNode(baseVersionWs_, baseVersionPath_).getNode("jcr:frozenNode"));
    return getPreviewImagePath(node, baseVersionWs_, baseVersionName_);
  }

  public String getImage() throws Exception {
    Node node = org.exoplatform.wcm.webui.Utils.getRealNode(getNode(versionWs_, versionPath_).getNode("jcr:frozenNode"));
    return getPreviewImagePath(node, versionWs_, versionName_);
  }

  private Node getNode(String ws, String path) throws Exception {
    DMSConfiguration dmsConf = WCMCoreUtils.getService(DMSConfiguration.class);
    String systemWS = dmsConf.getConfig().getSystemWorkspace();
    ManageableRepository repo = WCMCoreUtils.getRepository(); 
    SessionProvider provider = systemWS.equals(ws) ? WCMCoreUtils.getSystemSessionProvider() :
                                                     WCMCoreUtils.getUserSessionProvider();
    return (Node)provider.getSession(ws, repo).getItem(path);
  }

  public void setRootVersionNum(String rootVersionNum) {
    this.rootVersionNum_ = rootVersionNum;
  }

  public String getRootVersionNum() {
    return rootVersionNum_;
  }

  public void setOriginNodePath(String originNodeId) {
    this.originNodePath_ = originNodeId;
  }

  static public class CloseCompareActionListener extends EventListener<UIDiff> {
    public void execute(Event<UIDiff> event) throws Exception {
      UIDiff uiDiff = event.getSource();
      if(uiDiff.isRendered()) {
        uiDiff.setRendered(false);
      }
      UIDocumentWorkspace uiDocumentWorkspace = uiDiff.getAncestorOfType(UIDocumentWorkspace.class);
      UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);
      uiVersionInfo.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace) ;
    }
  }

  static public class CompareActionListener extends EventListener<UIDiff> {
    public void execute(Event<UIDiff> event) throws Exception {
      UIDiff uiDiff = (UIDiff) event.getSource();
      String fromVersionName = event.getRequestContext().getRequestParameter(FROM_PARAM);
      String toVersionName = event.getRequestContext().getRequestParameter(TO_PARAM);
      UIDocumentWorkspace uiDocumentWorkspace = uiDiff.getAncestorOfType(UIDocumentWorkspace.class);
      UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);
      Node node = uiVersionInfo.getCurrentNode();
      VersionHistory versionHistory = node.getVersionHistory();
      if (fromVersionName.equals(uiVersionInfo.getRootVersionNum())) {
        uiDiff.setVersions(uiVersionInfo.getCurrentNode().getVersionHistory().getRootVersion(), versionHistory.getVersion(toVersionName));
      } else if (toVersionName.equals(uiVersionInfo.getRootVersionNum())) {
        uiDiff.setVersions(versionHistory.getVersion(fromVersionName), uiVersionInfo.getCurrentNode().getVersionHistory().getRootVersion());
      } else {
        uiDiff.setVersions(versionHistory.getVersion(fromVersionName), versionHistory.getVersion(toVersionName));
      }
    }
  }
}
