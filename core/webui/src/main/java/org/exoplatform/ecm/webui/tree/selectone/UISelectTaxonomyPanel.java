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
package org.exoplatform.ecm.webui.tree.selectone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.tree.UIBaseNodeTreeSelector;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */

@ComponentConfig(
    template =  "classpath:groovy/ecm/webui/tree/selectone/UISelectPathPanel.gtmpl",
    events = {
        @EventConfig(listeners = UISelectTaxonomyPanel.SelectActionListener.class)
    }
)
public class UISelectTaxonomyPanel extends UISelectPathPanel {
  private UIPageIterator uiPageIterator_;
  private static String TAXONOMY_TREE = "taxonomyTree";
  private String taxonomyTreePath = "";

  public String getTaxonomyTreePath() {
    return taxonomyTreePath;
  }

  public void setTaxonomyTreePath(String taxonomyTreePath) {
    this.taxonomyTreePath = taxonomyTreePath;
  }

  public UISelectTaxonomyPanel() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "UISelectPathIterate");
  }

  public String getPathTaxonomy() throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);
    return nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
  }

  public String getDisplayName(Node node) throws RepositoryException {
    return getAncestorOfType(UIOneTaxonomySelector.class).getTaxonomyLabel(node);
  }

  static public class SelectActionListener extends EventListener<UISelectTaxonomyPanel> {
    public void execute(Event<UISelectTaxonomyPanel> event) throws Exception {
      UISelectTaxonomyPanel uiSelectPathPanel = event.getSource();
      UIOneTaxonomySelector uiTaxonomySelector = uiSelectPathPanel.getParent();
      UITreeTaxonomyList uiTreeList = uiTaxonomySelector.getChild(UITreeTaxonomyList.class);
      UIContainer uiTreeSelector = uiSelectPathPanel.getParent();
      String value = event.getRequestContext().getRequestParameter(OBJECTID);
      String taxoTreeName = uiTreeList.getUIFormSelectBox(TAXONOMY_TREE).getValue();
      Node taxoTreeNode = uiTaxonomySelector.getTaxoTreeNode(taxoTreeName);
      String taxoTreePath = taxoTreeNode.getPath();
      value = value.replace(taxoTreePath, taxoTreeName);

      if (uiTreeSelector instanceof UIOneNodePathSelector) {
        if (!((UIOneNodePathSelector) uiTreeSelector).isDisable()) {
          StringBuffer sb = new StringBuffer();
          sb.append(((UIOneNodePathSelector) uiTreeSelector).getWorkspaceName())
            .append(":")
            .append(value);
          value = sb.toString();
        }
      }

      String returnField = ((UIBaseNodeTreeSelector)uiTreeSelector).getReturnFieldName();
      ((UISelectable)((UIBaseNodeTreeSelector)uiTreeSelector).getSourceComponent()).doSelect(returnField, value) ;

      if (uiTreeSelector instanceof UIOneNodePathSelector) {
        UIComponent uiComponent = uiTreeSelector.getParent();
        if (uiComponent instanceof UIPopupWindow) {
          ((UIPopupWindow)uiComponent).setShow(false);
          ((UIPopupWindow)uiComponent).setRendered(false);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiComponent);
        }
        UIComponent component = ((UIOneNodePathSelector) uiTreeSelector).getSourceComponent().getParent();
        if (component != null) {
          event.getRequestContext().addUIComponentToUpdateByAjax(component);
          return;
        }
      }
      if (uiTreeSelector instanceof UIOneTaxonomySelector) {
        UIComponent uiComponent = uiTreeSelector.getParent();
        if (uiComponent instanceof UIPopupWindow) {
          ((UIPopupWindow)uiComponent).setShow(false);
          ((UIPopupWindow)uiComponent).setRendered(false);
          event.getRequestContext().addUIComponentToUpdateByAjax(uiComponent);
        }
        UIComponent component = ((UIOneTaxonomySelector) uiTreeSelector).getSourceComponent().getParent();
        if (component != null) {
          event.getRequestContext().addUIComponentToUpdateByAjax(component);
        }
      }
    }
  }
}
