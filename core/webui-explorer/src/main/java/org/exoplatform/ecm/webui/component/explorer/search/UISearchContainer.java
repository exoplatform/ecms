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
package org.exoplatform.ecm.webui.component.explorer.search;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.tree.selectone.UIOneTaxonomySelector;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 27, 2006
 * 2:04:24 PM
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UISearchContainer extends UIContainer {

  final static public String METADATA_POPUP = "MetadataPopup" ;
  final static public String NODETYPE_POPUP = "NodeTypePopup" ;
  final static public String SAVEQUERY_POPUP = "SaveQueryPopup" ;
  final static public String CATEGORY_POPUP = "CategoryPopup" ;

  public UISearchContainer() throws Exception {
    addChild(UISimpleSearch.class, null, null);
    addChild(UIConstraintsForm.class, null, null).setRendered(false);
    UIPopupContainer popup = addChild(UIPopupContainer.class, null, METADATA_POPUP);
    popup.getChild(UIPopupWindow.class).setId(METADATA_POPUP + "_Popup");
  }

  public void initMetadataPopup(String fieldName) throws Exception {
    UIPopupContainer uiPopup = getChild(UIPopupContainer.class) ;
    uiPopup.getChild(UIPopupWindow.class).setId(fieldName + METADATA_POPUP) ;
    UISelectPropertyForm uiSelectForm = createUIComponent(UISelectPropertyForm.class, null, null) ;
    uiSelectForm.setFieldName(fieldName) ;
    uiPopup.getChild(UIPopupWindow.class).setShowMask(true);
    uiPopup.activate(uiSelectForm, 500, 450) ;
  }

  public void initNodeTypePopup() throws Exception {
    UIPopupContainer uiPopup = getChild(UIPopupContainer.class) ;
    uiPopup.getChild(UIPopupWindow.class).setId(NODETYPE_POPUP) ;
    UINodeTypeSelectForm uiSelectForm = createUIComponent(UINodeTypeSelectForm.class, null, null) ;
    uiPopup.getChild(UIPopupWindow.class).setShowMask(true);
    uiPopup.activate(uiSelectForm, 400, 400) ;
    uiSelectForm.setRenderNodeTypes() ;
  }

  public void initCategoryPopup() throws Exception {
    /* Get UIJCRExplorer object*/
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    /* Get repository name */
    String repository = uiExplorer.getRepositoryName();
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspaceName = dmsConfiguration.getConfig().getSystemWorkspace();
    NodeHierarchyCreator nodeHierarchyCreator = uiExplorer.getApplicationComponent(NodeHierarchyCreator.class);
    uiExplorer.setIsHidePopup(true);
    /* Create Category panel in Search function */
    UICategoryManagerSearch uiCategoryManagerSearch = uiExplorer.createUIComponent(UICategoryManagerSearch.class, null, null);
    UIOneTaxonomySelector uiOneTaxonomySelector = uiCategoryManagerSearch.getChild(UIOneTaxonomySelector.class);
    uiOneTaxonomySelector.setIsDisable(workspaceName, true);
    String rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
    Session session = uiExplorer.getSessionByWorkspace(workspaceName);
    Node rootTree = (Node) session.getItem(rootTreePath);
    NodeIterator childrenIterator = rootTree.getNodes();
    while (childrenIterator.hasNext()) {
      Node childNode = childrenIterator.nextNode();
      rootTreePath = childNode.getPath();
      break;
    }
    uiOneTaxonomySelector.setRootNodeLocation(repository, workspaceName, rootTreePath);
    uiOneTaxonomySelector.setExceptedNodeTypesInPathPanel(new String[] {Utils.EXO_SYMLINK});
    uiOneTaxonomySelector.init(uiExplorer.getSystemProvider());
    UIConstraintsForm uiConstraintsForm = findFirstComponentOfType(UIConstraintsForm.class);
    uiOneTaxonomySelector.setSourceComponent(uiConstraintsForm, new String[] {UIConstraintsForm.CATEGORY_TYPE});
    UIPopupContainer UIPopupContainer = getChild(UIPopupContainer.class);
    UIPopupContainer.getChild(UIPopupWindow.class).setId(CATEGORY_POPUP) ;
    UIPopupContainer.activate(uiCategoryManagerSearch, 650, 500);
  }

  public void initSaveQueryPopup(String statement, boolean isSimpleSearch, String queryType) throws Exception {
    UIPopupContainer uiPopup = getChild(UIPopupContainer.class) ;
    uiPopup.getChild(UIPopupWindow.class).setId(SAVEQUERY_POPUP) ;
    UISaveQueryForm uiSaveQueryForm = createUIComponent(UISaveQueryForm.class, null, null) ;
    uiSaveQueryForm.setStatement(statement) ;
    uiSaveQueryForm.setSimpleSearch(isSimpleSearch) ;
    uiSaveQueryForm.setQueryType(queryType) ;
    uiPopup.getChild(UIPopupWindow.class).setShowMask(true);
    uiPopup.activate(uiSaveQueryForm, 420, 200) ;
  }
}
