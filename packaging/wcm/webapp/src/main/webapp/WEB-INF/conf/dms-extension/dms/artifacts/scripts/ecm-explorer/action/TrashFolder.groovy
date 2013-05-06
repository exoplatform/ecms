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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;

/*
* 
*/
public class TrashFolderScript implements CmsScript {
  
  final static public String EXO_RESTOREPATH = "exo:restorePath";
  final static public String EXO_RESTORELOCATION = "exo:restoreLocation";
  final static public String EXO_RESTORE_WORKSPACE = "exo:restoreWorkspace";
  private RepositoryService repositoryService_;
  private SessionProviderService seProviderService_;
  
  public TrashFolderScript(RepositoryService repositoryService, SessionProviderService sessionProviderService) {
    repositoryService_ = repositoryService;
    seProviderService_ = sessionProviderService;
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;       
    String nodePath = (String)variables.get("nodePath");
		String workspace = (String)variables.get("srcWorkspace");
    String srcPath = (String)variables.get("srcPath");
    int eventType = ((Integer)variables.get("eventType")).intValue();
    int index = nodePath.indexOf(':');
    if (index > -1)
    	nodePath = nodePath.substring(index + 1);

    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    Session session;
    Node node;
    try{
      session = seProviderService_.getSystemSessionProvider(null).getSession(workspace, manageableRepository);
      node = (Node)session.getItem(nodePath);
    } catch(Exception e) {
      return;
    }
    if ((eventType & Event.NODE_ADDED) > 0) {
    	if (!node.isNodeType(EXO_RESTORELOCATION)) {
    		node.addMixin(EXO_RESTORELOCATION);    		
  			node.setProperty(EXO_RESTOREPATH, fixRestorePath(srcPath));
  			node.setProperty(EXO_RESTORE_WORKSPACE, workspace);
    		session.save();
    	}
    } 
  }
  
	private String fixRestorePath(String path) {
		int leftBracket = path.lastIndexOf('[');
		int rightBracket = path.lastIndexOf(']');
		if (leftBracket == -1 || rightBracket == -1 || 
				(leftBracket >= rightBracket)) return path;
		
		try {
			Integer.parseInt(path.substring(leftBracket+1, rightBracket));
		} catch (Exception ex) {
			return path;
		}
		return path.substring(0, leftBracket);
	}
  
  public void setParams(String[] params) {}

}