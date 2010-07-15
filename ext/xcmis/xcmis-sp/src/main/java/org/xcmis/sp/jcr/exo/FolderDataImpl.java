/**
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.xcmis.sp.jcr.exo;

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.TypeDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: FolderDataImpl.java 1160 2010-05-21 17:06:16Z
 *          alexey.zavizionov@gmail.com $
 */
class FolderDataImpl extends BaseObjectData implements FolderData
{
   static final Set<String> SKIP_CHILD_ITEMS = new HashSet<String>();

   static
   {
      SKIP_CHILD_ITEMS.add("jcr:system");
      SKIP_CHILD_ITEMS.add("xcmis:system");
   }

   private class FolderChildrenIterator implements ItemsIterator<ObjectData>
   {

      /** JCR node iterator. */
      protected final NodeIterator iter;

      /** Next CMIS item instance. */
      protected ObjectData next;

      private final IndexListener indexListener;

      /**
       * @param iter back-end NodeIterator
       * @param indexListener the index listener
       */
      FolderChildrenIterator(NodeIterator iter, IndexListener indexListener)
      {
         this.iter = iter;
         this.indexListener = indexListener;
         fetchNext();
      }

      /**
       * {@inheritDoc}
       */
      public boolean hasNext()
      {
         return next != null;
      }

      /**
       * {@inheritDoc}
       */
      public ObjectData next()
      {
         if (next == null)
         {
            throw new NoSuchElementException();
         }
         ObjectData n = next;
         fetchNext();
         return n;
      }

      /**
       * {@inheritDoc}
       */
      public void remove()
      {
         throw new UnsupportedOperationException("remove");
      }

      /**
       * {@inheritDoc}
       */
      public int size()
      {
         return -1;
      }

      /**
       * {@inheritDoc}
       */
      public void skip(int skip) throws NoSuchElementException
      {
         while (skip-- > 0)
         {
            fetchNext();
            if (next == null)
            {
               throw new NoSuchElementException();
            }
         }
      }

      /**
       * To fetch next item.
       */
      protected void fetchNext()
      {
         next = null;
         while (next == null && iter.hasNext())
         {
            Node node = iter.nextNode();
            try
            {
               if (SKIP_CHILD_ITEMS.contains(node.getName()))
               {
                  continue;
               }

               if (!((NodeImpl)node).isValid())
               {
                  continue; // TODO temporary
               }

               if (node.isNodeType("nt:linkedFile"))
               {
                  node = node.getProperty("jcr:content").getNode();
               }
               else if (node.isNodeType("xcmis:unfiledObject"))
               {
                  NodeIterator child = node.getNodes();
                  if (child.hasNext())
                  {
                     node = child.nextNode();
                  }
               }

               TypeDefinition type = JcrTypeHelper.getTypeDefinition(node.getPrimaryNodeType(), true);

               if (type.getBaseId() == BaseType.DOCUMENT)
               {
                  if (!node.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
                  {
                     next = new JcrFile(new JcrNodeEntry(node, type), indexListener, renditionManager);
                  }
                  else
                  {
                     next = new DocumentDataImpl(new JcrNodeEntry(node, type), indexListener, renditionManager);
                  }
               }
               else if (type.getBaseId() == BaseType.FOLDER)
               {
                  if (!node.isNodeType(JcrCMIS.CMIS_MIX_FOLDER))
                  {
                     next = new JcrFolder(new JcrNodeEntry(node, type), indexListener, renditionManager);
                  }
                  else
                  {
                     next = new FolderDataImpl(new JcrNodeEntry(node, type), indexListener, renditionManager);
                  }
               }
            }
            catch (NotSupportedNodeTypeException iae)
            {
               if (LOG.isDebugEnabled())
               {
                  // Show only in debug mode. It may cause a lot of warn when
                  // unsupported by xCMIS nodes met.
                  LOG.warn("Unable get next object . " + iae.getMessage());
               }
            }
            catch (javax.jcr.RepositoryException re)
            {
               LOG.warn("Unexpected error. Failed get next CMIS object. " + re.getMessage());
            }
         }
      }
   }

   private static final Log LOG = ExoLogger.getLogger(FolderDataImpl.class);

   protected final RenditionManager renditionManager;

   public FolderDataImpl(JcrNodeEntry jcrEntry, IndexListener indexListener, RenditionManager renditionManager)
   {
      super(jcrEntry, indexListener);
      this.renditionManager = renditionManager;
   }

   /**
    * {@inheritDoc}
    */
   public void addObject(ObjectData object) throws ConstraintException
   {
      try
      {
         Node node = getNode();
         Session session = node.getSession();
         Node add = ((BaseObjectData)object).getNode();
         if (add.getParent().isNodeType("xcmis:unfiledObject"))
         {
            // Object is in unfiled store. Move object in current folder.
            Node unfiled = add.getParent();
            String dataName = add.getName();
            String destPath = node.getPath();
            destPath += destPath.equals("/") ? dataName : ("/" + dataName);

            session.move(add.getPath(), destPath);

            // Remove unnecessary wrapper.
            unfiled.remove();
         }
         else
         {
            // Object (real object) is in some folder in repository.
            // Add link in current folder.
            Node link = node.addNode(object.getName(), "nt:linkedFile");
            link.setProperty("jcr:content", add);
         }

         session.save();
         if (indexListener != null)
         {
            indexListener.updated(object);
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable add object to current folder. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<ObjectData> getChildren(String orderBy)
   {
      if (LOG.isDebugEnabled())
      {
         LOG.debug("Get children " + getObjectId() + ", name " + getName());
      }

      try
      {
         return new FolderChildrenIterator(getNode().getNodes(), indexListener);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get children for folder " + getObjectId() + ". " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ContentStream getContentStream(String streamId)
   {
      // TODO : renditions for Folder object.
      // It may be XML or HTML representation direct child or full tree.
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public FolderData getParent() throws ConstraintException
   {
      try
      {
         Node node = getNode();
         if (node.getDepth() == 0)
         {
            throw new ConstraintException("Unable get parent of root folder.");
         }
         Node parent = node.getParent();
         return new FolderDataImpl(new JcrNodeEntry(parent), indexListener, renditionManager);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get object parent. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection<FolderData> getParents()
   {
      try
      {
         Node node = getNode();
         if (node.getDepth() == 0)
         {
            return Collections.emptyList();
         }
         Node parent = node.getParent();
         List<FolderData> parents = new ArrayList<FolderData>(1);
         parents.add(new FolderDataImpl(new JcrNodeEntry(parent), indexListener, renditionManager));
         return parents;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get object parent. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getPath()
   {
      return jcrEntry.getPath();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasChildren()
   {
      try
      {
         // Weak solution. Even this method return true iterator over children may
         // be empty if folder contains only not CMIS object.
         return getNode().hasNodes();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unexpected error. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isAllowedChildType(String typeId)
   {
      String[] values = jcrEntry.getStrings(CmisConstants.ALLOWED_CHILD_OBJECT_TYPE_IDS);
      if (values != null && values.length > 0 && !Arrays.asList(values).contains(typeId))
      {
         return false;
      }
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isRoot()
   {
      try
      {
         return getNode().getDepth() == 0;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unexpected error. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeObject(ObjectData object)
   {
      try
      {
         Node remove = ((BaseObjectData)object).getNode();

         Node node = getNode();
         Session session = node.getSession();

         if (((ExtendedNode)remove.getParent()).getIdentifier().equals(((ExtendedNode)node).getIdentifier()))
         {
            // Node 'data' is filed in current folder directly.
            // Check links from other folders.
            Node link = null;
            for (PropertyIterator references = remove.getReferences(); references.hasNext();)
            {
               Node next = references.nextProperty().getParent();
               if (next.isNodeType("nt:linkedFile"))
               {
                  link = next;
                  break; // Get a first one which met.
               }
            }

            // Determine where we should place object.
            String destPath;
            if (link != null)
            {
               // At least one link (object filed in more then one folder) exists.
               // Replace founded link by original object.
               destPath = link.getPath();
               link.remove();
            }
            else
            {
               // There is no any links for this node in other folders.
               // Move this node in unfiled store.
               Node unfiledStore =
                  (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_UNFILED);
               Node unfiled = unfiledStore.addNode(object.getObjectId(), "xcmis:unfiledObject");
               destPath = unfiled.getPath() + "/" + remove.getName();
            }

            // Move object node from current folder.
            session.move(remove.getPath(), destPath);
         }
         else
         {
            // Need find link in current folder.
            for (PropertyIterator references = remove.getReferences(); references.hasNext();)
            {
               Node next = references.nextProperty().getParent();
               if (next.isNodeType("nt:linkedFile")
                  && ((ExtendedNode)next.getParent()).getIdentifier().equals(((ExtendedNode)node).getIdentifier()))
               {
                  next.remove();
                  break;
               }
            }
         }

         session.save();
         if (indexListener != null)
         {
            indexListener.updated(object);
         }
      }
      catch (PathNotFoundException pe)
      {
         throw new InvalidArgumentException("Object " + object.getObjectId() + " is not filed in current folder.");
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable remove object from current folder. " + re.getMessage(), re);
      }
   }

   protected void delete() throws StorageException
   {
      if (isRoot())
      {
         throw new StorageException("Root folder can't be deleted.");
      }

      String objectId = getObjectId();
      try
      {
         Node node = getNode();
         Session session = node.getSession();
         node.remove();
         session.save();
      }
      catch (javax.jcr.ReferentialIntegrityException rie)
      {
         // TODO : Check is really ONLY relationships is in references.
         // Should raise StorageException if is not relationship reference.
         throw new StorageException("Object can't be deleted cause to storage referential integrity. "
            + "Probably this object is source or target at least one Relationship. "
            + "Those Relationship should be deleted before.");
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable delete object. " + re.getMessage(), re);
      }

      if (indexListener != null)
      {
         Set<String> removed = new HashSet<String>();
         removed.add(objectId);
         indexListener.removed(removed);
      }
   }

}
