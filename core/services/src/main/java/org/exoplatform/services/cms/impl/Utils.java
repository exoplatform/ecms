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
package org.exoplatform.services.cms.impl;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.jcr.core.ExtendedNode;

/**
 * @author benjaminmestrallet
 */
public class Utils {
  
  public static Node makePath(Node rootNode, String path, String nodetype)
  throws PathNotFoundException, RepositoryException {
    return makePath(rootNode, path, nodetype, null);
  }

  @SuppressWarnings("unchecked")
  public static Node makePath(Node rootNode, String path, String nodetype, Map permissions)
  throws PathNotFoundException, RepositoryException {    
    String[] tokens = path.split("/") ;
    Node node = rootNode;
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      if(token.length() > 0) {
        if(node.hasNode(token)) {
          node = node.getNode(token) ;
        } else {
          node = node.addNode(token, nodetype);
          if (node.canAddMixin("exo:privilegeable")){
            node.addMixin("exo:privilegeable");
          }
          if(permissions != null){          
            ((ExtendedNode)node).setPermissions(permissions);
          }
        }      
      }
    }
    return node;
  }
  
  /**
   * Gets the title.
   * 
   * @param node the node
   * @return the title
   * @throws Exception the exception
   */
  public static String getTitle(Node node) throws Exception {
	  String title = null;
	  if (node.hasProperty("exo:title")) {
	  	title = node.getProperty("exo:title").getValue().getString();
	  } else if (node.hasNode("jcr:content")) {
		  Node content = node.getNode("jcr:content");
		  if (content.hasProperty("dc:title")) {
		    try {
		      title = content.getProperty("dc:title").getValues()[0].getString();
		    } catch(Exception ex) {}
		  }
	  }
	  if (title==null) {
	  	if (node.isNodeType("nt:frozenNode")){
	  		String uuid = node.getProperty("jcr:frozenUuid").getString();
	  		Node originalNode = node.getSession().getNodeByUUID(uuid);
	  		title = originalNode.getName();
	  	} else {
	  		title = node.getName();
	  	}
	  	
	  }
	  return Text.unescapeIllegalJcrChars(title);
  }
}