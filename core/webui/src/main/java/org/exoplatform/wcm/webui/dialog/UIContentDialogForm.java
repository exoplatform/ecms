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
package org.exoplatform.wcm.webui.dialog;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.portlet.PortletMode;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.DialogFormUtil;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.ecm.webui.form.DialogFormActionListeners;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Oct 29, 2009
 */
@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    events = {
      @EventConfig(listeners = UIContentDialogForm.AddActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIContentDialogForm.RemoveReferenceActionListener.class,
                   confirm = "DialogFormField.msg.confirm-delete", phase = Phase.DECODE),
      @EventConfig(listeners = UIContentDialogForm.SaveDraftActionListener.class),
      @EventConfig(listeners = UIContentDialogForm.FastPublishActionListener.class),
      @EventConfig(listeners = UIContentDialogForm.PreferencesActionListener.class),
      @EventConfig(listeners = UIContentDialogForm.CloseActionListener.class),
      @EventConfig(listeners = DialogFormActionListeners.RemoveDataActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = DialogFormActionListeners.ChangeTabActionListener.class, phase = Phase.DECODE)
    }
)
public class UIContentDialogForm extends UIDialogForm  implements UIPopupComponent, UISelectable {

  /** The Constant CONTENT_DIALOG_FORM_POPUP_WINDOW. */
  public static final String CONTENT_DIALOG_FORM_POPUP_WINDOW = "UIContentDialogFormPopupWindow";

  /** The Constant FIELD_TAXONOMY. */
  public static final String FIELD_TAXONOMY = "categories";

  /** The Constant TAXONOMY_CONTENT_POPUP_WINDOW. */
  public static final String TAXONOMY_CONTENT_POPUP_WINDOW = "UIContentPopupWindow";

  /** The Log **/
  private static final Log LOG  = ExoLogger.getLogger(UIContentDialogForm.class.getName());

  /** The webcontent node location. */
  private NodeLocation webcontentNodeLocation;

  /** The list taxonomy. */
  private List<String> listTaxonomy = new ArrayList<String>();

  /** The list taxonomy name. */
  private List<String> listTaxonomyName = new ArrayList<String>();

  /** The template. */
  private String template;


  /**
   * Gets the list taxonomy.
   *
   * @return the list taxonomy
   */
  public List<String> getListTaxonomy() {
    return listTaxonomy;
  }

  /**
   * Gets the list taxonomy name.
   *
   * @return the list taxonomy name
   */
  public List<String> getlistTaxonomyName() {
    return listTaxonomyName;
  }

  /**
   * Sets the list taxonomy.
   *
   * @param listTaxonomyNew the new list taxonomy
   */
  public void setListTaxonomy(List<String> listTaxonomyNew) {
    listTaxonomy = listTaxonomyNew;
  }

  /**
   * Sets the list taxonomy name.
   *
   * @param listTaxonomyNameNew the new list taxonomy name
   */
  public void setListTaxonomyName(List<String> listTaxonomyNameNew) {
    listTaxonomyName = listTaxonomyNameNew;
  }

  /** The preference component. */
  private Class<? extends UIContentDialogPreference> preferenceComponent;

  /**
   * Gets the webcontent node location.
   *
   * @return the webcontent node location
   */
  public NodeLocation getWebcontentNodeLocation() {
    return webcontentNodeLocation;
  }

  /**
   * Sets the webcontent node location.
   *
   * @param webcontentNodeLocation the new webcontent node location
   */
  public void setWebcontentNodeLocation(NodeLocation webcontentNodeLocation) {
    this.webcontentNodeLocation = webcontentNodeLocation;
  }

  /**
   * Gets the preference component.
   *
   * @return the preference component
   */
  public Class<? extends UIContentDialogPreference> getPreferenceComponent() {
    return preferenceComponent;
  }

  /**
   * Sets the preference component.
   *
   * @param preferenceComponent the new preference component
   */
  public void setPreferenceComponent(Class<? extends UIContentDialogPreference> preferenceComponent) {
    this.preferenceComponent = preferenceComponent;
  }

  /**
   * Instantiates a new uI content dialog form.
   *
   * @throws Exception the exception
   */
  public UIContentDialogForm() throws Exception {
    setActions(new String [] {"SaveDraft", "FastPublish", "Preferences", "Close"});
  }

  /**
   * Inits the.
   *
   * @param webcontent the webcontent
   * @param isAddNew the is add new
   *
   * @throws Exception the exception
   */
  public void init(Node webcontent, boolean isAddNew) throws Exception {
    NodeLocation webcontentNodeLocation = null;
    if(webcontent.isNodeType("exo:symlink")) {
      LinkManager linkManager = getApplicationComponent(LinkManager.class);
      Node realNode = linkManager.getTarget(webcontent);
      webcontentNodeLocation = NodeLocation.getNodeLocationByNode(realNode);
      this.contentType = realNode.getPrimaryNodeType().getName();
      this.nodePath = realNode.getPath();
      setStoredPath(getParentPath(realNode));
    } else {
      webcontentNodeLocation = NodeLocation.getNodeLocationByNode(webcontent);
      this.contentType = webcontent.getPrimaryNodeType().getName();
      this.nodePath = webcontent.getPath();
      setStoredPath(getParentPath(webcontent));
    }
    this.webcontentNodeLocation = webcontentNodeLocation;
    this.repositoryName = webcontentNodeLocation.getRepository();
    this.workspaceName = webcontentNodeLocation.getWorkspace();
    this.isAddNew = isAddNew;
    resetProperties();
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    String userName = Util.getPortalRequestContext().getRemoteUser();
    this.template = templateService.getTemplatePathByUser(true, contentType, userName);
    initFieldInput();
  }

  private String getParentPath(Node node) throws RepositoryException {
    return node.getPath().substring(0, node.getPath().lastIndexOf('/'));
  }

  /**
   * Inits the field input.
   *
   * @throws Exception the exception
   */
  private void initFieldInput() throws Exception {
    TemplateService tservice = this.getApplicationComponent(TemplateService.class);
    List<String> documentNodeType = tservice.getDocumentTemplates();
    if(!documentNodeType.contains(this.contentType)){
      return;
    }
    if (!isAddNew) {
      TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
      Node currentNode = getCurrentNode();
      List<Node> listCategories = taxonomyService.getAllCategories(currentNode);
      for (Node itemNode : listCategories) {
        String categoryPath = itemNode.getPath().replaceAll(getPathTaxonomy() + "/", "");
        if (!listTaxonomy.contains(categoryPath)) {
          listTaxonomy.add(categoryPath);
          listTaxonomyName.add(categoryPath);
        }
      }
    }
    if(listTaxonomyName == null || listTaxonomyName.size() == 0) return;
    UIFormMultiValueInputSet uiFormMultiValue = createUIComponent(UIFormMultiValueInputSet.class, null, null);
    uiFormMultiValue.setId(FIELD_TAXONOMY);
    uiFormMultiValue.setName(FIELD_TAXONOMY);
    uiFormMultiValue.setType(UIFormStringInput.class);
    uiFormMultiValue.setValue(listTaxonomyName);
    addUIFormInput(uiFormMultiValue);
  }

  /**
   * Gets the current node.
   *
   * @return the current node
   */
  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(webcontentNodeLocation);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.form.UIDialogForm#getTemplate()
   */
  public String getTemplate() {
    return template;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
    return new JCRResourceResolver(workspace);
  }

  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener</code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class CloseActionListener extends EventListener<UIContentDialogForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentDialogForm> event) throws Exception {
      UIContentDialogForm contentDialogForm = event.getSource();
      if (Util.getUIPortalApplication().getModeState() == UIPortalApplication.NORMAL_MODE)
        ((PortletRequestContext)event.getRequestContext()).setApplicationMode(PortletMode.VIEW);
      Utils.closePopupWindow(contentDialogForm, CONTENT_DIALOG_FORM_POPUP_WINDOW);
    }
  }

  /**
   * The listener interface for receiving preferencesAction events.
   * The class that is interested in processing a preferencesAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPreferencesActionListener</code> method. When
   * the PreferencesAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class PreferencesActionListener extends EventListener<UIContentDialogForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentDialogForm> event) throws Exception {
      UIContentDialogForm contentDialogForm = event.getSource();
      UIPopupContainer popupContainer = Utils.getPopupContainer(contentDialogForm);
      popupContainer.addChild(contentDialogForm);
      contentDialogForm.setParent(popupContainer);
      UIContentDialogPreference contentDialogPreference = null;
      if (contentDialogForm.getPreferenceComponent() != null)
        contentDialogPreference = contentDialogForm.createUIComponent(contentDialogForm.getPreferenceComponent(),
                                                                      null,
                                                                      null);
      else
        contentDialogPreference = contentDialogForm.createUIComponent(UIContentDialogPreference.class,
                                                                      null,
                                                                      null);

      Utils.updatePopupWindow(contentDialogForm, contentDialogPreference, CONTENT_DIALOG_FORM_POPUP_WINDOW);
      contentDialogPreference.init();
    }
  }

  /**
   * The listener interface for receiving saveAction events.
   * The class that is interested in processing a saveAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSaveDraftActionListener</code> method. When
   * the saveAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SaveDraftActionListener extends EventListener<UIContentDialogForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentDialogForm> event) throws Exception {
      UIContentDialogForm contentDialogForm = event.getSource();
      try {
        Node webContentNode = contentDialogForm.getNode();
        if (!webContentNode.isCheckedOut()) {
          webContentNode.checkout();
        }
        List<UIComponent> inputs = contentDialogForm.getChildren();
        if (contentDialogForm.checkCategories(contentDialogForm)) {
          Utils.createPopupMessage(contentDialogForm,
                                   "UIContentDialogForm.msg.non-categories",
                                   null,
                                   ApplicationMessage.WARNING);
          return;
        }
        Map<String, JcrInputProperty> inputProperties = DialogFormUtil.prepareMap(inputs,
                                                                                  contentDialogForm.getInputProperties(),
                                                                                  contentDialogForm.getInputOptions());
        CmsService cmsService = contentDialogForm.getApplicationComponent(CmsService.class);
        if (WCMCoreUtils.canAccessParentNode(webContentNode)) {
          cmsService.storeNode(contentDialogForm.contentType,
                               webContentNode.getParent(),
                               inputProperties,
                               contentDialogForm.isAddNew);
        } else {
          cmsService.storeEditedNode(contentDialogForm.contentType,
                                     webContentNode,
                                     inputProperties,
                                     contentDialogForm.isAddNew);
        }

        if (Util.getUIPortalApplication().getModeState() == UIPortalApplication.NORMAL_MODE) {
          ((PortletRequestContext) event.getRequestContext()).setApplicationMode(PortletMode.VIEW);
        }
        Utils.closePopupWindow(contentDialogForm, CONTENT_DIALOG_FORM_POPUP_WINDOW);

      } catch(LockException le) {
        Object[] args = {contentDialogForm.getNode().getPath()};
        Utils.createPopupMessage(contentDialogForm, "UIContentDialogForm.msg.node-locked", args, ApplicationMessage.WARNING);
      } catch(AccessControlException ace) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(ace.getMessage());
        }
      } catch (AccessDeniedException ade) {
        Utils.createPopupMessage(contentDialogForm,
                                 "UIDocumentInfo.msg.access-denied-exception",
                                 null,
                                 ApplicationMessage.WARNING);
      } catch(VersionException ve) {
        Utils.createPopupMessage(contentDialogForm, "UIDocumentForm.msg.in-versioning", null, ApplicationMessage.WARNING);
      } catch(ItemNotFoundException item) {
        Utils.createPopupMessage(contentDialogForm, "UIDocumentForm.msg.item-not-found", null, ApplicationMessage.WARNING);
      } catch(RepositoryException repo) {
        String key = "UIDocumentForm.msg.repository-exception";
        if (ItemExistsException.class.isInstance(repo)) key = "UIDocumentForm.msg.not-allowed-same-name-sibling";
        Utils.createPopupMessage(contentDialogForm, key, null, ApplicationMessage.WARNING);
      } catch (NumberFormatException nfe) {
        Utils.createPopupMessage(contentDialogForm,
                                 "UIDocumentForm.msg.numberformat-exception",
                                 null,
                                 ApplicationMessage.WARNING);
      } catch (Exception e) {
        Utils.createPopupMessage(contentDialogForm,
                                 "UIDocumentForm.msg.cannot-save",
                                 null,
                                 ApplicationMessage.WARNING);
      }
    }

  }

  /**
   * Check categories.
   *
   * @param contentDialogForm the content dialog form
   *
   * @return true, if successful
   */
  private boolean checkCategories(UIContentDialogForm contentDialogForm) {
    String[] categoriesPathList = null;
    int index = 0;
    if (contentDialogForm.isReference) {
      UIFormMultiValueInputSet uiSet = contentDialogForm.getChild(UIFormMultiValueInputSet.class);
      if ((uiSet != null) && (uiSet.getName() != null) && uiSet.getName().equals(FIELD_TAXONOMY)) {
        List<UIComponent> listChildren = uiSet.getChildren();
        StringBuffer sb = new StringBuffer();
        for (UIComponent component : listChildren) {
          UIFormStringInput uiStringInput = (UIFormStringInput) component;
          if (uiStringInput.getValue() != null) {
            String value = uiStringInput.getValue().trim();
            sb.append(value).append(",");
          }
        }
        String categoriesPath = sb.toString();
        if (categoriesPath != null && categoriesPath.length() > 0) {
          try {
            if (categoriesPath.endsWith(",")) {
              categoriesPath = categoriesPath.substring(0, categoriesPath.length() - 1).trim();
              if (categoriesPath.trim().length() == 0) {
                return true;
              }
            }
            categoriesPathList = categoriesPath.split(",");
            if ((categoriesPathList == null) || (categoriesPathList.length == 0)) {
              return true;
            }
            for (String categoryPath : categoriesPathList) {
              index = categoryPath.indexOf("/");
              if (index < 0) {
                return true;
              }
            }
          } catch (Exception e) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * The listener interface for receiving fastPublishAction events.
   * The class that is interested in processing a cancelAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addFastPublishActionListener</code> method. When
   * the cancelAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class FastPublishActionListener extends EventListener<UIContentDialogForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentDialogForm> event) throws Exception {
      UIContentDialogForm contentDialogForm = event.getSource();
      try{
        Node webContentNode = contentDialogForm.getNode();
        if (!webContentNode.isCheckedOut()) {
          webContentNode.checkout();
        }
        List<UIComponent> inputs = contentDialogForm.getChildren();
        if (contentDialogForm.checkCategories(contentDialogForm)) {
          Utils.createPopupMessage(contentDialogForm,
                                   "UIContentDialogForm.msg.non-categories",
                                   null,
                                   ApplicationMessage.WARNING);
          return;
        }
        Map<String, JcrInputProperty> inputProperties = DialogFormUtil.prepareMap(inputs,
                                                                                  contentDialogForm.getInputProperties(),
                                                                                  contentDialogForm.getInputOptions());
        CmsService cmsService = contentDialogForm.getApplicationComponent(CmsService.class);
        cmsService.storeNode(contentDialogForm.contentType,
                             contentDialogForm.getNode().getParent(),
                             inputProperties,
                             contentDialogForm.isAddNew);

        PublicationService publicationService = contentDialogForm.getApplicationComponent(PublicationService.class);
        PublicationPlugin publicationPlugin = publicationService.getPublicationPlugins()
                                                                .get(publicationService.getNodeLifecycleName(webContentNode));
        HashMap<String, String> context = new HashMap<String, String>();
        if(webContentNode != null) {
          context.put("Publication.context.currentVersion", webContentNode.getName());
        }
        publicationPlugin.changeState(webContentNode, PublicationDefaultStates.PUBLISHED, context);

        if (Util.getUIPortalApplication().getModeState() == UIPortalApplication.NORMAL_MODE) {
            ((PortletRequestContext)event.getRequestContext()).setApplicationMode(PortletMode.VIEW);
        }
        Utils.closePopupWindow(contentDialogForm, CONTENT_DIALOG_FORM_POPUP_WINDOW);

      } catch(LockException le) {
        Object[] args = {contentDialogForm.getNode().getPath()};
        Utils.createPopupMessage(contentDialogForm,
                                 "UIContentDialogForm.msg.node-locked",
                                 args,
                                 ApplicationMessage.WARNING);
      } catch (AccessControlException ace) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(ace.getMessage());
        }
      } catch (AccessDeniedException ade) {
        Utils.createPopupMessage(contentDialogForm,
                                 "UIDocumentInfo.msg.access-denied-exception",
                                 null,
                                 ApplicationMessage.WARNING);
      } catch (VersionException ve) {
        Utils.createPopupMessage(contentDialogForm,
                                 "UIDocumentForm.msg.in-versioning",
                                 null,
                                 ApplicationMessage.WARNING);
      } catch (ItemNotFoundException item) {
        Utils.createPopupMessage(contentDialogForm,
                                 "UIDocumentForm.msg.item-not-found",
                                 null,
                                 ApplicationMessage.WARNING);
      } catch (RepositoryException repo) {
        String key = "UIDocumentForm.msg.repository-exception";
        if (ItemExistsException.class.isInstance(repo))
          key = "UIDocumentForm.msg.not-allowed-same-name-sibling";
        Utils.createPopupMessage(contentDialogForm, key, null, ApplicationMessage.WARNING);
      } catch (NumberFormatException nfe) {
        Utils.createPopupMessage(contentDialogForm,
                                 "UIDocumentForm.msg.numberformat-exception",
                                 null,
                                 ApplicationMessage.WARNING);
      } catch (Exception e) {
        Utils.createPopupMessage(contentDialogForm,
                                 "UIDocumentForm.msg.cannot-save",
                                 null,
                                 ApplicationMessage.WARNING);
      }
    }
  }

  /**
   * The listener interface for receiving addAction events. The class that is
   * interested in processing a addAction event implements this interface, and
   * the object created with that class is registered with a component using the
   * component's <code>addAddActionListener</code> method. When
   * the addAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class AddActionListener extends EventListener<UIContentDialogForm> {

    /*
     * (non-Javadoc)
     * @see
     * org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui
     * .event.Event)
     */
    public void execute(Event<UIContentDialogForm> event) throws Exception {
      UIContentDialogForm contentDialogForm = event.getSource();
      String clickedField = event.getRequestContext().getRequestParameter(OBJECTID);
      if (contentDialogForm.isReference) {
        UIApplication uiApp = contentDialogForm.getAncestorOfType(UIApplication.class);
        try {
          UIFormMultiValueInputSet uiSet = contentDialogForm.getChildById(FIELD_TAXONOMY);
          if ((uiSet != null) && (uiSet.getName() != null)
              && uiSet.getName().equals(FIELD_TAXONOMY)) {
            if ((clickedField != null) && (clickedField.equals(FIELD_TAXONOMY))) {
              NodeHierarchyCreator nodeHierarchyCreator = contentDialogForm.getApplicationComponent(NodeHierarchyCreator.class);
              String repository = contentDialogForm.repositoryName;
              DMSConfiguration dmsConfiguration = contentDialogForm.getApplicationComponent(DMSConfiguration.class);
              DMSRepositoryConfiguration repositoryConfiguration = dmsConfiguration.getConfig();
              String workspaceName = repositoryConfiguration.getSystemWorkspace();
              UIOneTaxonomySelector uiOneTaxonomySelector = contentDialogForm.createUIComponent(UIOneTaxonomySelector.class,
                                                                                                null,
                                                                                                null);
              if (uiSet.getValue().size() == 0)
                uiSet.setValue(new ArrayList<Value>());
              String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
              RepositoryService repositoryService = (RepositoryService) contentDialogForm.
                  getApplicationComponent(RepositoryService.class);
              ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
              Session session = WCMCoreUtils.getUserSessionProvider()
                                            .getSession(workspaceName, manageableRepository);
              Node rootTree = (Node) session.getItem(rootTreePath);
              NodeIterator childrenIterator = rootTree.getNodes();
              while (childrenIterator.hasNext()) {
                Node childNode = childrenIterator.nextNode();
                rootTreePath = childNode.getPath();
                break;
              }
              uiOneTaxonomySelector.setRootNodeLocation(repository, workspaceName, rootTreePath);
              uiOneTaxonomySelector.setExceptedNodeTypesInPathPanel(new String[] { "exo:symlink" });
              uiOneTaxonomySelector.init(WCMCoreUtils.getUserSessionProvider());
              String param = "returnField=" + FIELD_TAXONOMY;
              uiOneTaxonomySelector.setSourceComponent(contentDialogForm, new String[] { param });
              Utils.createPopupWindow(contentDialogForm,
                                      uiOneTaxonomySelector,
                                      TAXONOMY_CONTENT_POPUP_WINDOW,
                                      700);
            }
          }
        } catch (AccessDeniedException accessDeniedException) {
          uiApp.addMessage(new ApplicationMessage("UIContentDialogForm.msg.access-denied",
                                                  null,
                                                  ApplicationMessage.WARNING));
          return;
        } catch (Exception e) {
          uiApp.addMessage(new ApplicationMessage("UIContentDialogForm.msg.exception",
                                                  null,
                                                  ApplicationMessage.WARNING));
          return;
        }
      } else {
        event.getRequestContext().addUIComponentToUpdateByAjax(contentDialogForm);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPopupComponent#activate()
   */
  public void activate() {
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPopupComponent#deActivate()
   */
  public void deActivate() {
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public void doSelect(String selectField, Object value) throws Exception {
    isUpdateSelect = true;
    UIFormInput formInput = getUIInput(selectField);
    if(formInput instanceof UIFormInputBase) {
      ((UIFormInputBase)formInput).setValue(value.toString());
    }else if(formInput instanceof UIFormMultiValueInputSet) {
      UIFormMultiValueInputSet  inputSet = (UIFormMultiValueInputSet) formInput;
      String valueTaxonomy = String.valueOf(value).trim();
      List taxonomylist = inputSet.getValue();
      if (!taxonomylist.contains(valueTaxonomy)) {
        listTaxonomy.add(valueTaxonomy);
        listTaxonomyName.add(valueTaxonomy);
        taxonomylist.add(valueTaxonomy);
      }
      inputSet.setValue(taxonomylist);
    }
  }

  /**
   * The listener interface for receiving removeReferenceAction events.
   * The class that is interested in processing a removeReferenceAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRemoveReferenceActionListener</code> method. When
   * the removeReferenceAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class RemoveReferenceActionListener extends EventListener<UIContentDialogForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentDialogForm> event) throws Exception {
      UIContentDialogForm contentDialogForm = event.getSource();
      contentDialogForm.isRemovePreference = true;
      String fieldName = event.getRequestContext().getRequestParameter(OBJECTID);
      contentDialogForm.getUIStringInput(fieldName).setValue(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(contentDialogForm);
    }
  }
}
