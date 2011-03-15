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
package org.exoplatform.ecm.webui.component.explorer.popup.info ;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * September 19, 2006
 */
@ComponentConfig(
    template =  "app:/groovy/webui/component/explorer/UIViewRelationList.gtmpl",
    events = {@EventConfig(listeners = UIViewRelationList.CloseActionListener.class)}
  )

public class UIViewRelationList extends UIContainer{

  public UIViewRelationList() throws Exception { }

  public List<Node> getRelations() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    Session session = uiExplorer.getSession() ;
    List<Node> relations = new ArrayList<Node>() ;
    Value[] vals = null ;
    try {
     vals = uiExplorer.getCurrentNode().getProperty("exo:relation").getValues() ;
    }catch (Exception e) { return relations ;}
    for(Value val : vals) {
      String uuid = val.getString();
      Node node = session.getNodeByUUID(uuid) ;
      relations.add(node) ;
    }
    return relations ;
  }

  static public class CloseActionListener extends EventListener<UIViewRelationList> {
    public void execute(Event<UIViewRelationList> event) throws Exception {
    }
  }
}
