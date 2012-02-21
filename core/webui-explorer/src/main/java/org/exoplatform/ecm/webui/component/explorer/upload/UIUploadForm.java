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
package org.exoplatform.ecm.webui.component.explorer.upload;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIMultiLanguageForm;
import org.exoplatform.ecm.webui.component.explorer.popup.actions.UIMultiLanguageManager;
import org.exoplatform.ecm.webui.form.validator.IllegalDMSCharValidator;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.documents.DocumentTypeService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormUploadInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

/**
 * Created by The eXo Platform SARL
 * Author : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * July 3, 2006
 * 10:07:15 AM
 */

@ComponentConfigs(
    {
      @ComponentConfig(
          lifecycle = UIFormLifecycle.class,
          template = "app:/groovy/webui/component/explorer/upload/UIUploadForm.gtmpl",
          events = {
            @EventConfig(listeners = UIUploadForm.SaveActionListener.class),
            @EventConfig(listeners = UIUploadForm.CancelActionListener.class, phase = Phase.DECODE),
            @EventConfig(listeners = UIUploadForm.AddUploadActionListener.class, phase = Phase.DECODE),
            @EventConfig(listeners = UIUploadForm.RemoveUploadActionListener.class, phase = Phase.DECODE)
          }
      ),
      @ComponentConfig(
          type = UIFormMultiValueInputSet.class,
          id="UploadMultipleInputset",
          events = {
            @EventConfig(listeners = UIUploadForm.RemoveActionListener.class, phase = Phase.DECODE),
            @EventConfig(listeners = UIUploadForm.AddActionListener.class, phase = Phase.DECODE)
          }
      )
    }
)

public class UIUploadForm extends UIForm implements UIPopupComponent, UISelectable {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("explorer.upload.UIUploadForm");

  final static public String FIELD_NAME =  "name" ;
  final static public String FIELD_UPLOAD = "upload" ;
  final static public String JCRCONTENT = "jcr:content";
  final static public String FIELD_TAXONOMY = "fieldTaxonomy";
  final static public String FIELD_LISTTAXONOMY = "fieldListTaxonomy";
  final static public String POPUP_TAXONOMY = "UIPopupTaxonomy";
  final static public String ACCESSIBLE_MEDIA = "accessibleMedia";

  private boolean isMultiLanguage_;
  private String language_;
  private boolean isDefault_;
  private List<String> listTaxonomy = new ArrayList<String>();
  private List<String> listTaxonomyName = new ArrayList<String>();

  private int numberUploadFile = 1;
  private HashMap<String, List<String>> mapTaxonomies = new HashMap<String, List<String>>();
  private List<NodeLocation> listUploadedNodes = new ArrayList<NodeLocation>();
  private boolean taxonomyMandatory = false;
  
  private DocumentTypeService docService;

  public UIUploadForm() throws Exception {
    setMultiPart(true) ;
    addUIFormInput(new UIFormStringInput(FIELD_NAME, FIELD_NAME, null).
                              addValidator(IllegalDMSCharValidator.class));
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String limitPref = portletPref.getValue(Utils.UPLOAD_SIZE_LIMIT_MB, "");
    UIFormUploadInput uiInput = null;
    if (limitPref != null) {
      try {
        uiInput = new UIFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD, Integer.parseInt(limitPref.trim()));
      } catch (NumberFormatException e) {
        uiInput = new UIFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD);
      }
    } else {
      uiInput = new UIFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD);
    }
    uiInput.setAutoUpload(true);
    addUIFormInput(uiInput);
    docService = WCMCoreUtils.getService(DocumentTypeService.class);
  }

  public int getNumberUploadFile() {
    return numberUploadFile;
  }

  public void setNumberUploadFile(int numberUpload) {
    numberUploadFile = numberUpload;
  }

  public HashMap<String, List<String>> getMapTaxonomies() {
    return mapTaxonomies;
  }

  public void setMapTaxonomies(HashMap<String, List<String>> mapTaxonomiesAvaiable) {
    mapTaxonomies = mapTaxonomiesAvaiable;
  }

  public List<String> getListTaxonomy() {
    return listTaxonomy;
  }

  public List<String> getlistTaxonomyName() {
    return listTaxonomyName;
  }

  public void setListTaxonomy(List<String> listTaxonomyNew) {
    listTaxonomy = listTaxonomyNew;
  }

  public void setListTaxonomyName(List<String> listTaxonomyNameNew) {
    listTaxonomyName = listTaxonomyNameNew;
  }

  public boolean getTaxonomyMandatory() {
    return taxonomyMandatory;
  }

  public void setTaxonomyMandatory(boolean taxoMandatory) {
    taxonomyMandatory = taxoMandatory;
  }

  public String getPathTaxonomy() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    DMSConfiguration dmsConfig = getApplicationComponent(DMSConfiguration.class);
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfig.getConfig();
    String workspaceName = dmsRepoConfig.getSystemWorkspace();
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    Session session = uiExplorer.getSessionByWorkspace(workspaceName);
    return ((Node)session.getItem(nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH))).getPath();
  }

  public void initFieldInput() throws Exception {
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String categoryMandatoryWhenFileUpload =  portletPref.getValue(Utils.CATEGORY_MANDATORY, "").trim();
    UIFormMultiValueInputSet uiFormMultiValue = createUIComponent(UIFormMultiValueInputSet.class,
                                                                  "UploadMultipleInputset",
                                                                  null);
    uiFormMultiValue.setId(FIELD_LISTTAXONOMY);
    uiFormMultiValue.setName(FIELD_LISTTAXONOMY);
    uiFormMultiValue.setType(UIFormStringInput.class);
    uiFormMultiValue.setEditable(false);
    if (categoryMandatoryWhenFileUpload.equalsIgnoreCase("true")) {
      uiFormMultiValue.addValidator(MandatoryValidator.class);
      setTaxonomyMandatory(true);
    } else {
      setTaxonomyMandatory(false);
    }
    uiFormMultiValue.setValue(listTaxonomyName);
    addUIFormInput(uiFormMultiValue);
  }

  public String[] getActions() {
    return new String[] {"Save", "Cancel"};
  }

  public void setIsMultiLanguage(boolean isMultiLanguage, String language) {
    isMultiLanguage_ = isMultiLanguage ;
    language_ = language ;
  }

  public void resetComponent() {
    removeChildById(FIELD_UPLOAD);
    addUIFormInput(new UIFormUploadInput(FIELD_UPLOAD, FIELD_UPLOAD));
  }

  public boolean isMultiLanguage() { return isMultiLanguage_ ; }

  public void setIsDefaultLanguage(boolean isDefault) { isDefault_ = isDefault ; }

  private String getLanguageSelected() { return language_ ; }

  public void activate() throws Exception {}
  public void deActivate() throws Exception {}

  public void doSelect(String selectField, Object value) throws Exception {
    String valueTaxonomy = String.valueOf(value).trim();
    List<String> indexMapTaxonomy = new ArrayList<String>();
    if (mapTaxonomies.containsKey(selectField)){
      indexMapTaxonomy = mapTaxonomies.get(selectField);
      mapTaxonomies.remove(selectField);
    }
    if (!indexMapTaxonomy.contains(valueTaxonomy)) indexMapTaxonomy.add(valueTaxonomy);
    mapTaxonomies.put(selectField, indexMapTaxonomy);

    updateAdvanceTaxonomy(selectField);
    UIUploadManager uiUploadManager = getParent();
    uiUploadManager.removeChildById(POPUP_TAXONOMY);
  }

  public List<String> getListSameNames(Event<UIUploadForm> event) throws Exception {
    List<String> sameNameList = new ArrayList<String>();
    Node selectedNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
    int index = 0;
    String name = null;
    for (UIComponent uiComp : getChildren()) {
      if(uiComp instanceof UIFormUploadInput) {
        String[] arrayId = uiComp.getId().split(FIELD_UPLOAD);
        if ((arrayId.length > 0) && (arrayId[0].length() > 0)) index = new Integer(arrayId[0]).intValue();
        UIFormUploadInput uiFormUploadInput;
        if (index == 0){
          uiFormUploadInput = (UIFormUploadInput)getUIInput(FIELD_UPLOAD);
        } else {
          uiFormUploadInput = (UIFormUploadInput)getUIInput(index + FIELD_UPLOAD);
        }
        if (uiFormUploadInput.getUploadResource() == null) return sameNameList;
        String fileName = uiFormUploadInput.getUploadResource().getFileName();
        if (index == 0){
          name = getUIStringInput(FIELD_NAME).getValue();
        } else {
          name = getUIStringInput(index + FIELD_NAME).getValue();
        }
        if(name == null) {
          name = fileName;
        } else {
          name = name.trim();
        }
        name = Text.escapeIllegalJcrChars(name);
        if (!passNameValidation(name)) {
          return new ArrayList<String>();
        }
        if(selectedNode.hasNode(name)) sameNameList.add(name);
      }
    }
    return sameNameList;
  }

  @SuppressWarnings("rawtypes")
  public void doUpload(Event event, boolean isKeepFile) throws Exception {
    UIApplication uiApp = getAncestorOfType(UIApplication.class) ;
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;

    UIUploadManager uiManager = getParent();
    UIUploadContainer uiUploadContainer = uiManager.getChild(UIUploadContainer.class);
    UploadService uploadService = getApplicationComponent(UploadService.class);
    UIUploadContent uiUploadContent = uiManager.findFirstComponentOfType(UIUploadContent.class);
    List<String[]> listArrValues = new ArrayList<String[]>();
    CmsService cmsService = getApplicationComponent(CmsService.class) ;
    List<UIComponent> listFormChildren = getChildren();
    int index = 0;
    InputStream inputStream;
    String name = null;
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    MultiLanguageService multiLangService = getApplicationComponent(MultiLanguageService.class) ;
    if(uiExplorer.getCurrentNode().isLocked()) {
      String lockToken = LockUtil.getLockToken(uiExplorer.getCurrentNode());
      if(lockToken != null) uiExplorer.getSession().addLockToken(lockToken);
    }
    PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String categoryMandatoryWhenFileUpload =  portletPref.getValue(Utils.CATEGORY_MANDATORY, "").trim();
    DMSMimeTypeResolver mimeTypeSolver = DMSMimeTypeResolver.getInstance();
    Node selectedNode = uiExplorer.getCurrentNode();
    if (categoryMandatoryWhenFileUpload.equalsIgnoreCase("true") &&
        (getMapTaxonomies().size() == 0) && !uiExplorer.getCurrentNode().hasNode(JCRCONTENT)) {
      uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.taxonomyPath-error", null,
          ApplicationMessage.WARNING)) ;

      return ;
    }
    String pers = PermissionType.ADD_NODE + "," + PermissionType.SET_PROPERTY ;
    selectedNode.getSession().checkPermission(selectedNode.getPath(), pers);
    try {
      int indexValidate = 0;
      for (UIComponent uiCompValidate : listFormChildren) {
        if(uiCompValidate instanceof UIFormUploadInput) {
          String[] arrayIdValidate = uiCompValidate.getId().split(FIELD_UPLOAD);
          if ((arrayIdValidate.length > 0) && (arrayIdValidate[0].length() > 0))
            indexValidate = new Integer(arrayIdValidate[0]).intValue();
          UIFormUploadInput uiFormUploadInput;
          if (indexValidate == 0){
            uiFormUploadInput = (UIFormUploadInput) getUIInput(FIELD_UPLOAD);
          } else {
            uiFormUploadInput = (UIFormUploadInput) getUIInput(indexValidate + FIELD_UPLOAD);
          }
          if(uiFormUploadInput.getUploadResource() == null) {
            uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-error", null,
                                                    ApplicationMessage.WARNING)) ;

            return ;
          }
          String fileName = uiFormUploadInput.getUploadResource().getFileName();
          if(fileName == null || fileName.length() == 0) {
            uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-error", null,
                                                    ApplicationMessage.WARNING)) ;

            return;
          }
        }
      }
      for (UIComponent uiComp : listFormChildren) {
        if(uiComp instanceof UIFormUploadInput) {
          String[] arrayId = uiComp.getId().split(FIELD_UPLOAD);
          if ((arrayId.length > 0) && (arrayId[0].length() > 0)) index = new Integer(arrayId[0]).intValue();
          UIFormUploadInput uiFormUploadInput;
          if (index == 0){
            uiFormUploadInput = (UIFormUploadInput) getUIInput(FIELD_UPLOAD);
          } else {
            uiFormUploadInput = (UIFormUploadInput) getUIInput(index + FIELD_UPLOAD);
          }
          if(uiFormUploadInput.getUploadResource() == null) {
            if ((listUploadedNodes != null) && (listUploadedNodes.size() > 0)) {
              for (Object uploadedNode : NodeLocation.getNodeListByLocationList(listUploadedNodes)) {
                ((Node)uploadedNode).remove();
              }
              uiExplorer.getCurrentNode().save();
              listUploadedNodes.clear();
            }
            uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-error", null,
                                                    ApplicationMessage.WARNING)) ;

            return ;
          }

          String fileName = uiFormUploadInput.getUploadResource().getFileName();

          if(fileName == null || fileName.length() == 0) {
            if ((listUploadedNodes != null) && (listUploadedNodes.size() > 0)) {
              for (Object uploadedNode : NodeLocation.getNodeListByLocationList(listUploadedNodes)) {
                ((Node)uploadedNode).remove();
              }
              uiExplorer.getCurrentNode().save();
              listUploadedNodes.clear();
            }
            uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-error", null,
                                                    ApplicationMessage.WARNING)) ;

            return;
          }
          try {
            inputStream = new BufferedInputStream(uiFormUploadInput.getUploadDataAsStream());
          } catch (FileNotFoundException e) {
            inputStream = new BufferedInputStream(new ByteArrayInputStream(new byte[] {}));
          }
          if (index == 0){
            name = getUIStringInput(FIELD_NAME).getValue();
          } else {
            name = getUIStringInput(index + FIELD_NAME).getValue();
          }

          if(name == null) name = fileName;
          else name = name.trim();

          if (!passNameValidation(name)) {
            uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-invalid-with-name",
                new Object[] {name}, ApplicationMessage.WARNING));

            return;
          }

          name = Text.escapeIllegalJcrChars(name);

          // Append extension if necessary
          String mimeType = mimeTypeSolver.getMimeType(fileName);
          String ext = "." + fileName.substring(fileName.lastIndexOf(".") + 1);
          if (name.lastIndexOf(ext) < 0 && !mimeTypeSolver.getMimeType(name).equals(mimeType)) {
            StringBuffer sb = new StringBuffer();
            sb.append(name).append(ext);
            name = sb.toString();
          }

          List<String> listTaxonomyNameNew = new ArrayList<String>();
          if (index == 0) listTaxonomyNameNew = mapTaxonomies.get(FIELD_LISTTAXONOMY);
          else listTaxonomyNameNew = mapTaxonomies.get(index + FIELD_LISTTAXONOMY);
          String taxonomyTree = null;
          String taxonomyPath = null;
          if (listTaxonomyNameNew != null) {
            for(String categoryPath : listTaxonomyNameNew) {
              try {
                if (categoryPath.startsWith("/")) categoryPath = categoryPath.substring(1);
                if(categoryPath.indexOf("/")>0)
                {
                  taxonomyTree = categoryPath.substring(0, categoryPath.indexOf("/"));
                  taxonomyPath = categoryPath.substring(categoryPath.indexOf("/") + 1);
                  taxonomyService.getTaxonomyTree(taxonomyTree).hasNode(taxonomyPath);
                }
                else
                {
                  taxonomyTree = categoryPath;
                  taxonomyPath = "";
                }
              } catch (ItemNotFoundException e) {
                uiApp.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.non-categories", null,
                    ApplicationMessage.WARNING)) ;

                return;
              } catch (RepositoryException re) {
                uiApp.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.non-categories", null,
                    ApplicationMessage.WARNING)) ;

                return;
              } catch(Exception e) {
                if (LOG.isErrorEnabled()) {
                  LOG.error("An unexpected error occurs", e);
                }
                uiApp.addMessage(new ApplicationMessage("UISelectedCategoriesGrid.msg.non-categories", null,
                    ApplicationMessage.WARNING)) ;

                return;
              }
            }
          }
          boolean isExist = selectedNode.hasNode(name) ;
          String newNodeUUID = null;

          if(isMultiLanguage()) {
            ValueFactoryImpl valueFactory = (ValueFactoryImpl) uiExplorer.getSession().getValueFactory() ;
            Value contentValue = valueFactory.createValue(inputStream) ;
            multiLangService.addFileLanguage(selectedNode, name, contentValue, mimeType,
                getLanguageSelected(), uiExplorer.getRepositoryName(), isDefault_) ;
            uiExplorer.setIsHidePopup(true) ;
            UIMultiLanguageManager uiLanguageManager = getAncestorOfType(UIMultiLanguageManager.class) ;
            UIMultiLanguageForm uiMultiForm = uiLanguageManager.getChild(UIMultiLanguageForm.class) ;
            uiMultiForm.doSelect(uiExplorer.getCurrentNode()) ;
            event.getRequestContext().addUIComponentToUpdateByAjax(uiLanguageManager);
          } else {
            if(selectedNode.getPrimaryNodeType().isNodeType(Utils.NT_FILE)) {
              if(!selectedNode.isCheckedOut()) selectedNode.checkout() ;
              Node contentNode = selectedNode.getNode(Utils.JCR_CONTENT);
              if(contentNode.getProperty(Utils.JCR_MIMETYPE).getString().equals(mimeType)) {
                contentNode.setProperty(Utils.JCR_DATA, inputStream);
                contentNode.setProperty(Utils.JCR_LASTMODIFIED, new GregorianCalendar());
                selectedNode.save() ;
                uiManager.setRendered(false);
                uiExplorer.updateAjax(event);
                return;
              }
            }
            if(!isExist || isKeepFile) {
              String nodeType = contains(docService.getMimeTypes(ACCESSIBLE_MEDIA), mimeType) ? 
                                NodetypeConstant.EXO_ACCESSIBLE_MEDIA : Utils.NT_FILE; 
              newNodeUUID = cmsService.storeNodeByUUID(nodeType, selectedNode,
                  getInputProperties(name, inputStream, mimeType), true) ;
              selectedNode.save();
              selectedNode.getSession().save();
              if ((listTaxonomyNameNew != null) && (listTaxonomyNameNew.size() > 0)) {
                Node newNode = null;
                try {
                  newNode = selectedNode.getSession().getNodeByUUID(newNodeUUID);
                } catch(ItemNotFoundException e) {
                  newNode = Utils.findNodeByUUID(newNodeUUID);
                }
                if (newNode != null) {
                  for (String categoryPath : listTaxonomyNameNew) {
                    try {
                      if (categoryPath.startsWith("/")) categoryPath = categoryPath.substring(1);
                      if(categoryPath.indexOf("/")>0)
                      {
                        taxonomyTree = categoryPath.substring(0, categoryPath.indexOf("/"));
                        taxonomyPath = categoryPath.substring(categoryPath.indexOf("/") + 1);
                      }
                      else
                      {
                        taxonomyTree = categoryPath;
                        taxonomyPath = "";
                      }
                      taxonomyService.addCategory(newNode, taxonomyTree, taxonomyPath);
                    } catch (ItemExistsException e) {
                      uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.ItemExistsException",
                          null, ApplicationMessage.WARNING));
                      return;
                    } catch (RepositoryException e) {
                      if (LOG.isErrorEnabled()) {
                        LOG.error("Unexpected error", e);
                      }
                      JCRExceptionManager.process(uiApp, e);
                      return;
                    }
                  }
                }
              }
            } else {
              Node node = selectedNode.getNode(name) ;
              if (isTaxonomyChildNode(node)) {
                LinkManager linkManager = getApplicationComponent(LinkManager.class);
                node = linkManager.getTarget(node);
              }
              if(!node.getPrimaryNodeType().isNodeType(Utils.NT_FILE)) {
                Object[] args = { name } ;
                uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.name-is-exist", args,
                                                        ApplicationMessage.WARNING)) ;

                return ;
              }
              if(!node.isNodeType(Utils.MIX_VERSIONABLE) && node.canAddMixin(Utils.MIX_VERSIONABLE)) {
                node.addMixin(Utils.MIX_VERSIONABLE);
              }
              Node contentNode = node.getNode(Utils.JCR_CONTENT);
              if(!node.isCheckedOut()) node.checkout() ;
              contentNode.setProperty(Utils.JCR_DATA, inputStream);
              contentNode.setProperty(Utils.JCR_MIMETYPE, mimeType);
              contentNode.setProperty(Utils.JCR_LASTMODIFIED, new GregorianCalendar());
              if (node.isNodeType("exo:datetime")) {
                node.setProperty("exo:dateModified",new GregorianCalendar()) ;
              }
              node.save();
              ListenerService listenerService = getApplicationComponent(ListenerService.class);
              listenerService.broadcast(CmsService.POST_EDIT_CONTENT_EVENT, this, node);
              if (listTaxonomyNameNew != null) {
                for (String categoryPath : listTaxonomyNameNew) {
                  try {
                    if (categoryPath.startsWith("/")) categoryPath = categoryPath.substring(1);
                    if(categoryPath.indexOf("/")>0)
                    {
                      taxonomyTree = categoryPath.substring(0, categoryPath.indexOf("/"));
                      taxonomyPath = categoryPath.substring(categoryPath.indexOf("/") + 1);
                    }
                    else
                    {
                      taxonomyTree = categoryPath;
                      taxonomyPath = "";
                    }
                    taxonomyService.addCategory(node, taxonomyTree, taxonomyPath);
                  } catch (ItemExistsException e) {
                    uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.ItemExistsException",
                        null, ApplicationMessage.WARNING));
                    return;
                  } catch (RepositoryException e) {
                    if (LOG.isErrorEnabled()) {
                      LOG.error("Unexpected error", e);
                    }
                    JCRExceptionManager.process(uiApp, e);
                    return;
                  }
                }
              }
            }
          }
          uiExplorer.getSession().save() ;

          Node uploadedNode = null;
          if(isMultiLanguage_) {
            uiUploadContainer.setUploadedNode(selectedNode);
            uploadedNode = selectedNode;
          } else {
            Node newNode = null ;
            if(!isExist) {
              try {
                newNode = selectedNode.getSession().getNodeByUUID(newNodeUUID);
              } catch(ItemNotFoundException e) {
                newNode = Utils.findNodeByUUID(newNodeUUID);
              }
            } else {
              newNode = selectedNode.getNode(name) ;
            }
            if(newNode != null) {
              uiUploadContainer.setUploadedNode(newNode);
              uploadedNode = newNode;
            }
          }

          //get file size
          double size = 0;
          if (uploadedNode.hasNode(Utils.JCR_CONTENT)) {
            Node contentNode = uploadedNode.getNode(Utils.JCR_CONTENT);
            if (contentNode.hasProperty(Utils.JCR_DATA)) {
              size = contentNode.getProperty(Utils.JCR_DATA).getLength();
            }
          } else {
            size = uploadService.getUploadResource(uiFormUploadInput.getUploadId()).getEstimatedSize();
          }
          String fileSize = Utils.calculateFileSize(size);
          String iconUpload = Utils.getNodeTypeIcon(uploadedNode, "16x16Icon").replaceAll("nt_file16x16Icon ", "");
          String[] arrValues = {iconUpload, Text.unescapeIllegalJcrChars(fileName),
              Text.unescapeIllegalJcrChars(name), fileSize, mimeType, uploadedNode.getPath()};
          listUploadedNodes.add(NodeLocation.getNodeLocationByNode(uploadedNode));
          listArrValues.add(arrValues);
          inputStream.close();
        }
      }
      uiUploadContent.setListUploadValues(listArrValues);
      uiManager.setRenderedChild(UIUploadContainer.class);
      uiExplorer.setIsHidePopup(true);
      uiExplorer.updateAjax(event);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    } catch(ConstraintViolationException con) {
      Object[] args = {name, } ;
      throw new MessageException(new ApplicationMessage("UIUploadForm.msg.contraint-violation",
                                                         args, ApplicationMessage.WARNING)) ;
    } catch(LockException lock) {
      throw new MessageException(new ApplicationMessage("UIUploadForm.msg.lock-exception",
          null, ApplicationMessage.WARNING)) ;
    } catch(AccessDeniedException ade) {
      throw new MessageException(new ApplicationMessage("UIActionBar.msg.access-add-denied",
          null, ApplicationMessage.WARNING));
    } catch(AccessControlException ace) {
      throw new MessageException(new ApplicationMessage("UIActionBar.msg.access-add-denied",
          null, ApplicationMessage.WARNING));
    } catch (ItemExistsException iee) {
      uiApp.addMessage(new ApplicationMessage("UIActionBar.msg.item-existed", null, ApplicationMessage.WARNING));
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs", e);
      }
      JCRExceptionManager.process(uiApp, e);
      return ;
    }
  }

  /**
   * Check if a node is child node of taxonomy node or not
   *
   * @param node
   * @return
   */
  private boolean isTaxonomyChildNode(Node node) throws RepositoryException {
    Node parrentNode = node.getParent();
    while (!((NodeImpl) parrentNode).isRoot()) {
      if (parrentNode.isNodeType(Utils.EXO_TAXONOMY)) {
        return true;
      }
      parrentNode = parrentNode.getParent();
    }
    return false;
  }

  private Map<String, JcrInputProperty> getInputProperties(String name, InputStream inputStream, String mimeType) {
    Map<String,JcrInputProperty> inputProperties = new HashMap<String,JcrInputProperty>() ;
    JcrInputProperty nodeInput = new JcrInputProperty() ;
    nodeInput.setJcrPath("/node") ;
    nodeInput.setValue(name) ;
    nodeInput.setMixintype("mix:i18n,mix:votable,mix:commentable") ;
    nodeInput.setType(JcrInputProperty.NODE) ;
    inputProperties.put("/node",nodeInput) ;

    JcrInputProperty jcrContent = new JcrInputProperty() ;
    jcrContent.setJcrPath("/node/jcr:content") ;
    jcrContent.setValue("") ;
    jcrContent.setMixintype("dc:elementSet") ;
    jcrContent.setNodetype(Utils.NT_RESOURCE) ;
    jcrContent.setType(JcrInputProperty.NODE) ;
    inputProperties.put("/node/jcr:content",jcrContent) ;

    JcrInputProperty jcrData = new JcrInputProperty() ;
    jcrData.setJcrPath("/node/jcr:content/jcr:data") ;
    jcrData.setValue(inputStream) ;
    inputProperties.put("/node/jcr:content/jcr:data",jcrData) ;

    JcrInputProperty jcrMimeType = new JcrInputProperty() ;
    jcrMimeType.setJcrPath("/node/jcr:content/jcr:mimeType") ;
    jcrMimeType.setValue(mimeType) ;
    inputProperties.put("/node/jcr:content/jcr:mimeType",jcrMimeType) ;

    JcrInputProperty jcrLastModified = new JcrInputProperty() ;
    jcrLastModified.setJcrPath("/node/jcr:content/jcr:lastModified") ;
    jcrLastModified.setValue(new GregorianCalendar()) ;
    inputProperties.put("/node/jcr:content/jcr:lastModified",jcrLastModified) ;

    JcrInputProperty jcrEncoding = new JcrInputProperty() ;
    jcrEncoding.setJcrPath("/node/jcr:content/jcr:encoding") ;
    jcrEncoding.setValue("UTF-8") ;
    inputProperties.put("/node/jcr:content/jcr:encoding",jcrEncoding) ;
    return inputProperties;
  }

  private void updateAdvanceTaxonomy(String selectField) throws Exception {
    List<UIComponent> listChildren = getChildren();
    for (UIComponent uiComp : listChildren) {
      if (uiComp.getId().equals(selectField)) {
        UIFormMultiValueInputSet uiFormMultiValueInputSet = getChildById(selectField);
        if (mapTaxonomies.containsKey(selectField))
          uiFormMultiValueInputSet.setValue(getTaxonomyLabel(mapTaxonomies.get(selectField)));
      }
    }
  }

  private List<String> getTaxonomyLabel(List<String> taxonomyPaths) {
    List<String> taxonomyLabels = new ArrayList<String>();
    String[] taxonomyPathSplit = null;
    StringBuilder buildlabel;
    StringBuilder buildPathlabel;
    for (String taxonomyPath : taxonomyPaths) {
      if (taxonomyPath.startsWith("/"))
        taxonomyPath = taxonomyPath.substring(1);
      taxonomyPathSplit = taxonomyPath.split("/");
      buildlabel = new StringBuilder();
      buildPathlabel = new StringBuilder();
      for (int i = 0; i < taxonomyPathSplit.length; i++) {
        buildlabel = new StringBuilder("eXoTaxonomies");
        try {
          for (int j = 0; j <= i; j++) {
            buildlabel.append(".").append(taxonomyPathSplit[j]);
          }
          buildPathlabel.append(Utils.getResourceBundle(buildlabel.append(".label").toString())).append("/");
        } catch (MissingResourceException me) {
          buildPathlabel.append(taxonomyPathSplit[i]).append("/");
        }

      }

      taxonomyLabels.add(buildPathlabel.substring(0, buildPathlabel.length() - 1));
    }
    return taxonomyLabels;
  }

  private boolean passNameValidation(String name) throws Exception {
    if (name == null || name.contains("[") || name.contains("]") ||
        name.contains("\"") || name.contains("/"))
      return false;

    int count = 0;
    for (char c : name.toCharArray()) {
      if (c == ':') count ++;
      if (count > 1) return false;
    }

    if (count == 1) {
      if (name.split(":").length < 2) return false;
      String namespace = name.split(":")[0];
      NamespaceRegistry namespaceRegistry = getApplicationComponent(RepositoryService.class)
      .getRepository(getAncestorOfType(UIJCRExplorer.class).getRepositoryName()).getNamespaceRegistry() ;
      String[] prefixs = namespaceRegistry.getPrefixes();
      for (String prefix : prefixs)
        if (namespace.equals(prefix))
          return true;
      return false;

    }
    return true;
  }
  
  private boolean contains(String[] arr, String item) {
    if (arr != null) {
      for (String arrItem : arr) {
        if (arrItem != null && arrItem.equals(item))
          return true;
        if (arrItem == item)
          return true;
      }
    }
    return false;
  }

  static  public class SaveActionListener extends EventListener<UIUploadForm> {
    public void execute(Event<UIUploadForm> event) throws Exception {
      UIUploadForm uiForm = event.getSource();
      UIUploadManager uiManager = uiForm.getParent();
      if(uiForm.getListSameNames(event).size() > 0) {
        UIPopupWindow uiPopupWindow = uiManager.initPopupWhenHaveSameName();
        UIUploadBehaviorWithSameName uiUploadBehavior =
          uiManager.createUIComponent(UIUploadBehaviorWithSameName.class, null, null);
        uiUploadBehavior.setMessageKey("UIUploadForm.msg.confirm-behavior");
        uiUploadBehavior.setArguments(
            uiForm.getListSameNames(event).toArray(new String[uiForm.getListSameNames(event).size()]));
        uiPopupWindow.setUIComponent(uiUploadBehavior);
        uiPopupWindow.setShow(true);
        uiPopupWindow.setRendered(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
        return;
      }
      uiForm.doUpload(event, false);
    }
  }

  static  public class CancelActionListener extends EventListener<UIUploadForm> {
    public void execute(Event<UIUploadForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }

  static  public class RemoveActionListener extends EventListener<UIFormMultiValueInputSet> {
    public void execute(Event<UIFormMultiValueInputSet> event) throws Exception {
      UIFormMultiValueInputSet uiSet = event.getSource();
      UIComponent uiComponent = uiSet.getParent();
      if (uiComponent instanceof UIUploadForm) {
        UIUploadForm uiUploadForm = (UIUploadForm)uiComponent;
        String id = event.getRequestContext().getRequestParameter(OBJECTID);
        String[] arrayId = id.split(FIELD_LISTTAXONOMY);
        int index = 0;
        int indexRemove = 0;
        if ((arrayId.length > 0) && (arrayId[0].length() > 0))
          index = new Integer(arrayId[0]).intValue();
        if ((arrayId.length > 0) && (arrayId[1].length() > 0))
          indexRemove = new Integer(arrayId[1]).intValue();
        String idFieldListTaxonomy;
        if (index == 0)
          idFieldListTaxonomy = FIELD_LISTTAXONOMY;
        else
          idFieldListTaxonomy = index + FIELD_LISTTAXONOMY;
        if (uiUploadForm.mapTaxonomies.containsKey(idFieldListTaxonomy)) {
          List<String> indexMapTaxonomy = new ArrayList<String>();
          indexMapTaxonomy = uiUploadForm.mapTaxonomies.get(idFieldListTaxonomy);
          uiUploadForm.mapTaxonomies.remove(idFieldListTaxonomy);
          if (indexMapTaxonomy.size() > indexRemove) indexMapTaxonomy.remove(indexRemove);
          uiUploadForm.mapTaxonomies.put(idFieldListTaxonomy, indexMapTaxonomy);
        }
        uiSet.removeChildById(id);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadForm);
      }
    }
  }

  static  public class AddActionListener extends EventListener<UIFormMultiValueInputSet> {
    public void execute(Event<UIFormMultiValueInputSet> event) throws Exception {
      UIFormMultiValueInputSet uiSet = event.getSource();
      UIUploadForm uiUploadForm =  (UIUploadForm) uiSet.getParent();
      UIApplication uiApp = uiUploadForm.getAncestorOfType(UIApplication.class);
      try {
        String fieldTaxonomyId = event.getRequestContext().getRequestParameter(OBJECTID);
        String[] arrayId = fieldTaxonomyId.split(FIELD_LISTTAXONOMY);
        int index = 0;
        if ((arrayId.length > 0) && (arrayId[0].length() > 0)) index = new Integer(arrayId[0]).intValue();
        String idFieldUpload;
        if (index == 0) idFieldUpload = FIELD_UPLOAD; else idFieldUpload = index + FIELD_UPLOAD;
        UIFormUploadInput uiFormUploadInput = uiUploadForm.getChildById(idFieldUpload);
        UploadResource uploadResource = uiFormUploadInput.getUploadResource();
        if (uploadResource == null) {
          uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.upload-not-null", null,
              ApplicationMessage.WARNING));

          return;
        }
        UIUploadManager uiUploadManager = uiUploadForm.getParent();
        UIJCRExplorer uiExplorer = uiUploadForm.getAncestorOfType(UIJCRExplorer.class);
        String repository = uiExplorer.getRepositoryName();


        UIPopupWindow uiPopupWindow = uiUploadManager.initPopupTaxonomy(POPUP_TAXONOMY);
        UIOneTaxonomySelector uiOneTaxonomySelector =
          uiUploadManager.createUIComponent(UIOneTaxonomySelector.class, null, null);
        uiPopupWindow.setUIComponent(uiOneTaxonomySelector);
        TaxonomyService taxonomyService = uiUploadForm.getApplicationComponent(TaxonomyService.class);
        List<Node> lstTaxonomyTree = taxonomyService.getAllTaxonomyTrees();
        if (lstTaxonomyTree.size() == 0) throw new AccessDeniedException();
        String workspaceName = lstTaxonomyTree.get(0).getSession().getWorkspace().getName();
        uiOneTaxonomySelector.setIsDisable(workspaceName, false);
        uiOneTaxonomySelector.setRootNodeLocation(repository, workspaceName, lstTaxonomyTree.get(0).getPath());
        uiOneTaxonomySelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
        uiOneTaxonomySelector.init(uiExplorer.getSystemProvider());
        String param = "returnField=" + fieldTaxonomyId;
        uiOneTaxonomySelector.setSourceComponent(uiUploadForm, new String[]{param});
        uiPopupWindow.setRendered(true);
        uiPopupWindow.setShow(true);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadManager);
      } catch (AccessDeniedException accessDeniedException) {
        uiApp.addMessage(new ApplicationMessage("Taxonomy.msg.AccessDeniedException", null,
            ApplicationMessage.WARNING));

        return;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);

        return;
      }
    }
  }

  static  public class AddUploadActionListener extends EventListener<UIUploadForm> {
    public void execute(Event<UIUploadForm> event) throws Exception {
      UIUploadForm uiUploadForm = event.getSource();
      List<UIComponent> listChildren = uiUploadForm.getChildren();
      int index = 0;
      int numberUploadFile = 0;
      String fieldFieldUpload = null;
      for (UIComponent uiComp : listChildren) {
        if(uiComp instanceof UIFormUploadInput) {
          fieldFieldUpload = uiComp.getId();
          numberUploadFile++;
        }
      }
      if (fieldFieldUpload != null) {
        String[] arrayId = fieldFieldUpload.split(FIELD_UPLOAD);
        if ((arrayId.length > 0) && (arrayId[0].length() > 0)) index = new Integer(arrayId[0]).intValue();
      }
      index++;
      uiUploadForm.addUIFormInput(new UIFormStringInput(index + FIELD_NAME, index + FIELD_NAME, null));
      PortletRequestContext pcontext = (PortletRequestContext)WebuiRequestContext.getCurrentInstance();
      PortletPreferences portletPref = pcontext.getRequest().getPreferences();
      String limitPref = portletPref.getValue(Utils.UPLOAD_SIZE_LIMIT_MB, "");
      UIFormUploadInput uiInput = null;
      if (limitPref != null) {
        try {
          uiInput = new UIFormUploadInput(index + FIELD_UPLOAD,
                                          index + FIELD_UPLOAD,
                                          Integer.parseInt(limitPref.trim()));
        } catch (NumberFormatException e) {
          uiInput = new UIFormUploadInput(index + FIELD_UPLOAD, index + FIELD_UPLOAD);
        }
      } else {
        uiInput = new UIFormUploadInput(index + FIELD_UPLOAD, index + FIELD_UPLOAD);
      }
      uiInput.setAutoUpload(true);
      uiUploadForm.addUIFormInput(uiInput);
      UIFormMultiValueInputSet uiFormMultiValue = uiUploadForm.createUIComponent(UIFormMultiValueInputSet.class,
                                                                                 "UploadMultipleInputset",
                                                                                 null);
      uiFormMultiValue.setId(index + FIELD_LISTTAXONOMY);
      uiFormMultiValue.setName(index + FIELD_LISTTAXONOMY);
      uiFormMultiValue.setType(UIFormStringInput.class);
      uiFormMultiValue.setEditable(false);
      uiUploadForm.addUIFormInput(uiFormMultiValue);
      uiUploadForm.setNumberUploadFile(numberUploadFile + 1);
      uiUploadForm.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadForm.getParent());
    }
  }

  static  public class RemoveUploadActionListener extends EventListener<UIUploadForm> {
    public void execute(Event<UIUploadForm> event) throws Exception {
      String id = event.getRequestContext().getRequestParameter(OBJECTID);
      UIUploadForm uiUploadForm = event.getSource();
      List<UIComponent> listChildren = uiUploadForm.getChildren();
      int index = 0;
      for (UIComponent uiComp : listChildren) {
        if(uiComp instanceof UIFormUploadInput) index++;
      }
      String[] arrayId = id.split(FIELD_NAME);
      int indexRemove = 0;
      if ((arrayId.length > 0) && (arrayId[0].length() > 0))
        indexRemove = new Integer(arrayId[0]).intValue();
      if (indexRemove == 0) {
        uiUploadForm.removeChildById(FIELD_NAME);
        uiUploadForm.removeChildById(FIELD_UPLOAD);
        uiUploadForm.removeChildById(FIELD_LISTTAXONOMY);
        if (uiUploadForm.mapTaxonomies.containsKey(FIELD_LISTTAXONOMY))
          uiUploadForm.mapTaxonomies.remove(FIELD_LISTTAXONOMY);
      } else {
        uiUploadForm.removeChildById(indexRemove + FIELD_NAME);
        uiUploadForm.removeChildById(indexRemove + FIELD_UPLOAD);
        uiUploadForm.removeChildById(indexRemove + FIELD_LISTTAXONOMY);
        if (uiUploadForm.mapTaxonomies.containsKey(indexRemove + FIELD_LISTTAXONOMY))
          uiUploadForm.mapTaxonomies.remove(indexRemove + FIELD_LISTTAXONOMY);
      }
      uiUploadForm.setNumberUploadFile(index - 1);
      uiUploadForm.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiUploadForm.getParent());
    }
  }
}
