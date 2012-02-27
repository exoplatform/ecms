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
package org.exoplatform.ecm.webui.component.admin.taxonomy;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.core.UIPagingGridDecorator;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.TaxonomyTreeData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL Author : Hoang Van Hung hunghvit@gmail.com
 * Apr 3, 2009
 */

@ComponentConfig(
    template = "app:/groovy/webui/component/admin/taxonomy/UITaxonomyTreeList.gtmpl",
    events = {
      @EventConfig(listeners = UITaxonomyTreeList.DeleteActionListener.class, confirm = "UITaxonomyTreeList.msg.confirm-delete"),
      @EventConfig(listeners = UITaxonomyTreeList.EditTaxonomyTreeActionListener.class),
      @EventConfig(listeners = UITaxonomyTreeList.AddTaxonomyTreeActionListener.class)
    }
)

public class UITaxonomyTreeList extends UIPagingGridDecorator {

  public static final String[] ACTIONS           = { "AddTaxonomyTree" };

  public static final String   ST_ADD            = "AddTaxonomyTreePopup";

  public static final String   ST_EDIT           = "EditTaxonomyTreePopup";

  public static final String   ACCESS_PERMISSION = "exo:accessPermissions";

  private boolean              isTargetInTrash_  = false;
  
  private static final Log LOG = ExoLogger.getLogger(UITaxonomyTreeList.class);

  public UITaxonomyTreeList() throws Exception {
    getUIPageIterator().setId("UITaxonomyTreeListIterator");
  }


  public String[] getActions() {
    return ACTIONS;
  }

  public List getTaxonomyTreeList() throws Exception {
    return getUIPageIterator().getCurrentPageData();
  }

  public void refresh(int currentPage) throws Exception {
    this.isTargetInTrash_ = false;
    ListAccess<TaxonomyTreeData> taxonomyTreeList = new ListAccessImpl<TaxonomyTreeData>(TaxonomyTreeData.class,
                                                                                         getAllTaxonomyTreeList());
    LazyPageList<TaxonomyTreeData> dataPageList = new LazyPageList<TaxonomyTreeData>(taxonomyTreeList,
                                                                                     getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setPageList(dataPageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  public boolean isTargetInTrash() {
    return isTargetInTrash_;
  }

  private List<TaxonomyTreeData> getAllTaxonomyTreeList() throws RepositoryException {
    List<TaxonomyTreeData> lstTaxonomyTreeData = new ArrayList<TaxonomyTreeData>();
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    List<Node> lstTaxonomyTreeNode = taxonomyService.getAllTaxonomyTrees();
    if (lstTaxonomyTreeNode != null && lstTaxonomyTreeNode.size() > 0) {
      for (Node node : lstTaxonomyTreeNode) {
        lstTaxonomyTreeData.add(setData(node));
      }
    }
    return lstTaxonomyTreeData;
  }

  private TaxonomyTreeData setData(Node node) {
    TaxonomyTreeData taxonomyTreeData = null ;
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    try {
      if (node != null) {
        taxonomyTreeData = new TaxonomyTreeData();
        taxonomyTreeData.setTaxoTreeName(node.getName());
        if (Utils.isInTrash(node)) {
          taxonomyTreeData.setEdit(false);
          this.isTargetInTrash_ = true;
        } else {
          taxonomyTreeData.setEdit(true);
        }
        taxonomyTreeData.setTaxoTreeHomePath(node.getPath());
        taxonomyTreeData.setTaxoTreeWorkspace(node.getSession().getWorkspace().getName());
        Node realTreeNode = taxonomyService.getTaxonomyTree(node.getName(), true);
        Value[] values = realTreeNode.getProperty("exo:permissions").getValues();
        StringBuffer buffer = new StringBuffer(1024);
        try {
          for (Value permission: values) {
            buffer.append(permission.getString()).append(';');
          }
        } catch (ValueFormatException e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
        catch (RepositoryException e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
        String permission = buffer.toString();
        taxonomyTreeData.setTaxoTreePermissions(permission.substring(0, permission.length() - 1));
      }
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e.getMessage(), e);
      }
      // TODO: handle exception
    }
    return taxonomyTreeData;
  }

  public static class DeleteActionListener extends EventListener<UITaxonomyTreeList> {
    public void execute(Event<UITaxonomyTreeList> event) throws Exception {
      UITaxonomyTreeList uiTaxonomyTreeList = event.getSource();
      UITaxonomyManagerTrees uiTaxonomyManagerTrees = uiTaxonomyTreeList.getParent();
      String taxoTreeName = event.getRequestContext().getRequestParameter(OBJECTID);
      String repository = uiTaxonomyTreeList.getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository();
      TaxonomyService taxonomyService = uiTaxonomyTreeList.getApplicationComponent(TaxonomyService.class);
      ActionServiceContainer actionService = uiTaxonomyTreeList.getApplicationComponent(ActionServiceContainer.class);
      UIApplication uiApp = uiTaxonomyTreeList.getAncestorOfType(UIApplication.class);
      try {
        // Remove all avaiable action
        Node taxonomyTreeNode = taxonomyService.getTaxonomyTree(taxoTreeName,true);
        actionService.removeAction(taxonomyTreeNode, repository);
        taxonomyService.removeTaxonomyTree(taxoTreeName);
      } catch(RepositoryException e) {
        uiApp.addMessage(new ApplicationMessage("UITaxonomyTreeList.msg.remove-exception",
            null, ApplicationMessage.WARNING));
        
        return;
      }
      uiTaxonomyManagerTrees.update();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManagerTrees);
    }
  }

  public static class AddTaxonomyTreeActionListener extends EventListener<UITaxonomyTreeList> {
    public void execute(Event<UITaxonomyTreeList> event) throws Exception {
      UITaxonomyManagerTrees uiTaxonomyManagerTrees = event.getSource().getParent();
      uiTaxonomyManagerTrees.removeChildById(UITaxonomyTreeList.ST_EDIT);
      uiTaxonomyManagerTrees.initPopupTreeContainer(UITaxonomyTreeList.ST_ADD);
      UITaxonomyTreeContainer uiForm = uiTaxonomyManagerTrees.findFirstComponentOfType(UITaxonomyTreeContainer.class);
      uiForm.refresh();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManagerTrees);
    }
  }

  public static class EditTaxonomyTreeActionListener extends EventListener<UITaxonomyTreeList> {
    public void execute(Event<UITaxonomyTreeList> event) throws Exception {
      UITaxonomyManagerTrees uiTaxonomyManagerTrees = event.getSource().getParent();
      uiTaxonomyManagerTrees.removeChildById(UITaxonomyTreeList.ST_ADD);
      uiTaxonomyManagerTrees.initPopupTreeContainer(UITaxonomyTreeList.ST_EDIT);
      UITaxonomyTreeContainer uiTaxoTreeContainer = uiTaxonomyManagerTrees
          .findFirstComponentOfType(UITaxonomyTreeContainer.class);
      String taxoTreeName = event.getRequestContext().getRequestParameter(OBJECTID);
      String repository = uiTaxonomyManagerTrees.getAncestorOfType(UIECMAdminPortlet.class)
      .getPreferenceRepository();
      TaxonomyTreeData taxoTreeData = new TaxonomyTreeData();
      taxoTreeData.setTaxoTreeName(taxoTreeName);
      taxoTreeData.setEdit(true);
      taxoTreeData.setRepository(repository);
      uiTaxoTreeContainer.setTaxonomyTreeData(taxoTreeData);
      uiTaxoTreeContainer.refresh();
      uiTaxoTreeContainer.viewStep(4);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiTaxonomyManagerTrees);
    }
  }
}
