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
package org.exoplatform.ecm.webui.component.explorer.popup.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 18, 2006
 * 2:28:18 PM
 */
@ComponentConfig(template = "app:/groovy/webui/component/explorer/popup/admin/UICategoriesAddedList.gtmpl",
                 events = {
    @EventConfig(listeners = UICategoriesAddedList.DeleteActionListener.class,
                 confirm = "UICategoriesAddedList.msg.confirm-delete") })
public class UICategoriesAddedList extends UIContainer implements UISelectable {

  private UIPageIterator uiPageIterator_;

  private static final Log LOG = ExoLogger.getLogger(UICategoriesAddedList.class);

  public UICategoriesAddedList() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "CategoriesAddedList");
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  public List getListCategories() throws Exception {
    return NodeLocation.getNodeListByLocationList(uiPageIterator_.getCurrentPageData());
  }

  public void updateGrid(int currentPage) throws Exception {
    ListAccess<Object> categoryList = new ListAccessImpl<Object>(Object.class,
                                                                 NodeLocation.getLocationsByNodeList(getCategories()));
    LazyPageList<Object> objPageList = new LazyPageList<Object>(categoryList, 10);
    uiPageIterator_.setPageList(objPageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  public List<Node> getCategories() throws Exception {
    List<Node> listCategories = new ArrayList<Node>();
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class);
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    List<Node> listNode = getAllTaxonomyTrees();
    for(Node itemNode : listNode) {
      listCategories.addAll(taxonomyService.getCategories(uiJCRExplorer.getCurrentNode(), itemNode.getName()));
    }
    return listCategories;
  }

  List<Node> getAllTaxonomyTrees() throws RepositoryException {
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    return taxonomyService.getAllTaxonomyTrees();
  }

  String displayCategory(Node node, List<Node> taxonomyTrees) {
    try {
      for (Node taxonomyTree : taxonomyTrees) {
        if (node.getPath().contains(taxonomyTree.getPath())) {
          return getCategoryLabel(node.getPath().replace(taxonomyTree.getPath(), taxonomyTree.getName()));
        }
      }
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error when ");
      }
    }
    return "";
  }

  private String getCategoryLabel(String resource) {
    String[] taxonomyPathSplit = resource.split("/");
    StringBuilder buildlabel;
    StringBuilder buildPathlabel = new StringBuilder();
    for (int i = 0; i < taxonomyPathSplit.length; i++) {
      buildlabel = new StringBuilder("eXoTaxonomies");
      for (int j = 0; j <= i; j++) {
        buildlabel.append(".").append(taxonomyPathSplit[j]);
      }
      try {
        buildPathlabel.append(Utils.getResourceBundle(buildlabel.append(".label").toString())).append("/");
      } catch (MissingResourceException me) {
        buildPathlabel.append(taxonomyPathSplit[i]).append("/");
      }
    }
    return buildPathlabel.substring(0, buildPathlabel.length() - 1);
  }

  @SuppressWarnings("unused")
  public void doSelect(String selectField, Object value) throws Exception {
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class);
    UICategoryManager uiCategoryManager = getAncestorOfType(UICategoryManager.class);
    UIOneTaxonomySelector uiOneTaxonomySelector = uiCategoryManager.getChild(UIOneTaxonomySelector.class);
    String rootTaxonomyName = uiOneTaxonomySelector.getRootTaxonomyName();
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    try {
      Node currentNode = uiJCRExplorer.getCurrentNode();
      uiJCRExplorer.addLockToken(currentNode);
      if (rootTaxonomyName.equals(value)) {
        taxonomyService.addCategory(currentNode, rootTaxonomyName, "");
      } else {
        String[] arrayCategoryPath = String.valueOf(value.toString()).split("/");
        StringBuffer categoryPath = new StringBuffer().append("/");
        for(int i = 1; i < arrayCategoryPath.length; i++ ) {
          categoryPath.append(arrayCategoryPath[i]);
          categoryPath.append("/");
        }
        taxonomyService.addCategory(currentNode, rootTaxonomyName, categoryPath.toString());
      }
      uiJCRExplorer.getCurrentNode().save() ;
      uiJCRExplorer.getSession().save() ;
      updateGrid(1) ;
      setRenderSibling(UICategoriesAddedList.class) ;

      NodeLocation location = NodeLocation.getNodeLocationByNode(currentNode);
      WCMComposer composer = WCMCoreUtils.getService(WCMComposer.class);
      composer.updateContent(location.getWorkspace(),
                             location.getPath(),
                             new HashMap<String, String>());

    } catch (AccessDeniedException accessDeniedException) {
      throw new MessageException(new ApplicationMessage("AccessControlException.msg",
                                                        null,
                                                        ApplicationMessage.WARNING));
    } catch (ItemExistsException item) {
      throw new MessageException(new ApplicationMessage("UICategoriesAddedList.msg.ItemExistsException",
                                                        null,
                                                        ApplicationMessage.WARNING));
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
      JCRExceptionManager.process(getAncestorOfType(UIApplication.class), e);
    }
  }

  static public class DeleteActionListener extends EventListener<UICategoriesAddedList> {
    public void execute(Event<UICategoriesAddedList> event) throws Exception {
      UICategoriesAddedList uiAddedList = event.getSource();
      UIContainer uiManager = uiAddedList.getParent();
      UIApplication uiApp = uiAddedList.getAncestorOfType(UIApplication.class);
      String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIJCRExplorer uiExplorer = uiAddedList.getAncestorOfType(UIJCRExplorer.class);
      WCMComposer composer = WCMCoreUtils.getService(WCMComposer.class);
      Node currentNode = uiExplorer.getCurrentNode();
      TaxonomyService taxonomyService =
        uiAddedList.getApplicationComponent(TaxonomyService.class);
      List<Node> categories = taxonomyService.getAllCategories(currentNode);

      try {
        List<Node> listNode = uiAddedList.getAllTaxonomyTrees();
        for(Node itemNode : listNode) {
          if(nodePath.contains(itemNode.getPath())) {
            taxonomyService.removeCategory(currentNode, itemNode.getName(),
                nodePath.substring(itemNode.getPath().length()));
            break;
          }
        }
        uiAddedList.updateGrid(uiAddedList.getUIPageIterator().getCurrentPage());
        for (Node catnode : categories) {
          composer.updateContents(catnode.getSession().getWorkspace().getName(),
                                  catnode.getPath(),
                                  new HashMap<String, String>());
        }
      } catch(AccessDeniedException ace) {
        throw new MessageException(new ApplicationMessage("UICategoriesAddedList.msg.access-denied",
                                   null, ApplicationMessage.WARNING)) ;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        JCRExceptionManager.process(uiApp, e);
      }
      uiManager.setRenderedChild("UICategoriesAddedList");
    }
  }
}
