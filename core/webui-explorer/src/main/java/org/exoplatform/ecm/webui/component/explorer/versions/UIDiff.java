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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.version.Version;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.diff.AddDelta;
import org.exoplatform.services.document.diff.ChangeDelta;
import org.exoplatform.services.document.diff.DeleteDelta;
import org.exoplatform.services.document.diff.Delta;
import org.exoplatform.services.document.diff.DiffService;
import org.exoplatform.services.document.diff.Revision;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          tuan.pham@exoplatform.com
 * May 3, 2007
 */

@ComponentConfig(template = "app:/groovy/webui/component/explorer/versions/UIDiff.gtmpl")

public class UIDiff extends UIComponent {

  //name, date, ws, path
  private String baseVersionName_;
  private String baseVersionDate_;
  private String baseVersionWs_;
  private String baseVersionPath_;
  
  private String versionName_;
  private String versionDate_;
  private String versionWs_;
  private String versionPath_;
  
  private boolean versionCompareable_ = true ;

  public void setVersions(Version baseVersion, Version version)
  throws Exception {
    baseVersionName_ = baseVersion.getName();
    baseVersionDate_ = formatDate(baseVersion.getCreated());
    baseVersionWs_ = baseVersion.getSession().getWorkspace().getName();
    baseVersionPath_ = baseVersion.getPath();
    
    versionName_ = version.getName();
    versionDate_ = formatDate(version.getCreated());
    versionWs_ = version.getSession().getWorkspace().getName();
    versionPath_ = version.getPath();
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

  public String getBaseVersionNum() throws Exception { return  baseVersionName_; }
  public String getCurrentVersionNum() throws Exception {return versionName_; }

  public String getBaseVersionDate() throws Exception {
    return baseVersionDate_;
  }

  public String getCurrentVersionDate() throws Exception {
    return versionDate_;
  }

  private String formatDate(Calendar calendar) {
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    return dateFormat.format(calendar.getTime()) ;
  }

  public boolean isCompareable() { return versionCompareable_ ; }

  public List<Delta> getDeltas() throws Exception {
    List<Delta> deltas = new ArrayList<Delta>();
    String previousText = getText(getNode(versionWs_, versionPath_).getNode("jcr:frozenNode"));
    String currentText = getText(getNode(baseVersionWs_, baseVersionPath_).getNode("jcr:frozenNode"));
    if((previousText != null)&&(currentText != null)) {
      String lineSeparator = DiffService.NL;
      Object[] orig = StringUtils.split(previousText, lineSeparator);
      Object[] rev = StringUtils.split(currentText, lineSeparator);
      DiffService diffService = getApplicationComponent(DiffService.class) ;
      Revision revision = diffService.diff(orig, rev);
      for (int i = 0; i < revision.size(); i++) {
        deltas.add(revision.getDelta(i));
      }
    }
    return deltas;
  }

  public boolean isDeleteDelta(Delta delta) {
    if (delta instanceof DeleteDelta) return true;
    return false;
  }

  public boolean isAddDelta(Delta delta) {
    if (delta instanceof AddDelta) return true;
    return false;
  }

  public boolean isChangeDelta(Delta delta) {
    if (delta instanceof ChangeDelta) return true;
    return false;
  }
  
  private Node getNode(String ws, String path) throws Exception {
    DMSConfiguration dmsConf = WCMCoreUtils.getService(DMSConfiguration.class);
    String systemWS = dmsConf.getConfig().getSystemWorkspace();
    ManageableRepository repo = WCMCoreUtils.getRepository(); 
    SessionProvider provider = systemWS.equals(ws) ? WCMCoreUtils.getSystemSessionProvider() :
                                                     WCMCoreUtils.getUserSessionProvider();
    return (Node)provider.getSession(ws, repo).getItem(path);
  }
}
