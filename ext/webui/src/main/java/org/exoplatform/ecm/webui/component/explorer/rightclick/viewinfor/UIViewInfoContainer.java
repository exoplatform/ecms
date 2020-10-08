/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.rightclick.viewinfor;

import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 3, 2010
 */
@ComponentConfig (
  template = "classpath:templates/viewinfo/UIViewInfoContainer.gtmpl",
  events = {
     @EventConfig(listeners = UIViewInfoContainer.CloseActionListener.class)
  }
)
public class UIViewInfoContainer extends UIContainer {

  /**
   * the hash map which contains information of node
   */
  private Map<String, String> inforMap;

  //constains
  private static final String NAME                      = "name";

  private static final String TITLE                     = "title";

  private static final String TYPE                      = "type";

  private static final String SIZE                      = "size";

  private static final String OWNER                     = "owner";

  private static final String LAST_MODIFIER             = "lastModifier";

  private static final String CREATED                   = "created";

  private static final String LAST_MODIFIED             = "lastModified";

  private static final String PUBLICATION_STATE         = "publicationState";

  private static final String DC_TITLE                  = "dc:title";

  private static final String PUBLICATION_CURRENT_STATE = "publication:currentState";

  private static final String EXO_LAST_MODIFIED_DATE    = "exo:lastModifiedDate";

  /**
   * checking selected node is a folder or not
   */
  private boolean isFolder = false;

  /**
   * constructor
   * @throws RepositoryException
   */
  public UIViewInfoContainer() throws RepositoryException
  {
    inforMap = new LinkedHashMap<String, String>();
  }

  /**
   * get inforMap value
   * @return inforMap value
   */
  public Map<String, String> getInforMap() {
    return inforMap;
  }

  /**
   * set value for inforMap
   * @param inforMap
   */
  public void setInforMap(Map<String, String> inforMap) {
    this.inforMap = inforMap;
  }

  /**
   * checking selected node is a folder or not
   * @return
   */
  public boolean isFolder() {
    return isFolder;
  }

  /**
   * read node properties, put value in to inforMap
   * @throws Exception
   */
  public void readNodeInformation() throws Exception {
    Node selectedNode = getSelectedNode();

    //get name
    inforMap.put(NAME, getName(selectedNode));

    //get title
    inforMap.put(TITLE, Utils.getTitle(selectedNode));

    //get Type
    inforMap.put(TYPE, getType(selectedNode));

    //get file size
    if (selectedNode.hasNode(Utils.JCR_CONTENT)) {
      Node contentNode = selectedNode.getNode(Utils.JCR_CONTENT);
      if (contentNode.hasProperty(Utils.JCR_DATA)) {
        double size = contentNode.getProperty(Utils.JCR_DATA).getLength();
        String fileSize = Utils.calculateFileSize(size);
        inforMap.put(SIZE, fileSize);
      }
    }

    //get owner
    if (selectedNode.hasProperty(Utils.EXO_OWNER)) {
      inforMap.put(OWNER, selectedNode.getProperty(Utils.EXO_OWNER).getString());
    }

    //get last modifier
    if (selectedNode.hasProperty(Utils.EXO_LASTMODIFIER)) {
      inforMap.put(LAST_MODIFIER, selectedNode.getProperty(Utils.EXO_LASTMODIFIER).getString());
    }

    //get created date
    if (selectedNode.hasProperty(Utils.EXO_CREATED_DATE)) {
      inforMap.put(CREATED, selectedNode.getProperty(Utils.EXO_CREATED_DATE).getString());
    }

    //get last modified date
    if (selectedNode.hasProperty(EXO_LAST_MODIFIED_DATE)) {
      inforMap.put(LAST_MODIFIED, selectedNode.getProperty(EXO_LAST_MODIFIED_DATE).getString());
    }

    //get publication state
    if (selectedNode.hasProperty(PUBLICATION_CURRENT_STATE)) {
      inforMap.put(PUBLICATION_STATE, selectedNode.getProperty(PUBLICATION_CURRENT_STATE).getString());
    }
  }

  /**
   * get type of node
   * @param node
   * @return type of node
   * @throws Exception
   */
  private String getType(Node node) throws Exception {
    TemplateService templateService = getApplicationComponent(TemplateService.class);
    NodeType nodeType = node.getPrimaryNodeType();
    String strNodeTypeName = nodeType.getName();
    String strType = "";
    isFolder = false;
    if (nodeType.isNodeType(Utils.NT_FILE)) { // is file that is uploaded by
                                              // user

      if (!node.isCheckedOut())
        node.checkout();
      Node contentNode = node.getNode(Utils.JCR_CONTENT);
      strType = contentNode.getProperty(Utils.JCR_MIMETYPE).getString();

    } else if (templateService.isManagedNodeType(strNodeTypeName)) { 
      // is Document which is created by user on web
      strType = templateService.getTemplateLabel(strNodeTypeName);
    } else if (nodeType.isNodeType(Utils.NT_UNSTRUCTURED) || nodeType.isNodeType(Utils.NT_FOLDER)) { 
      // is a folder
      isFolder = true;
    } else { // other
      strType = strNodeTypeName;
    }
    return strType;
  }

  /**
   * get title of node
   * @param node
   * @return
   * @throws Exception
   */
  private String getTitle(Node node) throws Exception {
    String title = null;
    if (node.hasNode(Utils.JCR_CONTENT)) {
      Node content = node.getNode(Utils.JCR_CONTENT);
      if (content.hasProperty(DC_TITLE)) {
        try {
          title = content.getProperty(DC_TITLE).getValues()[0].getString();
        } catch(Exception ex) {
          title = null;
        }
      }
    } else if (node.hasProperty(Utils.EXO_TITLE)) {
      title = node.getProperty(Utils.EXO_TITLE).getValue().getString();
    }
    if ((title==null) || ((title!=null) && (title.trim().length()==0))) {
      title = node.getName();
    }
    return Text.unescapeIllegalJcrChars(title);
  }

  /**
   * get name of node
   * @param node
   * @return
   * @throws Exception
   */
  private String getName(Node node) throws Exception {
    String name;
    name = node.getProperty("exo:name").getValue().getString();
    try {
      name = node.getProperty("exo:name").getValue().getString();
    } catch (PathNotFoundException pnf1) {
      try {
        Value[] values = node.getNode("jcr:content").getProperty("dc:title").getValues();
        if (values.length != 0) {
          name = values[0].getString();
        }
      } catch (PathNotFoundException pnf2) {
        name = null;
      }
    } catch (ValueFormatException e) {
      name = null;
    } catch (IllegalStateException e) {
      name = null;
    } catch (RepositoryException e) {
      name = null;
    }
    // double decode name for cyrillic chars
    name = URLDecoder.decode(URLDecoder.decode(name, "UTF-8"), "UTF-8");
    return name;
  }

  /**
   * get name of action
   * @return
   */
  public String getCloseAction() {
    return  "Close";
  }

  /**
   * get selected node
   * @return
   */
  private Node getSelectedNode() {
    UIViewInfoManager uiManager = getParent();
    return uiManager.getSelectedNode();
  }

  /**
   * @author hai_lethanh
   * class used for handling Close event
   */
  static public class CloseActionListener extends EventListener<UIViewInfoContainer> {

    @Override
    public void execute(Event<UIViewInfoContainer> event) throws Exception {
      event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction() ;
    }

  }
}
