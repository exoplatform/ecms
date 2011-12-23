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
import java.util.List;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Oct 25, 2006 3:23:00 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(phase = Phase.DECODE, listeners = UINewConfigForm.BackActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UINewConfigForm.NextActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UINewConfigForm.OnChangeActionListener.class)
    }
)

public class UINewConfigForm extends UIForm {
  final static public String FIELD_REPOSITORY = "repository";
  final static public String FIELD_WORKSPACE = "workspace";
  final static public String FIELD_BROWSETYPE = "browseType";
  final static public String FIELD_TEMPLATE = "template";
  final static public String FIELD_DETAILBOXTEMP = "detailBoxTemp";
  final static public String FIELD_QUERYLANG = "queryLanguage";
  final static public String FIELD_QUERYSTATUS = "queryStatus";
  final static public String FIELD_QUERYSTORE = "queryStore";
  final static public String FIELD_QUERYTYPE = "queryType";
  final static public String FIELD_CATEGORYPATH = "categoryPath";
  final static public String FIELD_SCRIPTNAME = "scriptName";
  final static public String FIELD_DOCNAME = "docName";
  final static public String FIELD_ITEMPERPAGE = "itemPerPage";
  final static public String FIELD_ENABLETOOLBAR = "enableToolBar";
  final public static String FIELD_ALLOW_PUBLISH = "isAllowPublish";
  final static public String FIELD_ENABLEREFDOC = "enableRefDoc";
  final public static String FIELD_FILTER_CATEGORY = "filterCategory";
  final static public String FIELD_ENABLECHILDDOC = "enableChildDoc";
  final static public String FIELD_ENABLETAGMAP = "enableTagMap";
  final static public String FIELD_ENABLECOMMENT = "enableComment";
  final static public String FIELD_ENABLEVOTE = "enableVote";
  final static public String FIELD_QUERY = "query";
  final static public String FIELD_SEARCH_LOCATION = "searchLocation";
  final static public String FIELD_SEARCH_PATH_ENABLE = "searchEnable";
  final static public String[] DEFAULT_ACTION = new String[]{"Edit", "Add"};
  final static public String[] NORMAL_ACTION = new String[]{"Save", "Cancel"};
  final static public String[] ADD_NEW_ACTION = new String[]{"Back", "Save"};

  public UINewConfigForm() throws Exception {
    UIFormSelectBox repoSelectBox = new UIFormSelectBox(FIELD_REPOSITORY, FIELD_REPOSITORY, getRepoOption());
    repoSelectBox.setOnChange("OnChange");
    addChild(repoSelectBox);
    addChild(new UIFormSelectBox(FIELD_WORKSPACE, FIELD_WORKSPACE, getWorkSpaceOption()));
    addChild( new UIFormSelectBox(FIELD_BROWSETYPE, FIELD_BROWSETYPE, getBrowseTypeOption()));
    setActions(new String[]{"Back", "Next"});
  }

  public void resetForm() throws Exception{
    UIFormSelectBox repoField = getUIFormSelectBox(FIELD_REPOSITORY);
    repoField.setOptions(getRepoOption());
    getUIFormSelectBox(FIELD_WORKSPACE).setOptions(getWorkSpaceOption());
    getUIFormSelectBox(FIELD_WORKSPACE).reset();
    getUIFormSelectBox(FIELD_BROWSETYPE).setOptions(getBrowseTypeOption());
    getUIFormSelectBox(FIELD_BROWSETYPE).reset();
  }

  private List<SelectItemOption<String>> getRepoOption() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    RepositoryEntry repo = repositoryService.getCurrentRepository().getConfiguration();
    options.add(new SelectItemOption<String>(repo.getName(), repo.getName()));
    return options;
  }

  private List<SelectItemOption<String>> getWorkSpaceOption() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    String[] workspaceNames =
      getApplicationComponent(RepositoryService.class).getCurrentRepository().getWorkspaceNames();
    for(String workspace:workspaceNames) {
      options.add(new SelectItemOption<String>(workspace,workspace));
    }
    return options;
  }

  private List<SelectItemOption<String>> getBrowseTypeOption() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    options.add(new SelectItemOption<String>(Utils.FROM_PATH, Utils.CB_USE_FROM_PATH));
    options.add(new SelectItemOption<String>(Utils.USE_JCR_QUERY, Utils.CB_USE_JCR_QUERY));
    options.add(new SelectItemOption<String>(Utils.USE_SCRIPT, Utils.CB_USE_SCRIPT));
    options.add(new SelectItemOption<String>(Utils.USE_DOCUMENT, Utils.CB_USE_DOCUMENT));
    return options;
  }

  public static class OnChangeActionListener extends EventListener<UINewConfigForm>{
    public void execute(Event<UINewConfigForm> event) throws Exception {
      UINewConfigForm uiForm = event.getSource();
      UIFormSelectBox workspaceSelect = uiForm.getUIFormSelectBox(UINewConfigForm.FIELD_WORKSPACE);
      workspaceSelect.setOptions(uiForm.getWorkSpaceOption());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }

  }

  public static class BackActionListener extends EventListener<UINewConfigForm>{
    public void execute(Event<UINewConfigForm> event) throws Exception {
      UINewConfigForm uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setNewConfig(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }

  public static class NextActionListener extends EventListener<UINewConfigForm>{
    public void execute(Event<UINewConfigForm> event) throws Exception {
      UINewConfigForm uiForm = event.getSource();
      UIConfigTabPane uiConfigTabPane = uiForm.getAncestorOfType(UIConfigTabPane.class);
      uiConfigTabPane.setNewConfig(true);
      String browseType = uiForm.getUIFormSelectBox(FIELD_BROWSETYPE).getValue();
      String workSpace = uiForm.getUIFormSelectBox(FIELD_WORKSPACE).getValue();
      String repository = uiForm.getUIFormSelectBox(FIELD_REPOSITORY).getValue();
      uiConfigTabPane.initNewConfig(browseType, repository, workSpace);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiConfigTabPane);
    }
  }
}

