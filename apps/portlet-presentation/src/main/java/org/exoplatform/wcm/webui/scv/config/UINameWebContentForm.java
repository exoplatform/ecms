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
package org.exoplatform.wcm.webui.scv.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.portal.application.Preference;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.PortalDataMapper;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.portal.PortalFolderSchemaHandler;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.scv.UISingleContentViewerPortlet;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SAS Author : DANG TAN DUNG dzungdev@gmail.com Sep
 * 8, 2008
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/SingleContentViewer/config/UINameWebContentForm.gtmpl",
    events = {
      @EventConfig(listeners = UINameWebContentForm.SaveActionListener.class),
      @EventConfig(listeners = UINameWebContentForm.AbortActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UINameWebContentForm.ChangeTemplateTypeActionListener.class, phase = Phase.DECODE)
    }
)
public class UINameWebContentForm extends UIForm {

  /** The Constant NAME_WEBCONTENT. */
  public static final String NAME_WEBCONTENT    = "name".intern();

  /** The Constant SUMMARY_WEBCONTENT. */
  public static final String SUMMARY_WEBCONTENT = "summary".intern();

  /** The Constant FIELD_SELECT. */
  public static final String FIELD_SELECT = "selectTemplate".intern();

  /** The picture describe. */
  private String pictureDescribe;

  /**
   * Instantiates a new uI name web content form.
   *
   * @throws Exception the exception
   */
  public UINameWebContentForm() throws Exception {
    addUIFormInput(new UIFormStringInput(NAME_WEBCONTENT, NAME_WEBCONTENT, null).addValidator(MandatoryValidator.class)
                                                                                .addValidator(ECMNameValidator.class));
    UIFormSelectBox templateSelect = new UIFormSelectBox(FIELD_SELECT, FIELD_SELECT, getListFileType()) ;
    templateSelect.setSelectedValues(new String[] {"exo:webContent"});
    templateSelect.setOnChange("ChangeTemplateType");
    templateSelect.setDefaultValue("exo:webContent");
    setPictureDescribe("exo_webContent");
    addUIFormInput(templateSelect) ;
    setActions(new String[] {"Save", "Abort"});
  }

  private List<SelectItemOption<String>> getListFileType() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    NodeTypeManager nodeTypeManager = repositoryService.getCurrentRepository().getNodeTypeManager();
    List<String> acceptableContentTypes = templateService.getDocumentTemplates();
    if(acceptableContentTypes.size() == 0) return options;
    String userName = Util.getPortalRequestContext().getRemoteUser();
    for(String contentType: acceptableContentTypes) {
      NodeType nodeType = nodeTypeManager.getNodeType(contentType);
      if (nodeType.isNodeType("exo:webContent")) {
        String label = templateService.getTemplateLabel(contentType);
        try{
          String templatePath = templateService.getTemplatePathByUser(true, contentType, userName);
          if ((templatePath != null) && (templatePath.length() > 0)) {
            options.add(new SelectItemOption<String>(label, contentType));
          }
        }catch(Exception ex){continue;}
      }
    }
    Collections.sort(options, new ItemOptionNameComparator()) ;
    return options ;
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveActionListener<code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SaveActionEvent
   */
  public static class SaveActionListener extends EventListener<UINameWebContentForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINameWebContentForm> event) throws Exception {
      UINameWebContentForm uiNameWebContentForm = event.getSource();
      UIApplication uiApplication = uiNameWebContentForm.getAncestorOfType(UIApplication.class);
      String portalName = Util.getUIPortalApplication().getOwner();
      LivePortalManagerService livePortalManagerService = uiNameWebContentForm.
          getApplicationComponent(LivePortalManagerService.class);
      Node portalNode = livePortalManagerService.getLivePortal(Utils.getSessionProvider(), portalName);
      WebSchemaConfigService webSchemaConfigService = uiNameWebContentForm.
          getApplicationComponent(WebSchemaConfigService.class);
      PortalFolderSchemaHandler handler = webSchemaConfigService.
          getWebSchemaHandlerByType(PortalFolderSchemaHandler.class);
      Node webContentStorage = handler.getWebContentStorage(portalNode);
      String webContentTitle = ((UIFormStringInput) uiNameWebContentForm.getChildById(NAME_WEBCONTENT)).getValue();
      String webContentName = Utils.cleanString(webContentTitle);

      Node webContentNode = null;
      String contentType = uiNameWebContentForm.getUIFormSelectBox(FIELD_SELECT).getValue();
      try {
        webContentNode = webContentStorage.addNode(webContentName, contentType);
      } catch (RepositoryException e) {
        uiApplication.addMessage(new ApplicationMessage("UINameWebContentForm.msg.non-firstwhiteletter",
                                                        null,
                                                        ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApplication.getUIPopupMessages());
        return;
      }
      WebContentSchemaHandler webContentSchemaHandler = webSchemaConfigService.
          getWebSchemaHandlerByType(WebContentSchemaHandler.class);
      webContentSchemaHandler.createDefaultSchema(webContentNode);
      if (webContentNode.hasProperty("exo:title")) {
        webContentNode.setProperty("exo:title", webContentTitle);
      }
      if (webContentNode.canAddMixin("mix:votable"))
        webContentNode.addMixin("mix:votable");
      if (webContentNode.canAddMixin("mix:commentable"))
        webContentNode.addMixin("mix:commentable");
      webContentStorage.getSession().save();

      NodeLocation webcontentNodeLocation = NodeLocation.make(webContentNode);
      PortletRequestContext context = (PortletRequestContext) event.getRequestContext();
      PortletPreferences prefs = context.getRequest().getPreferences();
      prefs.setValue(UISingleContentViewerPortlet.REPOSITORY, webcontentNodeLocation.getRepository());
      prefs.setValue(UISingleContentViewerPortlet.WORKSPACE, webcontentNodeLocation.getWorkspace());
      prefs.setValue(UISingleContentViewerPortlet.IDENTIFIER, webContentNode.getUUID());
      prefs.store();

      WCMPublicationService wcmPublicationService = uiNameWebContentForm.getApplicationComponent(WCMPublicationService.class);
      wcmPublicationService.updateLifecyleOnChangeContent(webContentNode,
                                                          Util.getPortalRequestContext()
                                                              .getPortalOwner(),
                                                          Util.getPortalRequestContext()
                                                              .getRemoteUser(),
                                                          null);
      if (!Utils.isEditPortletInCreatePageWizard()) {
        String pageId = Util.getUIPortal().getSelectedNode().getPageReference();
        UserPortalConfigService upcService = uiNameWebContentForm.getApplicationComponent(UserPortalConfigService.class);
        wcmPublicationService.updateLifecyleOnChangePage(upcService.getPage(pageId), event.getRequestContext().getRemoteUser());
      }

      UIPortletConfig portletConfig = uiNameWebContentForm.createUIComponent(UIPortletConfig.class, null, null);
      Utils.updatePopupWindow(uiNameWebContentForm, portletConfig, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
      portletConfig.init();
    }
  }

  /**
   * The listener interface for receiving abortAction events.
   * The class that is interested in processing a abortAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAbortActionListener<code> method. When
   * the abortAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AbortActionEvent
   */
  public static class AbortActionListener extends EventListener<UINameWebContentForm> {

    @SuppressWarnings("unchecked")
    public void execute(Event<UINameWebContentForm> event) throws Exception {
      UINameWebContentForm nameWebcontentForm = event.getSource();
      UIPortal uiPortal = Util.getUIPortal();
      PageNode currentPageNode = uiPortal.getSelectedNode();
      DataStorage dataStorage = nameWebcontentForm.getApplicationComponent(DataStorage.class);
      Page currentPage = dataStorage.getPage(currentPageNode.getPageReference());
      ArrayList<Object> applications = new ArrayList<Object>();
      applications.addAll(currentPage.getChildren());
      ArrayList<ModelObject> applicationsTmp = currentPage.getChildren();
      Collections.reverse(applicationsTmp);
      for (Object applicationObject : applicationsTmp) {
        if (applicationObject instanceof Container) {
          continue;
        }
        Application application = Application.class.cast(applicationObject);
        if (application.getId() == null) {
          continue;
        }
        String applicationId = application.getId();
        org.exoplatform.portal.application.PortletPreferences portletPreferences =
            dataStorage.getPortletPreferences(applicationId);
        if (portletPreferences == null) {
          continue;
        }

        boolean isQuickCreate = false;
        String nodeIdentifier = null;

        for (Object preferenceObject : portletPreferences.getPreferences()) {
          Preference preference = Preference.class.cast(preferenceObject);

          if ("isQuickCreate".equals(preference.getName())) {
            isQuickCreate = Boolean.valueOf(preference.getValues().get(0).toString());
            if (!isQuickCreate) break;
          }

          if ("nodeIdentifier".equals(preference.getName())) {
            nodeIdentifier = preference.getValues().get(0).toString();
            if (nodeIdentifier == null || nodeIdentifier.length() == 0) break;
          }
        }

        if (isQuickCreate && (nodeIdentifier == null || nodeIdentifier.length() == 0)) {
          applications.remove(applicationObject);
        }
      }
//      currentPage.setChildren(applications);
      dataStorage.save(currentPage);
      UIPage uiPage = uiPortal.findFirstComponentOfType(UIPage.class);
      if (uiPage != null) {
        uiPage.setChildren(null);
        PortalDataMapper.toUIPage(uiPage, currentPage);
      }
      Utils.closePopupWindow(nameWebcontentForm, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
      Utils.updatePortal((PortletRequestContext)event.getRequestContext());
    }
  }

  /**
   * The listener interface for receiving changeTemplateTypeAction events.
   * The class that is interested in processing a changeTemplateTypeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeTemplateTypeActionListener<code> method. When
   * the changeTemplateTypeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ChangeTemplateTypeActionEvent
   */
  public static class ChangeTemplateTypeActionListener extends EventListener<UINameWebContentForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINameWebContentForm> event) throws Exception {
      UINameWebContentForm uiNameWebContentForm = event.getSource();
      String contentType = uiNameWebContentForm.getUIFormSelectBox(FIELD_SELECT).getValue();
      uiNameWebContentForm.setPictureDescribe(contentType.replace(":", "_"));
    }
  }

  /**
   * Gets the picture describe.
   *
   * @return the picture describe
   */
  public String getPictureDescribe() {
    return pictureDescribe;
  }

  /**
   * Sets the picture describe.
   *
   * @param pictureDescribe the new picture describe
   */
  public void setPictureDescribe(String pictureDescribe) {
    this.pictureDescribe = pictureDescribe;
  }
}
