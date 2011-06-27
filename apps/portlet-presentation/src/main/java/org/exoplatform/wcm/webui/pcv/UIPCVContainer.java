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
package org.exoplatform.wcm.webui.pcv;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.pcv.config.UIPCVConfig;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS Author : Phan Le Thanh Chuong
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com Nov 4, 2008
 */

@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "app:/groovy/ParameterizedContentViewer/UIPCVContainer.gtmpl",
  events = {
    @EventConfig(listeners = UIPCVContainer.QuickEditActionListener.class),
    @EventConfig(listeners = UIPCVContainer.EditActionListener.class)
  }
)
public class UIPCVContainer extends UIContainer {

  /** Flag indicating the draft revision. */
  private boolean isDraftRevision = false;

  /** Flag indicating the obsolete revision. */
  private boolean isObsoletedContent = false;

  /** Content child of this content. */
  private UIPCVPresentation uiContentViewer;

  /** The repository. */
  private String repository;

  /**
   * A flag used to display Print/Close buttons and hide Back one if its' value
   * is <code>true</code>. In <code>false</code> case, the Back button will be
   * shown only
   */
  private boolean            isPrint;

  /** The Constant PREFERENCE_REPOSITORY. */
  public static final String PREFERENCE_REPOSITORY = "repository";

  /** The date formatter. */
  private DateFormat               dateFormatter = null;

  /**
   * Instantiates a new uI content viewer container.
   *
   * @throws Exception the exception
   */
  public UIPCVContainer() throws Exception {

    addChild(UIPCVPresentation.class, null, null);
    uiContentViewer = getChild(UIPCVPresentation.class);
    PortletRequestContext porletRequestContext = WebuiRequestContext.getCurrentInstance();
    repository = porletRequestContext.getRequest().getPreferences().getValue(PREFERENCE_REPOSITORY, "");
    dateFormatter = new SimpleDateFormat();
    ((SimpleDateFormat) dateFormatter).applyPattern("dd.MM.yyyy '|' hh'h'mm");
  }

  /**
   * Checks if is show title.
   *
   * @return true, if is show title
   */
  public boolean isShowTitle() {
    PortletPreferences portletPreferences = getPortletPreferences();
    String showAble = portletPreferences.getValue(UIPCVPortlet.SHOW_TITLE, null);
    return (showAble != null) ? Boolean.parseBoolean(showAble) : false;
  }

  /**
   * Gets the portlet preferences.
   *
   * @return the portlet preferences
   */
  private PortletPreferences getPortletPreferences() {
    PortletRequestContext context = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPreferences = context.getRequest().getPreferences();
    return portletPreferences;
  }

  /**
   * Gets the title.
   *
   * @param node the node
   *
   * @return the title
   *
   * @throws Exception the exception
   */
  public String getTitle(Node node) throws Exception {
    String title = null;
    if (node.hasNode("jcr:content")) {
      Node content = node.getNode("jcr:content");
      if (content.hasProperty("dc:title")) {
        try {
          title = content.getProperty("dc:title").getValues()[0].getString();
        } catch (Exception e) {
          title = null;
        }
      }
    }
    if (node.hasProperty("exo:title")) {
      title = node.getProperty("exo:title").getValue().getString();
    }
    if (title==null) title = node.getName();

    return title;
  }

  /**
   * Checks if is show date created.
   *
   * @return true, if is show date created
   */
  public boolean isShowDateCreated() {
    PortletPreferences portletPreferences = getPortletPreferences();
    String showAble = portletPreferences.getValue(UIPCVPortlet.SHOW_DATE_CREATED, null);
    return (showAble != null) ? Boolean.parseBoolean(showAble) : false;
  }

  /**
   * Checks if is show date created.
   *
   * @return true, if is show date created
   */
  public boolean isShowBar() {
    PortletPreferences portletPreferences = getPortletPreferences();
    String showAble = portletPreferences.getValue(UIPCVPortlet.SHOW_BAR, null);
    return (showAble != null) ? Boolean.parseBoolean(showAble) : false;
  }

  /**
   * Gets the created date.
   *
   * @param node the node
   *
   * @return the created date
   *
   * @throws Exception the exception
   */
  public String getCreatedDate(Node node) throws Exception {
    if (node.hasProperty("exo:dateCreated")) {
      Calendar calendar = node.getProperty("exo:dateCreated").getValue().getDate();
      return dateFormatter.format(calendar.getTime());
    }
    return null;
  }

  /**
   * Gets the repository.
   *
   * @return the repository
   *
   * @throws RepositoryException the repository exception
   */
  public String getRepository() throws RepositoryException {
    return repository;
  }

  /**
   * Sets the repository.
   *
   * @param repository the new repository
   */
  public void setRepository(String repository) {
    this.repository = repository;
  }

  /**
   * Gets the node.
   *
   * @return the node
   *
   * @throws Exception the exception
   */
  public Node getNode() throws Exception {
    String parameters = getRequestParameters();
//    Node node = getNodebyPath(parameters);
//    if (node == null) node = getNodeByCategory(parameters);
//    if (node == null) return null;
//    NodeLocation nodeLocation = NodeLocation.make(node);
    Node nodeView = Utils.getViewableNodeByComposer(null, null, parameters);
    if (nodeView!=null) {
      boolean isDocumentType = false;
      if (nodeView.isNodeType("nt:frozenNode")) isDocumentType = true;
      // check node is a document node
      TemplateService templateService = getApplicationComponent(TemplateService.class);
      List<String> documentTypes = templateService.getDocumentTemplates();
      for (String documentType : documentTypes) {
        if (nodeView.isNodeType(documentType)) {
          isDocumentType = true;
          break;
        }
      }
      if (!isDocumentType) return null;
      if (hasChildren()) removeChild(UIPCVContainer.class);

      // set node view for UIPCVPresentation
      if (nodeView != null && nodeView.isNodeType("nt:frozenNode")) {
        String nodeUUID = nodeView.getProperty("jcr:frozenUuid").getString();
        uiContentViewer.setOriginalNode(nodeView.getSession().getNodeByUUID(nodeUUID));
        uiContentViewer.setNode(nodeView);
      } else if (nodeView == null) {
        return null;
      } else {
        uiContentViewer.setOriginalNode(nodeView);
        uiContentViewer.setNode(nodeView);
      }
      uiContentViewer.setRepository(this.getRepository());
      uiContentViewer.setWorkspace(nodeView.getSession().getWorkspace().getName());
      isPrint = Boolean.parseBoolean(Util.getPortalRequestContext().getRequestParameter("isPrint"));
    }
    return nodeView;
  }

  /**
   * Gets the request parameters.
   *
   * @return the request parameters
   */
  private String getRequestParameters() throws Exception {
    String parameters = null;
    try {
      parameters = URLDecoder.decode(StringUtils.substringAfter(Util.getPortalRequestContext()
                                                                    .getNodePath(),
                                                                Util.getUIPortal()
                                                                    .getSelectedUserNode()
                                                                    .getURI()
                                                                    + "/"), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return null;
    }

    if (!parameters.matches(UIPCVPresentation.PARAMETER_REGX)) {
      String path = Util.getPortalRequestContext().getRequestParameter("path");
      if (path == null){
        return getAncestorOfType(UIPCVPortlet.class).getCurrentNodePath();
      }

      parameters = Util.getPortalRequestContext().getRequestParameter("path").substring(1);
      return parameters;
    }
    return parameters;
  }

  /**
   * The listener interface for receiving quickEditAction events. The class
   * that is interested in processing a quickEditAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addQuickEditActionListener<code> method. When
   * the quickEditAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see QuickEditActionEvent
   */
  public static class QuickEditActionListener extends EventListener<UIPCVContainer> {
    /*
     * (non-Javadoc)
     *
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPCVContainer> event) throws Exception {
      UIPCVContainer uiContentViewerContainer = event.getSource();
      UIPCVConfig pcvConfigForm = uiContentViewerContainer.createUIComponent(UIPCVConfig.class, null, null);
      Utils.createPopupWindow(uiContentViewerContainer, pcvConfigForm, UIPCVPortlet.PCV_CONFIG_POPUP_WINDOW, 600);
    }
  }

  /**
   * The listener interface for receiving quickEditAction events. The class that
   * is interested in processing a quickEditAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addQuickEditActionListener<code> method. When
   * the quickEditAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see EditActionListener
   */
  public static class EditActionListener extends EventListener<UIPCVContainer> {
    /*
     * (non-Javadoc)
     *
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPCVContainer> event) throws Exception {
      UIPCVContainer uiContentViewerContainer = event.getSource();
      UIPCVPresentation uiContentViewer = uiContentViewerContainer.getChild(UIPCVPresentation.class);
      Node orginialNode = uiContentViewer.getOriginalNode();
      UIContentDialogForm uiDocumentDialogForm = uiContentViewerContainer.createUIComponent(UIContentDialogForm.class,
                                                                                            null,
                                                                                            null);
      try {
        uiDocumentDialogForm.init(orginialNode, false);
        Utils.createPopupWindow(uiContentViewerContainer,
                                uiDocumentDialogForm,
                                UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW,
                                800);
      } catch (Exception ex) {
        UIApplication uiApp = uiContentViewerContainer.getAncestorOfType(UIApplication.class);
        Object[] arg = { orginialNode.getPrimaryNodeType().getName() };
        uiApp.addMessage(new ApplicationMessage("UIPCVContainer.msg.not-support",
                                                arg,
                                                ApplicationMessage.ERROR));
      }
    }
  }

  /**
   * Gets <code>isPrint</code> value that is used to display Print/Close
   * buttons and hide Back one if its' value is <code>True</code>. In
   * <code>False</code> case, the Back button will be shown only.
   *
   * @return <code>isPrint</code>
   */
  public boolean getIsPrint() {
    return isPrint;
  }

  /**
   * Sets <code>isPrint</code> value that is used to display Print/Close
   * buttons and hide Back one if its' value is <code>True</code>. In
   * <code>False</code> case, the Back button will be shown only.
   *
   * @param isPrint the is print
   */
  public void setIsPrint(boolean isPrint) {
    this.isPrint = isPrint;
  }

  /**
   * Gets the draft revision value. If the revision is draft, an icon and one
   * text is shown. Otherwise, false.
   *
   * @return <code>isDraftRevision</code>
   */
  public boolean isDraftRevision() {
    return isDraftRevision;
  }

  /**
   * Sets the draft revision value. If the revision is draft, an icon and one
   * text is shown. Otherwise, false.
   *
   * @param isDraftRevision the is draft revision
   */
  public void setDraftRevision(boolean isDraftRevision) {
    this.isDraftRevision = isDraftRevision;
  }

  /**
   * Gets the draft obsolete value. If the revision is draft, the message is
   * shown to inform users of this state. Otherwise, false.
   *
   * @return <code>isDraftRevision</code>
   */
  public boolean isObsoletedContent() {
    return isObsoletedContent;
  }

  /**
   * Sets the draft obsolete value. If the revision is draft, the message is
   * shown to inform users of this state. Otherwise, false.
   *
   * @param isObsoletedContent the is obsoleted content
   */
  public void setObsoletedContent(boolean isObsoletedContent) {
    this.isObsoletedContent = isObsoletedContent;
  }
}
