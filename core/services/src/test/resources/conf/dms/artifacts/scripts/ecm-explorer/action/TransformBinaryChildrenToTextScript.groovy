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

import java.util.Map;

import javax.jcr.Property;
import javax.jcr.Node ;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.RepositoryService;

/*
* Will need to get The MailService when it has been moved to exo-platform
*/
public class TransformBinaryChildrenToTextScript implements CmsScript {
  
  private DocumentReaderService readerService_;
  private RepositoryService repositoryService_ ;
  
  public TransformBinaryChildrenToTextScript(RepositoryService repositoryService, DocumentReaderService readerService) {  
    repositoryService_ = repositoryService ;
    readerService_ = readerService;
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;       
    String srcWorkspace = (String)context.get("srcWorkspace") ;
    String srcPath = (String)variables.get("srcPath") ;
    Node folderNode = null ;
    Session session = null ;
    try {
      session = repositoryService_.getRepository().getSystemSession(srcWorkspace);
      folderNode = (Node) session.getItem(srcPath);
    } catch(Exception e) {}
    try {
      NodeIterator iter = folderNode.getNodes();
      while(iter.hasNext()) {
        Node childNode = iter.nextNode();
        if("nt:file".equals(childNode.getPrimaryNodeType().getName())) {
          Node content = childNode.getNode("jcr:content");
          Property mime = content.getProperty("jcr:mimeType");
          if (!mime.getString().startsWith("text")) {          
            String text = readerService_.getContentAsText(mime.getString(), content
              .getProperty("jcr:data").getStream());
            Node file = null;           
            try {
              file = folderNode.getNode(childNode.getName() + ".txt");
            } catch (PathNotFoundException e) {
              file = folderNode.addNode(childNode.getName() + ".txt", "nt:file");
            }
            Node contentNode = file.addNode("jcr:content", "nt:resource");
            contentNode.setProperty("jcr:encoding", "UTF-8");
            contentNode.setProperty("jcr:mimeType", "text/html");    
            contentNode.setProperty("jcr:data", text);
            contentNode.setProperty("jcr:lastModified", new GregorianCalendar());                              
          }
        }        
      }
      folderNode.save();
      session.save();
    } catch (Exception e) {
      if(session !=null) {
        session.logout();
      }
      e.printStackTrace();
    }  
  }

  public void setParams(String[] params) {}

}