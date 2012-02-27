/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.admin.taxonomy.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.component.admin.taxonomy.UITaxonomyTreeContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.TaxonomyTreeData;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL Author : Hoang Van Hung hunghvit@gmail.com
 * Apr 5, 2009
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/admin/taxonomy/UIFormWithoutAction.gtmpl",
    events = @EventConfig(listeners = UIActionTypeForm.ChangeActionTypeActionListener.class)
)

public class UIActionTypeForm extends UIForm {

  final static public String             ACTION_TYPE   = "actionType";

  final static public String             CHANGE_ACTION = "ChangeActionType";
  
  private static final Log LOG  = ExoLogger.getLogger(UIActionTypeForm.class);

  private List<SelectItemOption<String>> typeList_;

  public String                          defaultActionType_;

  public UIActionTypeForm() throws Exception {
    UIFormSelectBox uiSelectBox = new UIFormSelectBox(ACTION_TYPE, ACTION_TYPE,
        new ArrayList<SelectItemOption<String>>());
    uiSelectBox.setOnChange(CHANGE_ACTION);
    addUIFormInput(uiSelectBox);
  }

  private Iterator getCreatedActionTypes() throws Exception {
    ActionServiceContainer actionService = getApplicationComponent(ActionServiceContainer.class);
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository();
    return actionService.getCreatedActionTypes(repository).iterator();
  }

  public void setDefaultActionType(String actionType) throws Exception {
    defaultActionType_ = actionType;
    if (actionType == null) {
      defaultActionType_ = "exo:taxonomyAction";
    }
    List<SelectItemOption<String>> actionTypeList = new ArrayList<SelectItemOption<String>>();
    actionTypeList.add(new SelectItemOption<String>(defaultActionType_, defaultActionType_));
    getUIFormSelectBox(ACTION_TYPE).setOptions(actionTypeList);
    getUIFormSelectBox(ACTION_TYPE).setValue(defaultActionType_);
  }

  public void update() throws Exception {
    Iterator actions = getCreatedActionTypes();
    if (actions != null && actions.hasNext()) {
      typeList_ = new ArrayList<SelectItemOption<String>>();
      while (actions.hasNext()) {
        String action = ((NodeType) actions.next()).getName();
        typeList_.add(new SelectItemOption<String>(action, action));
      }
      getUIFormSelectBox(ACTION_TYPE).setOptions(typeList_);
      setDefaultActionType(defaultActionType_);

    }
  }

  public static class ChangeActionTypeActionListener extends EventListener<UIActionTypeForm> {
    public void execute(Event<UIActionTypeForm> event) throws Exception {
      UIActionTypeForm uiActionType = event.getSource();
      UITaxonomyTreeContainer uiTaxonomyTreeContainer = uiActionType
          .getAncestorOfType(UITaxonomyTreeContainer.class);
      String actionType = uiActionType.getUIFormSelectBox(ACTION_TYPE).getValue();
      TemplateService templateService = uiActionType.getApplicationComponent(TemplateService.class);
      UIActionTaxonomyManager uiActionTaxonomyManager = uiActionType
      .getAncestorOfType(UIActionTaxonomyManager.class);
      String userName = Util.getPortalRequestContext().getRemoteUser();
      UIApplication uiApp = uiActionType.getAncestorOfType(UIApplication.class);
      UIActionForm uiActionForm = uiActionTaxonomyManager.getChild(UIActionForm.class);
      try {
        String templatePath = templateService.getTemplatePathByUser(true, actionType, userName);
        if (templatePath == null) {
          Object[] arg = { actionType };
          uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.access-denied", arg,
              ApplicationMessage.WARNING));
          
          actionType = TaxonomyTreeData.ACTION_TAXONOMY_TREE;
          uiActionType.getUIFormSelectBox(UIActionTypeForm.ACTION_TYPE).setValue(actionType);
        }
      } catch (PathNotFoundException path) {
        Object[] arg = { actionType };
        uiApp.addMessage(new ApplicationMessage("UIActionForm.msg.not-support", arg,
            ApplicationMessage.WARNING));
        
        actionType = TaxonomyTreeData.ACTION_TAXONOMY_TREE;
        uiActionType.getUIFormSelectBox(UIActionTypeForm.ACTION_TYPE).setValue(actionType);
      }

      TaxonomyTreeData taxoTreeData = uiTaxonomyTreeContainer.getTaxonomyTreeData();
      TaxonomyService taxonomyService = uiTaxonomyTreeContainer.getApplicationComponent(TaxonomyService.class);
      Node taxoTreeNode = null;
      try {
        taxoTreeNode = taxonomyService.getTaxonomyTree(taxoTreeData.getTaxoTreeName(),
            true);
      } catch (RepositoryException re) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(re.getMessage());
        }
      }
      uiActionForm.createNewAction(taxoTreeNode, actionType, true);
      uiActionTaxonomyManager.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiActionTaxonomyManager);
    }
  }
}
