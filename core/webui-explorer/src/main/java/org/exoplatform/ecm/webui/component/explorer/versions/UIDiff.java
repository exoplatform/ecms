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
import java.util.Calendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.diff.DiffResult;
import org.exoplatform.commons.diff.DiffService;
import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.document.DocumentReader;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.HandlerNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL Author : Pham Tuan tuan.pham@exoplatform.com
 * May 3, 2007
 */

@ComponentConfig(template = "app:/groovy/webui/component/explorer/versions/UIDiff.gtmpl", events = {
    @EventConfig(listeners = UIDiff.CompareActionListener.class),
    @EventConfig(listeners = UIDiff.CloseCompareActionListener.class) })
public class UIDiff extends UIComponent {
  private static final String EXO_LAST_MODIFIED_DATE = "exo:lastModifiedDate";

  private static final Log   LOG                 = ExoLogger.getLogger(UIDiff.class.getName());

  public static final String FROM_PARAM          = "from";

  public static final String TO_PARAM            = "to";

  private String             baseVersionName_;

  private String             baseVersionDate_;

  private String             baseVersionAuthor_;

  private String             baseVersionLabel_;

  private String             versionName_;

  private String             versionDate_;

  private String             versionAuthor_;

  private String             versionLabel_;

  private String             previousText_       = null;

  private String             currentText_        = null;

  private boolean            versionCompareable_ = true;

  private String             diffResultHTML_     = null;

  private boolean            isImage_            = false;

  private String             baseImage_          = null;

  private String             currentImage_       = null;

  public void setVersions(Node baseVersion, Node version) throws Exception {
    UIDocumentWorkspace uiDocumentWorkspace = getAncestorOfType(UIDocumentWorkspace.class);
    UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);

    versionCompareable_ = baseVersion != null && version != null && !baseVersion.getUUID().equals(version.getUUID());
    if (!versionCompareable_) {
      throw new IllegalStateException("Can't compare both versions");
    } else {
      Node versionRootVersion = null;
      if (version instanceof Version) {
        versionRootVersion = ((Version) version).getContainingHistory().getRootVersion();
      } else {
        versionRootVersion = version.getVersionHistory().getRootVersion();
      }

      Node baseVersionRootVersion = null;
      if (baseVersion instanceof Version) {
        baseVersionRootVersion = ((Version) baseVersion).getContainingHistory().getRootVersion();
      } else {
        baseVersionRootVersion = baseVersion.getVersionHistory().getRootVersion();
      }

      versionCompareable_ = baseVersionRootVersion.getUUID().endsWith(versionRootVersion.getUUID());
      if (!versionCompareable_) {
        throw new IllegalStateException("Versions to compare are the same");
      }
    }

    Node currentNode = uiVersionInfo.getCurrentNode();
    VersionHistory versionHistory = currentNode.getVersionHistory();
    Version rootVersion = versionHistory.getRootVersion();

    // Make baseVersion parameter always the oldest version
    if (currentNode.getUUID().equals(baseVersion.getUUID())) {
      // switch version and baseVersion to make sure that baseVersion is the
      // oldest
      Node tmpNode = baseVersion;
      baseVersion = version;
      version = tmpNode;
    } else if (currentNode.getUUID().equals(version.getUUID())) {
      // It's ok, the version' indice > baseVersion indice
    } else if (baseVersion instanceof Version && version instanceof Version) {
      // compare versions by indice
      int baseVersionIndice = Integer.parseInt(baseVersion.getName());
      int versionNumIndice = Integer.parseInt(version.getName());

      if (baseVersionIndice == versionNumIndice) {
        throw new IllegalStateException("Can't compare the same version");
      } else if (baseVersionIndice > versionNumIndice) {
        Node tmpNode = baseVersion;
        baseVersion = version;
        version = tmpNode;
      }
    }

    // Base version is of type Version all the time
    baseVersionName_ = ((Version) baseVersion).getName();
    // Current version can be of type Version or Node (draft node)
    versionName_ = version instanceof Version ? version.getName() : uiVersionInfo.getRootVersionNum();

    // Base version is of type Version all the time
    Calendar modifiedDate = getModifiedDate(baseVersion);
    baseVersionDate_ = formatDate(modifiedDate);

    // Current version can be of type Version or Node (draft node)
    modifiedDate = getModifiedDate(version);
    versionDate_ = formatDate(modifiedDate);

    baseVersionAuthor_ = getLastModifier(baseVersion);
    versionAuthor_ = getLastModifier(version);

    // Base version is of type Version all the time
    String[] baseVersionLabels = versionHistory.getVersionLabels((Version) baseVersion);
    baseVersionLabel_ = baseVersionLabels == null || baseVersionLabels.length == 0 ? null : baseVersionLabels[0];
    // Current version can be of type Version or Node (draft node)
    if (version instanceof Version) {
      String[] versionLabels = versionHistory.getVersionLabels((Version) version);
      versionLabel_ = versionLabels == null || versionLabels.length == 0 ? null : versionLabels[0];
    } else {
      String[] versionLabels = versionHistory.getVersionLabels(rootVersion);
      versionLabel_ = versionLabels == null || versionLabels.length == 0 ? null : versionLabels[0];
    }

    isImage_ = isOriginalNodeImage(version) && isOriginalNodeImage(baseVersion);

    previousText_ = null;
    currentText_ = null;
    diffResultHTML_ = null;
    currentImage_ = null;
    baseImage_ = null;
    isImage_ = isOriginalNodeImage(version) && isOriginalNodeImage(baseVersion);

    if (isImage_) {
      String wsName = currentNode.getSession().getWorkspace().getName();
      String originalNodePath = currentNode.getPath();
      String basePath = "/" + WCMCoreUtils.getPortalName() + "/" + WCMCoreUtils.getRestContextName() + "/jcr/"
          + WCMCoreUtils.getRepository().getConfiguration().getName() + "/" + wsName + originalNodePath;

      long timeInMS = Calendar.getInstance().getTimeInMillis();

      currentImage_ = basePath + (currentNode.getUUID().equals(version.getUUID()) ? "?" + timeInMS : ("?version=" + version.getName() + "&" + timeInMS));
      baseImage_ = basePath + (currentNode.getUUID().equals(baseVersion.getUUID()) ? "?" + timeInMS : "?version=" + baseVersion.getName() + "&" + timeInMS);
    } else {
      try {
        previousText_ = getText(baseVersion);
        currentText_ = getText(version);
      } catch (HandlerNotFoundException e) {
        versionCompareable_ = false;
      }

      if (versionCompareable_) {
        if (StringUtils.isBlank(previousText_) && StringUtils.isBlank(currentText_)) {
          versionCompareable_ = false;
        } else if ((previousText_ != null) && (currentText_ != null)) {
          DiffService diffService = WCMCoreUtils.getService(DiffService.class);
          DiffResult diffResult = diffService.getDifferencesAsHTML(previousText_, currentText_, true);
          diffResultHTML_ = diffResult.getDiffHTML();
        }
      }
    }
  }

  public String getBaseVersionNum() throws Exception {
    return baseVersionName_;
  }

  public String getCurrentVersionNum() throws Exception {
    return versionName_;
  }

  public String getBaseVersionDate() throws Exception {
    return baseVersionDate_;
  }

  public String getCurrentVersionDate() throws Exception {
    return versionDate_;
  }

  public String getBaseVersionAuthor() {
    return baseVersionAuthor_;
  }

  public String getCurrentVersionAuthor() {
    return versionAuthor_;
  }

  public String getBaseVersionLabel() {
    return baseVersionLabel_;
  }

  public String getCurrentVersionLabel() {
    return versionLabel_;
  }

  public String getBaseImage() throws Exception {
    return baseImage_;
  }

  public String getCurrentImage() throws Exception {
    return currentImage_;
  }

  public boolean isCompareable() {
    return versionCompareable_;
  }

  public String getDifferences() throws Exception {
    return diffResultHTML_;
  }

  public boolean isImage() {
    return isImage_;
  }

  private Calendar getModifiedDate(Node node) throws RepositoryException {
    Property property = getProperty(node, EXO_LAST_MODIFIED_DATE);
    return property == null ? null : property.getDate();
  }

  private String getLastModifier(Node node) throws RepositoryException {
    Property property = getProperty(node, Utils.EXO_LASTMODIFIER);
    return property == null ? null : property.getString();
  }

  private boolean isOriginalNodeImage(Node node) throws Exception {
    Property mimeProperty = getProperty(node, Utils.JCR_MIMETYPE);
    String mimeType = mimeProperty == null ? null : mimeProperty.getString();
    return StringUtils.isNotBlank(mimeType) && mimeType.startsWith("image");
  }

  private String getText(Node node) throws Exception {
    Property mimeProperty = getProperty(node, Utils.JCR_MIMETYPE);
    Property dataProperty = getProperty(node, Utils.JCR_DATA);
    if (mimeProperty != null && dataProperty != null) {
      String mimeType = mimeProperty.getString();
      if (mimeType.startsWith("text")) {
        return dataProperty.getString();
      }
      DocumentReaderService readerService = getApplicationComponent(DocumentReaderService.class);
      DocumentReader documentReader = readerService.getDocumentReader(mimeType);
      if (documentReader != null) {
        try {
          return documentReader.getContentAsText(dataProperty.getStream());
        } catch (Exception e) {
          LOG.warn("An error occured while getting text from file " + node.getPath() + " with mimeType " + mimeType
              + " with document reader = " + documentReader + ". File comparaison will be disabled", e);
        }
      }
    }
    throw new HandlerNotFoundException();
  }

  private Property getProperty(Node node, String propertyName) throws RepositoryException, PathNotFoundException {
    Property property = null;
    if (node instanceof Version) {
      if (node.hasNode(Utils.JCR_FROZEN)) {
        node = node.getNode(Utils.JCR_FROZEN);
      } else {
        return null;
      }
    }
    if (node.hasProperty(propertyName)) {
      property = node.getProperty(propertyName);
    } else if (node.hasNode(Utils.JCR_CONTENT)) {
      Node content = node.getNode(Utils.JCR_CONTENT);
      if (content.hasProperty(propertyName)) {
        property = content.getProperty(propertyName);
      }
    }
    return property;
  }

  private String formatDate(Calendar calendar) {
    Locale currentLocale = Util.getPortalRequestContext().getLocale();
    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, currentLocale);
    return dateFormat.format(calendar.getTime());
  }

  static public class CloseCompareActionListener extends EventListener<UIDiff> {
    public void execute(Event<UIDiff> event) throws Exception {
      UIDiff uiDiff = event.getSource();
      if (uiDiff.isRendered()) {
        uiDiff.setRendered(false);
      }
      UIDocumentWorkspace uiDocumentWorkspace = uiDiff.getAncestorOfType(UIDocumentWorkspace.class);
      UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);
      uiVersionInfo.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentWorkspace);
    }
  }

  static public class CompareActionListener extends EventListener<UIDiff> {
    public void execute(Event<UIDiff> event) throws Exception {
      UIDiff uiDiff = (UIDiff) event.getSource();
      String fromVersionName = event.getRequestContext().getRequestParameter(FROM_PARAM);
      String toVersionName = event.getRequestContext().getRequestParameter(TO_PARAM);
      UIDocumentWorkspace uiDocumentWorkspace = uiDiff.getAncestorOfType(UIDocumentWorkspace.class);
      UIVersionInfo uiVersionInfo = uiDocumentWorkspace.getChild(UIVersionInfo.class);
      uiDiff.setVersions(uiVersionInfo.getVersion(fromVersionName), uiVersionInfo.getVersion(toVersionName));
    }
  }
}
