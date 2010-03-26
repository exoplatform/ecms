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
package org.exoplatform.wcm.webui.selector.folder;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.selector.UISelectPathPanel;
import org.exoplatform.wcm.webui.selector.content.UIContentTreeBuilder;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author : maivanha1610@gmail.com
 */

@ComponentConfig(
                 template = "classpath:groovy/wcm/webui/selector/content/UIContentTreeBuilder.gtmpl",
                 events = @EventConfig(listeners = UIContentTreeBuilderFolder.ChangeNodeActionListener.class)
)
public class UIContentTreeBuilderFolder extends UIContentTreeBuilder {

  /**
   * Instantiates a new uI content tree builder folder.
   * 
   * @throws Exception the exception
   */
  public UIContentTreeBuilderFolder() throws Exception {
	  super();
  }

	/**
	 * The listener interface for receiving changeNodeAction events.
	 * The class that is interested in processing a changeNodeAction
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addChangeNodeActionListener<code> method. When
	 * the changeNodeAction event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see ChangeNodeActionEvent
	 */
  static public class ChangeNodeActionListener extends EventListener<UIContentTreeBuilderFolder> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentTreeBuilderFolder> event) throws Exception {
      UIContentTreeBuilder contentTreeBuilder = event.getSource();      
      String values = event.getRequestContext().getRequestParameter(OBJECTID);
      String path = values.substring(values.lastIndexOf("/") + 1);
      while(contentTreeBuilder.path.isEmpty()==false){
        if(path.contains(contentTreeBuilder.path.get(contentTreeBuilder.path.size() - 1) + "rp")){
          contentTreeBuilder.path.add(path);
          break;
        }else contentTreeBuilder.path.remove(contentTreeBuilder.path.size() - 1);
      }
      if(contentTreeBuilder.path.isEmpty())contentTreeBuilder.path.add(path);
      values = values.substring(0, values.lastIndexOf("/"));
      String workSpaceName = values.substring(values.lastIndexOf("/") + 1);
      String nodePath = values.substring(0, values.lastIndexOf("/"));
      ManageableRepository manageableRepository = contentTreeBuilder.getApplicationComponent(RepositoryService.class).getDefaultRepository();
      SessionProvider sessionProvider = Utils.getSessionProvider();
      Session session = sessionProvider.getSession(workSpaceName, manageableRepository);
      Node rootNode = (Node)session.getItem(nodePath);
      UISelectPathPanel selectPathPanel = contentTreeBuilder.getAncestorOfType(UIContentBrowsePanelFolder.class).getChild(UISelectPathPanelFolder.class);
      selectPathPanel.setParentNode(rootNode);
      selectPathPanel.updateGrid();
      event.getRequestContext().addUIComponentToUpdateByAjax(selectPathPanel);
    }
  }
}
