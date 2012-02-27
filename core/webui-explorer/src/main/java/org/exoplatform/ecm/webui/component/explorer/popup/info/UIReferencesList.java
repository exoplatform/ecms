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
package org.exoplatform.ecm.webui.component.explorer.popup.info;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : nqhungvn
 *          nguyenkequanghung@yahoo.com
 * July 3, 2006
 * 10:07:15 AM
 * Edit: lxchiati 2006/10/16
 * Edit: phamtuan Oct 27, 2006
 */

@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/popup/info/UIReferencesList.gtmpl",
    events = { @EventConfig (listeners = UIReferencesList.CloseActionListener.class)}
)

public class UIReferencesList extends UIGrid implements UIPopupComponent{

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(UIReferencesList.class);
  
  private static String[] REFERENCES_BEAN_FIELD = {"workspace", "path"} ;

  public UIReferencesList() throws Exception {}

  public void activate() throws Exception {
    configure("workspace", REFERENCES_BEAN_FIELD, null) ;
    updateGrid() ;
  }

  public void deActivate() {}

  public void updateGrid() throws Exception {
    ListAccess<ReferenceBean> referenceList = new ListAccessImpl<ReferenceBean>(ReferenceBean.class,
                                                                                getReferences());
    LazyPageList<ReferenceBean> dataPageList = new LazyPageList<ReferenceBean>(referenceList, 10);
    getUIPageIterator().setPageList(dataPageList);
  }

  private List<ReferenceBean> getReferences() throws Exception {
    List<ReferenceBean> referBeans = new ArrayList<ReferenceBean>() ;
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class) ;
    UIJCRExplorer uiJCRExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    Node currentNode = uiJCRExplorer.getCurrentNode() ;
    String uuid = currentNode.getUUID() ;
    ManageableRepository repository = repositoryService.getCurrentRepository();
    Session session = null ;
    for(String workspace : repository.getWorkspaceNames()) {
      session = repository.getSystemSession(workspace) ;
      try{
        Node lookupNode = session.getNodeByUUID(uuid) ;
        PropertyIterator iter = lookupNode.getReferences() ;
        if(iter != null) {
          while(iter.hasNext()) {
            Node refNode = iter.nextProperty().getParent() ;
            referBeans.add(new ReferenceBean(workspace, refNode.getPath())) ;
          }
        }
      } catch(Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
     session.logout() ;
    }
    return referBeans ;
  }

  static public class CloseActionListener extends EventListener<UIReferencesList> {
    public void execute(Event<UIReferencesList> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }

  public class ReferenceBean {
    private String workspace_ ;
    private String path_ ;

    public ReferenceBean ( String workspace, String path) {
      workspace_ = workspace ;
      path_ = path ;
    }

    public String getPath() { return path_ ;}
    public void setPath(String path) { this.path_ = path ;}

    public String getWorkspace() { return workspace_ ;}
    public void setWorkspace(String workspace) { this.workspace_ = workspace ;}
  }
}
