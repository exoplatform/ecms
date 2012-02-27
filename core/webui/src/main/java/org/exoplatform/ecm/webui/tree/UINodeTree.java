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
package org.exoplatform.ecm.webui.tree;

import java.util.MissingResourceException;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIRightClickPopupMenu;
import org.exoplatform.webui.core.UITree;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */

@ComponentConfig(
    template = "system:/groovy/webui/core/UITree.gtmpl" ,
    events = @EventConfig(listeners = UITree.ChangeNodeActionListener.class)
  )
  /**
   * This class extend <code>org.exoplatform.webui.core.UITree</code>
   * to render node tree for <code>javax.jcr.Node</code>
   *
   * */
public class UINodeTree extends UITree {

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(UINodeTree.class);
  
  private String rootPath = "";

  private boolean isTaxonomyLocalize;

  public boolean isTaxonomyLocalize() {
    return isTaxonomyLocalize;
  }

  public void setTaxonomyLocalize(boolean isTaxonomyLocalize) {
    this.isTaxonomyLocalize = isTaxonomyLocalize;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  /*
   * render nodetype icon for node in tree
   * @see org.exoplatform.webui.core.UITree#renderNode(java.lang.Object)
   */
  public String renderNode(Object obj) throws Exception {
    Node node = (Node) obj;
    String nodeTypeIcon = Utils.getNodeTypeIcon(node,"16x16Icon");

    String nodeIcon = this.getExpandIcon();
    String iconGroup = this.getIcon();
    String note = "" ;
    if(isSelected(obj)) {
      nodeIcon = getColapseIcon();
      iconGroup = getSelectedIcon();
      note = " NodeSelected" ;
    }
    String beanIconField = getBeanIconField();
    if(beanIconField != null && beanIconField.length() > 0) {
      if(getFieldValue(obj, beanIconField) != null)
        iconGroup = (String)getFieldValue(obj, beanIconField);
    }
    String objId = Utils.formatNodeName(String.valueOf(getId(obj)));
    String actionLink = event("ChangeNode", objId);
    StringBuilder builder = new StringBuilder();
    if(nodeIcon.equals(getExpandIcon())) {
      builder.append(" <a class=\"")
             .append(nodeIcon)
             .append(" ")
             .append(nodeTypeIcon)
             .append("\" href=\"")
             .append(actionLink)
             .append("\">");
    } else {
      builder.append(" <a class=\"")
             .append(nodeIcon)
             .append(" ")
             .append(nodeTypeIcon)
             .append("\" onclick=\"eXo.portal.UIPortalControl.collapseTree(this)")
             .append("\">");
    }
    UIRightClickPopupMenu popupMenu = getUiPopupMenu();
    String beanFieldValue = getDisplayFieldValue(obj);
    beanFieldValue = Text.unescapeIllegalJcrChars(Utils.getIndexName(node));
    String className="NodeIcon";
    boolean flgSymlink = false;
    if (Utils.isSymLink(node)) {
      flgSymlink = true;
      className = "NodeIconLink";
    }
    if(popupMenu == null) {
      builder.append(" <div class=\"").append(className).append(" ").append(iconGroup).append(" ").append(nodeTypeIcon)
          .append(note).append("\"").append(" title=\"").append(beanFieldValue)
          .append("\"").append(">");
      if (flgSymlink) {
        builder.append("  <div class=\"LinkSmall\">")
          .append(beanFieldValue)
          .append("</div>");
      } else {
        builder.append(beanFieldValue);
      }
      builder.append("</div>");
    } else {
      builder.append(" <div class=\"").append(className).append(" ").append(iconGroup).append(" ").append(nodeTypeIcon)
          .append(note).append("\" ").append(popupMenu.getJSOnclickShowPopup(objId, null)).append(
              " title=\"").append(beanFieldValue).append("\"").append(">");
      if (flgSymlink) {
        builder.append("  <div class=\"LinkSmall\">")
          .append(beanFieldValue)
          .append("</div>");
      } else {
        builder.append(beanFieldValue);
      }
      builder.append("</div>");
    }
    builder.append(" </a>");
    return builder.toString();
  }

  private String getDisplayFieldValue(Object bean) throws Exception{
    if (isTaxonomyLocalize && Node.class.isInstance(bean)) {
      String path = ((Node)bean).getPath();
      String taxonomyTreeName = rootPath.substring(rootPath.lastIndexOf("/") + 1);
      try {
        String display = taxonomyTreeName.concat(path.replace(rootPath, "")).replaceAll("/", ".");
        return Utils.getResourceBundle(("eXoTaxonomies.").concat(display).concat(".label"));
      } catch (MissingResourceException me) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(me.getMessage());
        }
      }
    }
    return String.valueOf(getFieldValue(bean, getBeanLabelField()));
  }

  public boolean isSelected(Object obj) throws Exception {
    Node selectedNode = this.getSelected();
    Node node = (Node) obj;
    if(selectedNode == null) return false;
    return selectedNode.getPath().equals(node.getPath());
  }
}
