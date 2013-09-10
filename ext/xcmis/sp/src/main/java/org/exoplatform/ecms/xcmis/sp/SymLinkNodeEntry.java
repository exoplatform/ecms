/**
 *  Copyright (C) 2003-2010 eXo Platform SAS.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.ecms.xcmis.sp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.StorageException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
class SymLinkNodeEntry extends JcrNodeEntry
{

   private Node link;

   /**
    * @param link exo:symlink node
    * @param node target of symlink
    * @param storage CMIS storage
    * @throws RepositoryException if any JCR repository error occurs
    */
   public SymLinkNodeEntry(Node link, Node node, BaseJcrStorage storage) throws RepositoryException
   {
      super(node.getPath(), node.getSession().getWorkspace().getName(), storage);
      this.link = link;
   }

   @Override
   String getId()
   {
      try
      {
         return ((ExtendedNode)link).getIdentifier();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get objects's id ." + re.getMessage(), re);
      }
   }

   @Override
   void delete() throws StorageException
   {
      try
      {
         Session session = link.getSession();
         // NOTE target node is not removed.
         // Since if it was created in JCR in some way (we do nothing about it here)
         // it should be removed via JCR actions, etc.
         link.remove();
         session.save();
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable delete object. " + re.getMessage(), re);
      }
   }

   @Override
   Collection<JcrNodeEntry> getParents()
   {
      try
      {
         Set<JcrNodeEntry> parents = new HashSet<JcrNodeEntry>();
         Node parent = link.getParent();
         try
         {
            parents.add(storage.fromNode(parent));
         }
         catch (ObjectNotFoundException onfe)
         {
            // Ignore nodes with object not found.
         }
         return parents;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get object parent. " + re.getMessage(), re);
      }
   }

   @Override
   String path()
   {
      try
      {
         return link.getPath();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get JCR node path. " + re.getMessage(), re);
      }
   }

   @Override
   String getName()
   {
      try
      {
         return link.getName();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable object's name. " + re.getMessage(), re);
      }
   }
}
