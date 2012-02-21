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
package org.exoplatform.ecm.webui.component.browsecontent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.portlet.PortletPreferences;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.comparator.ItemOptionNameComparator;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.queries.QueryService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.validator.NumberFormatValidator;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Dec 19, 2006 9:05:58 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(listeners = UIQueryConfig.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueryConfig.ChangeLangActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueryConfig.ChangeStatusActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueryConfig.ChangeTypeActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueryConfig.EditActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueryConfig.AddActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueryConfig.CancelActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIQueryConfig.BackActionListener.class)
    }
)
public class UIQueryConfig extends UIForm {

  final private String xpathDefault_ = "/jcr:root/Documents/Live//element(*, exo:article)";
  final private String sqlDefault_ = "select * from exo:article where jcr:path like '/Documents/Live%'";
  final private static String NEW_QUERY = "New Query";
  final private static String EXISTING_QUERY = "Existing Query";
  final private static String PERSONAL_QUERY = "Personal Query";
  final private static String SHARED_QUERY = "Shared Query";
  final private static String EMPTYQUERY = "Query not found";
  protected boolean isEdit_ = false;
  private static final Log LOG  = ExoLogger.getLogger("browsecontent.UIQueryConfig");
  public UIQueryConfig() throws Exception {
    List<SelectItemOption<String>> Options = new ArrayList<SelectItemOption<String>>();
    addChild(new UIFormStringInput(UINewConfigForm.FIELD_REPOSITORY, UINewConfigForm.FIELD_REPOSITORY, null));
    addChild(new UIFormStringInput(UINewConfigForm.FIELD_WORKSPACE, UINewConfigForm.FIELD_WORKSPACE, null));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_QUERYSTATUS, null, Options));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_QUERYLANG, null, Options));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_QUERYTYPE, null, Options).setRendered(false));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_QUERYSTORE, null, Options).setRendered(false));
    addChild(new UIFormTextAreaInput(UINewConfigForm.FIELD_QUERY, null, null));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_TEMPLATE, null, Options));
    addChild(new UIFormStringInput(UINewConfigForm.FIELD_ITEMPERPAGE,
        UINewConfigForm.FIELD_ITEMPERPAGE, null).addValidator(NumberFormatValidator.class));
    addChild(new UIFormSelectBox(UINewConfigForm.FIELD_DETAILBOXTEMP, null, Options));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ALLOW_PUBLISH, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLETAGMAP, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLECOMMENT, null, null));
    addChild(new UIFormCheckBoxInput<Boolean>(UINewConfigForm.FIELD_ENABLEVOTE, null, null));
    setActions(UINewConfigForm.DEFAULT_ACTION);
  }

  public PortletPreferences getPortletPreferences() {
    return getAncestorOfType(UIBrowseContentPortlet.class).getPortletPreferences();
  }

  public void initForm(PortletPreferences preference, String repository, String workSpace,
      boolean isAddNew) throws Exception {
    String queryLang = "sql";
    String queryType = PERSONAL_QUERY;
    String queryStoreName = null;
    String queryStatement = sqlDefault_;
    String queryNew = "true";
    String hasComment = "false";
    String hasVote = "false";
    String hasTagMap = "false";
    String itemPerPage = null;
    String detailTemp = "";
    String template = "";
    boolean isAllowPublish = Boolean.parseBoolean(preference.getValue(Utils.CB_ALLOW_PUBLISH, ""));
    UIFormStringInput workSpaceField = getChildById(UINewConfigForm.FIELD_WORKSPACE);
    workSpaceField.setValue(workSpace);
    workSpaceField.setEditable(false);
    UIFormStringInput repositoryField = getChildById(UINewConfigForm.FIELD_REPOSITORY);
    repositoryField.setValue(repository);
    repositoryField.setEditable(false);
    UIFormSelectBox queryStatusField = getChildById(UINewConfigForm.FIELD_QUERYSTATUS);
    UIFormSelectBox queryLangField = getChildById(UINewConfigForm.FIELD_QUERYLANG);
    UIFormSelectBox queryTypeField = getChildById(UINewConfigForm.FIELD_QUERYTYPE);
    UIFormSelectBox queryStoreField = getChildById(UINewConfigForm.FIELD_QUERYSTORE);
    UIFormTextAreaInput queryField = getChildById(UINewConfigForm.FIELD_QUERY);
    UIFormSelectBox templateField = getChildById(UINewConfigForm.FIELD_TEMPLATE);
    UIFormStringInput numbPerPageField = getChildById(UINewConfigForm.FIELD_ITEMPERPAGE);
    UIFormSelectBox detailtemField = getChildById(UINewConfigForm.FIELD_DETAILBOXTEMP);
    UIFormCheckBoxInput allowPublishField = getChildById(UINewConfigForm.FIELD_ALLOW_PUBLISH);
    UIFormCheckBoxInput enableTagMapField = getChildById(UINewConfigForm.FIELD_ENABLETAGMAP);
    UIFormCheckBoxInput enableCommentField = getChildById(UINewConfigForm.FIELD_ENABLECOMMENT);
    UIFormCheckBoxInput enableVoteField = getChildById(UINewConfigForm.FIELD_ENABLEVOTE);
    if(isEdit_) {
      if(isAddNew) {
        setActions(UINewConfigForm.ADD_NEW_ACTION);
        templateField.setOptions(getQueryTemplate());
        UIConfigTabPane uiConfigTabPane = getAncestorOfType(UIConfigTabPane.class);
        detailtemField.setOptions(uiConfigTabPane.getBoxTemplateOption());
        queryStatusField.setOptions(getQueryStatus());
        queryStatusField.setValue(NEW_QUERY);
        queryLangField.setOptions(getQueryLang());
        queryLangField.setValue(queryLang);
        queryTypeField.setOptions(getQueryType());
        queryTypeField.setValue(queryType);
        onchangeAction(queryStatusField.getValue(), queryTypeField.getValue(),
            queryLangField.getValue(), null, queryStatement);
        numbPerPageField.setValue(itemPerPage);
        allowPublishField.setChecked(isAllowPublish);
        enableTagMapField.setChecked(Boolean.parseBoolean(hasTagMap));
        enableCommentField.setChecked(Boolean.parseBoolean(hasComment));
        enableVoteField.setChecked(Boolean.parseBoolean(hasVote));
        queryStatusField.setOptions(getQueryStatus());
      }else {
        setActions(UINewConfigForm.NORMAL_ACTION);
      }
    } else {
      setActions(UINewConfigForm.DEFAULT_ACTION);
      repository = preference.getValue(Utils.REPOSITORY, "");
      queryNew = preference.getValue(Utils.CB_QUERY_ISNEW, "");
      queryType = preference.getValue(Utils.CB_QUERY_TYPE, "");
      queryStoreName = preference.getValue(Utils.CB_QUERY_STORE, "");
      queryStatement = preference.getValue(Utils.CB_QUERY_STATEMENT, "");
      queryLang = preference.getValue(Utils.CB_QUERY_LANGUAGE, "");
      itemPerPage = preference.getValue(Utils.CB_NB_PER_PAGE, "");
      hasTagMap  = preference.getValue(Utils.CB_VIEW_TAGMAP, "");
      hasComment = preference.getValue(Utils.CB_VIEW_COMMENT, "");
      detailTemp = preference.getValue(Utils.CB_BOX_TEMPLATE, "");
      hasVote = preference.getValue(Utils.CB_VIEW_VOTE, "");
      template = preference.getValue(Utils.CB_TEMPLATE, "");

      templateField.setOptions(getQueryTemplate());
      templateField.setValue(template);
      numbPerPageField.setValue(itemPerPage);
      UIConfigTabPane uiConfigTabPane = getAncestorOfType(UIConfigTabPane.class);
      detailtemField.setOptions(uiConfigTabPane.getBoxTemplateOption());
      detailtemField.setValue(detailTemp);
      allowPublishField.setChecked(isAllowPublish);
      enableTagMapField.setChecked(Boolean.parseBoolean(hasTagMap));
      enableCommentField.setChecked(Boolean.parseBoolean(hasComment));
      enableVoteField.setChecked(Boolean.parseBoolean(hasVote));
      queryStatusField.setOptions(getQueryStatus());
      queryStatusField.setValue(EXISTING_QUERY);
      if(Boolean.parseBoolean(queryNew)) queryStatusField.setValue(NEW_QUERY);
      queryLangField.setOptions(getQueryLang());
      queryLangField.setValue(queryLang);
      queryTypeField.setOptions(getQueryType());
      queryTypeField.setValue(queryType);
      queryStoreField.setValue(queryStoreName);
      onchangeAction(queryStatusField.getValue(), queryTypeField.getValue(), queryLangField.getValue(),
          queryStoreName, queryStatement);
    }
    queryStatusField.setOnChange("ChangeStatus");
    queryLangField.setOnChange("ChangeLang");
    queryTypeField.setOnChange("ChangeType");
    queryStatusField.setEnable(isEdit_);
    queryLangField.setEnable(isEdit_);
    queryTypeField.setEnable(isEdit_);
    queryStoreField.setEnable(isEdit_);
    queryField.setEditable(isEdit_);
    numbPerPageField.setEditable(isEdit_);
    templateField.setEnable(isEdit_);
    detailtemField.setEnable(isEdit_);
    allowPublishField.setEnable(isEdit_);
    enableTagMapField.setEnable(isEdit_);
    enableCommentField.setEnable(isEdit_);
    enableVoteField.setEnable(isEdit_);
  }

  protected void onchangeAction(String queryStatus, String queryType, String queryLanguage,
      String queryStoreName,
      String queryStatement) throws Exception {
    boolean isNewquery = queryStatus.equals(NEW_QUERY);
    UIFormSelectBox queryStore = getChildById(UINewConfigForm.FIELD_QUERYSTORE);
    UIFormSelectBox queryTypeField = getChildById(UINewConfigForm.FIELD_QUERYTYPE);
    UIFormTextAreaInput queryField = getChildById(UINewConfigForm.FIELD_QUERY);
    if(isNewquery) {
      if(queryLanguage.equals(Query.XPATH)) {
        if(queryStatement == null) queryStatement = xpathDefault_;
        queryField.setValue(queryStatement);
      } else if(queryLanguage.equals(Query.SQL)) {
        if(queryStatement == null) queryStatement = sqlDefault_;
        queryField.setValue(queryStatement);
      }
    } else {
      queryStore.setOptions(getQueryStore(queryType, queryLanguage));
      if(queryStoreName != null) queryStore.setValue(queryStoreName);
    }
    queryStore.setRendered(!isNewquery);
    queryTypeField.setRendered(!isNewquery);
    queryField.setRendered(isNewquery);
  }

  @SuppressWarnings("unchecked")
  private List<SelectItemOption<String>> getQueryTemplate() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    List<Node> querylTemplates = getApplicationComponent(ManageViewService.class).
      getAllTemplates(BasePath.CB_QUERY_TEMPLATES, SessionProviderFactory.createSystemProvider());
    for(Node node: querylTemplates){
      options.add(new SelectItemOption<String>(node.getName(),node.getName()));
    }
    Collections.sort(options, new ItemOptionNameComparator());
    return options;
  }

  public List<SelectItemOption<String>> getWorkSpaceOption() throws Exception {
    UIConfigTabPane uiTabPane = getAncestorOfType(UIConfigTabPane.class);
    return uiTabPane.getWorkSpaceOption();
  }

  private List<SelectItemOption<String>> getQueryLang() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(Query.SQL, Query.SQL));
    options.add(new SelectItemOption<String>(Query.XPATH, Query.XPATH));
    return options;
  }

  private List<SelectItemOption<String>> getQueryStatus() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(NEW_QUERY, NEW_QUERY));
    options.add(new SelectItemOption<String>(EXISTING_QUERY, EXISTING_QUERY));
    return options;
  }

  private List<SelectItemOption<String>> getQueryType() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(SHARED_QUERY, SHARED_QUERY));
    options.add(new SelectItemOption<String>(PERSONAL_QUERY, PERSONAL_QUERY));
    return options;
  }

  @SuppressWarnings("unchecked")
  private List<SelectItemOption<String>> getQueryStore(String queryType, String queryLanguage) throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    QueryService qservice = getApplicationComponent(QueryService.class);
    SessionProvider provider = SessionProviderFactory.createSystemProvider();
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if(UIQueryConfig.PERSONAL_QUERY.equals(queryType)) {
      List<Query> queries = qservice.getQueries(userId, provider);
      for(Query queryNode : queries) {
        String path = queryNode.getStoredQueryPath();
        if(queryNode.getLanguage().equals(queryLanguage)) {
          options.add(new SelectItemOption<String>(path.substring(path.lastIndexOf("/")+ 1), path));
        }
      }
    } else {
      List<Node> queries = qservice.getSharedQueries(queryLanguage,userId, provider);
      for(Node queryNode : queries) {
        options.add(new SelectItemOption<String>(queryNode.getName(), queryNode.getPath()));
      }
    }
    if(options.isEmpty()) {
      options.add(new SelectItemOption<String>(EMPTYQUERY, EMPTYQUERY));
    }
    Collections.sort(options, new ItemOptionNameComparator());
    return options;
  }

  public Session getSession() throws Exception {
    String workspace = getUIStringInput(UINewConfigForm.FIELD_WORKSPACE).getValue();
    ManageableRepository repository =
      getApplicationComponent(RepositoryService.class).getCurrentRepository();
    if(SessionProviderFactory.isAnonim()) {
      return SessionProviderFactory.createAnonimProvider().getSession(workspace,repository);
    }
    return SessionProviderFactory.createSessionProvider().getSession(workspace,repository);
  }

  public static class SaveActionListener extends EventListener<UIQueryConfig>{
    public void execute(Event<UIQueryConfig> event) throws Exception {
      UIQueryConfig uiForm = event.getSource();
      UIBrowseContentPortlet uiBrowseContentPortlet = uiForm.getAncestorOfType(UIBrowseContentPortlet.class);
      PortletPreferences prefs = uiBrowseContentPortlet.getPortletPreferences();
      String repository = uiForm.getUIStringInput(UINewConfigForm.FIELD_REPOSITORY).getValue();
      String workSpace = uiForm.getUIStringInput(UINewConfigForm.FIELD_WORKSPACE).getValue();
      String queryLang = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_QUERYLANG).getValue();
      String query = uiForm.getUIFormTextAreaInput(UINewConfigForm.FIELD_QUERY).getValue();
      String template = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_TEMPLATE).getValue();
      String itemPerPage = uiForm.getUIStringInput(UINewConfigForm.FIELD_ITEMPERPAGE).getValue();
      String boxTemplate = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_DETAILBOXTEMP).getValue();
      UIFormSelectBox queryValueField = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_QUERYSTORE);
      String  queryPath = "";
      String queryStatu = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_QUERYSTATUS).getValue();
      boolean isNewquery = queryStatu.equals(UIQueryConfig.NEW_QUERY);
      UIApplication app = uiForm.getAncestorOfType(UIApplication.class);
      if((!queryStatu.equals(UIQueryConfig.NEW_QUERY))&&(queryValueField.isRendered())) {
        queryPath = queryValueField.getValue();
        if(queryPath.equals(UIQueryConfig.EMPTYQUERY)){
          app.addMessage(new ApplicationMessage("UIQueryConfig.msg.invalid-name", null,
              ApplicationMessage.WARNING));
          
          return;
        }
      }
      if(isNewquery) {
        if(Utils.isNameEmpty(query)) {
          app.addMessage(new ApplicationMessage("UIQueryConfig.msg.invalid-query", null,
              ApplicationMessage.WARNING));
          
          return;
        }
        try {
          QueryManager queryManager =uiForm.getSession().getWorkspace().getQueryManager();
          Query queryObj = queryManager.createQuery(query, queryLang);
          queryObj.execute();
        } catch(InvalidQueryException iqe) {
          app.addMessage(new ApplicationMessage("UIQueryConfig.msg.invalid-query", null,
              ApplicationMessage.WARNING));
          
          return;
        } catch(NoSuchNodeTypeException nt){
          app.addMessage(new ApplicationMessage("UIQueryConfig.msg.noSuchNodeTypeException", null,
              ApplicationMessage.WARNING));
          
          return;
        } catch(RepositoryException rp){
          app.addMessage(new ApplicationMessage("UIQueryConfig.msg.repostoryException", null,
              ApplicationMessage.WARNING));
          
          return;
        }catch(Exception e){
          if (LOG.isErrorEnabled()) {
            LOG.error("Unexpected error", e);
          }
        }
      }
      try{
        Integer.parseInt(itemPerPage);
      } catch(Exception e){
        app.addMessage(new ApplicationMessage("UIQueryConfig.msg.invalid-value", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      if(Integer.parseInt(itemPerPage) == 0) {
        app.addMessage(new ApplicationMessage("UIQueryConfig.msg.invalid-value", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      String  queryType = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_QUERYTYPE).getValue();
      boolean allowPublish = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ALLOW_PUBLISH).isChecked();
      boolean hasTagMap = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLETAGMAP).isChecked();
      boolean hasComment = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLECOMMENT).isChecked();
      boolean hasVote = uiForm.getUIFormCheckBoxInput(UINewConfigForm.FIELD_ENABLEVOTE).isChecked();
      prefs.setValue(Utils.CB_USECASE, Utils.CB_USE_JCR_QUERY);
      prefs.setValue(Utils.REPOSITORY, repository);
      prefs.setValue(Utils.WORKSPACE_NAME, workSpace);
      prefs.setValue(Utils.CB_QUERY_LANGUAGE, queryLang);
      prefs.setValue(Utils.CB_NB_PER_PAGE, itemPerPage);
      prefs.setValue(Utils.CB_TEMPLATE, template);
      prefs.setValue(Utils.CB_BOX_TEMPLATE, boxTemplate);
      prefs.setValue(Utils.CB_ALLOW_PUBLISH, String.valueOf(allowPublish));
      prefs.setValue(Utils.CB_VIEW_TAGMAP, String.valueOf(hasTagMap));
      prefs.setValue(Utils.CB_VIEW_COMMENT,String.valueOf(hasComment));
      prefs.setValue(Utils.CB_VIEW_VOTE,String.valueOf(hasVote));
      prefs.setValue(Utils.CB_QUERY_ISNEW, String.valueOf(isNewquery));
      prefs.setValue(Utils.CB_QUERY_TYPE, queryType);
      prefs.setValue(Utils.CB_QUERY_STORE, queryPath);
      prefs.setValue(Utils.CB_QUERY_STATEMENT, query);
      prefs.store();
      uiBrowseContentPortlet.getChild(UIBrowseContainer.class).setShowDocumentDetail(false);
      uiBrowseContentPortlet.getChild(UIBrowseContainer.class).loadPortletConfig(prefs);
      uiForm.isEdit_ = false;
      UIConfigTabPane uiConfigTabpane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabpane.setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabpane);
    }
  }


  public static class ChangeStatusActionListener extends EventListener<UIQueryConfig>{
    public void execute(Event<UIQueryConfig> event) throws Exception {
      UIQueryConfig uiForm = event.getSource();
      UIFormSelectBox queryStatuField = uiForm.getChildById(UINewConfigForm.FIELD_QUERYSTATUS);
      UIFormSelectBox queryTypeField = uiForm.getChildById(UINewConfigForm.FIELD_QUERYTYPE);
      UIFormSelectBox queryLangField = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_QUERYLANG);
      uiForm.onchangeAction(queryStatuField.getValue(), queryTypeField.getValue(), queryLangField.getValue(), null, null);
      uiForm.isEdit_ = true;
      UIConfigTabPane uiConfigTabpane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabpane.setNewConfig(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabpane);
    }
  }

  public static class ChangeTypeActionListener extends EventListener<UIQueryConfig>{
    public void execute(Event<UIQueryConfig> event) throws Exception {
      UIQueryConfig uiForm = event.getSource();
      UIFormSelectBox queryStatuField = uiForm.getChildById(UINewConfigForm.FIELD_QUERYSTATUS);
      UIFormSelectBox queryLangField = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_QUERYLANG);
      UIFormSelectBox queryTypeField = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_QUERYTYPE);
      uiForm.onchangeAction(queryStatuField.getValue(), queryTypeField.getValue(), queryLangField.getValue(), null, null);
      uiForm.isEdit_ = true;
      UIConfigTabPane uiConfigTabpane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabpane.setNewConfig(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabpane);
    }
  }

  public static class ChangeLangActionListener extends EventListener<UIQueryConfig>{
    public void execute(Event<UIQueryConfig> event) throws Exception {
      UIQueryConfig uiForm = event.getSource();
      UIFormSelectBox queryStatusField = uiForm.getChildById(UINewConfigForm.FIELD_QUERYSTATUS);
      UIFormSelectBox queryTypeField = uiForm.getChildById(UINewConfigForm.FIELD_QUERYTYPE);
      UIFormSelectBox queryLangField = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_QUERYLANG);
      uiForm.onchangeAction(queryStatusField.getValue(), queryTypeField.getValue(), queryLangField.getValue(), null, null);
      uiForm.isEdit_ = true;
      UIConfigTabPane uiConfigTabpane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabpane.setNewConfig(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabpane);
    }
  }

  public static class AddActionListener extends EventListener<UIQueryConfig>{
    public void execute(Event<UIQueryConfig> event) throws Exception {
      UIQueryConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setNewConfig(true);
      uiConfigTabPane.showNewConfigForm(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }

  public static class CancelActionListener extends EventListener<UIQueryConfig>{
    public void execute(Event<UIQueryConfig> event) throws Exception {
      UIQueryConfig uiForm = event.getSource();
      uiForm.isEdit_ = false;
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
  public static class BackActionListener extends EventListener<UIQueryConfig>{
    public void execute(Event<UIQueryConfig> event) throws Exception {
      UIQueryConfig uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiForm.isEdit_ =  false;
      uiConfigTabPane.setNewConfig(true);
      uiConfigTabPane.showNewConfigForm(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
  public static class EditActionListener extends EventListener<UIQueryConfig>{
    public void execute(Event<UIQueryConfig> event) throws Exception {
      UIQueryConfig uiForm = event.getSource();
      uiForm.isEdit_ = true;
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
}
