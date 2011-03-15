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
package org.exoplatform.services.jcr.ext.classify.impl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.ext.classify.NodeClassifyPlugin;

/*
 * Created by The eXo Platform SAS
 * Author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Apr 9, 2008
 */
public class AlphabetClassifyPlugin extends NodeClassifyPlugin {
  public void classifyChildrenNode(Node parent) throws Exception {
    Session session = parent.getSession();
    NodeIterator nodeIterator = parent.getNodes();
    while (nodeIterator.hasNext()) {
      Node child = nodeIterator.nextNode();
      char firstCharacter = child.getName().charAt(0);
      Node classifiedNode = null;
      try {
        classifiedNode = parent.getNode(Character.toString(firstCharacter));
      } catch (PathNotFoundException ex) {
        classifiedNode = parent.addNode(Character.toString(firstCharacter));
        session.save();
      }
      String srcPath = child.getPath();
      String destPath = classifiedNode.getPath() + "/" + child.getName();
      session.move(srcPath, destPath);
    }
    session.save();
  }
}
