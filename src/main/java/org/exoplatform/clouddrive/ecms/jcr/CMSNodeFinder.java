/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.clouddrive.ecms.jcr;

import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.impl.NodeFinderImpl;
import org.exoplatform.services.jcr.RepositoryService;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Node finder based on original implementation from ECMS.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CMSNodeFinder.java 00000 Feb 26, 2013 pnedonosko $
 * 
 */
public class CMSNodeFinder extends NodeFinderImpl implements NodeFinder {

  public CMSNodeFinder(RepositoryService repositoryService, LinkManager linkManager) {
    super(repositoryService, linkManager);
  }
  
  /**
   * {@inheritDoc}
   */
  public String cleanName(String name) {
    // Align name to ECMS conventions
    int extIndex = name.lastIndexOf('.');
    String fileName;
    if (extIndex >= 0) {
      fileName = name.substring(0, extIndex);
    } else {
      fileName = name;
    }
    String jcrName = Text.escapeIllegalJcrChars(org.exoplatform.services.cms.impl.Utils.cleanString(fileName));
    if (extIndex >= 0) {
      jcrName += name.substring(extIndex);
    }
    return jcrName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Item findItem(Session session, String absPath) throws PathNotFoundException, RepositoryException {
    return getItem(session, absPath, true);
  }
}
