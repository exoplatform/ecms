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

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.UpdateConflictException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.utils.MimeType;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: DocumentDataImpl.java 1177 2010-05-25 12:03:35Z
 *          alexey.zavizionov@gmail.com $
 */
class DocumentDataImpl extends BaseObjectData implements DocumentData
{

   protected static final Set<String> CHECKOUT_SKIP = new HashSet<String>();

   static
   {
      CHECKOUT_SKIP.add(CmisConstants.NAME);
      CHECKOUT_SKIP.add(CmisConstants.OBJECT_ID);
      CHECKOUT_SKIP.add(CmisConstants.OBJECT_TYPE_ID);
      CHECKOUT_SKIP.add(CmisConstants.BASE_TYPE_ID);
      CHECKOUT_SKIP.add(CmisConstants.CREATED_BY);
      CHECKOUT_SKIP.add(CmisConstants.CREATION_DATE);
      CHECKOUT_SKIP.add(CmisConstants.LAST_MODIFIED_BY);
      CHECKOUT_SKIP.add(CmisConstants.LAST_MODIFICATION_DATE);
      CHECKOUT_SKIP.add(CmisConstants.CHANGE_TOKEN);
      CHECKOUT_SKIP.add(CmisConstants.IS_IMMUTABLE);
      CHECKOUT_SKIP.add(CmisConstants.VERSION_SERIES_ID);
      CHECKOUT_SKIP.add(CmisConstants.IS_LATEST_VERSION);
      CHECKOUT_SKIP.add(CmisConstants.IS_MAJOR_VERSION);
      CHECKOUT_SKIP.add(CmisConstants.IS_LATEST_MAJOR_VERSION);
      CHECKOUT_SKIP.add(CmisConstants.VERSION_LABEL);
      CHECKOUT_SKIP.add(CmisConstants.CHECKIN_COMMENT);
      CHECKOUT_SKIP.add(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT);
      CHECKOUT_SKIP.add(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID);
      CHECKOUT_SKIP.add(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY);
      CHECKOUT_SKIP.add(CmisConstants.CONTENT_STREAM_FILE_NAME);
      CHECKOUT_SKIP.add(CmisConstants.CONTENT_STREAM_ID);
      CHECKOUT_SKIP.add(CmisConstants.CONTENT_STREAM_LENGTH);
      CHECKOUT_SKIP.add(CmisConstants.CONTENT_STREAM_MIME_TYPE);
      CHECKOUT_SKIP.add("xcmis:latestVersionId");
   }

   protected final RenditionManager renditionManager;

   public DocumentDataImpl(JcrNodeEntry jcrEntry, IndexListener indexListener, RenditionManager renditionManager)
   {
      super(jcrEntry, indexListener);
      this.renditionManager = renditionManager;
   }

   /**
    * {@inheritDoc}
    */
   public void cancelCheckout() throws VersioningException, UpdateConflictException, StorageException
   {
      try
      {
         Session session = getNode().getSession();
         Node pwcNode = ((ExtendedSession)session).getNodeByIdentifier(getVersionSeriesCheckedOutId());
         PWC pwc = new PWC(new JcrNodeEntry(pwcNode), indexListener, renditionManager, this);
         pwc.delete();
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable cancel checkout. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public DocumentData checkin(boolean major, String checkinComment, Map<String, Property<?>> properties,
      ContentStream content, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws NameConstraintViolationException, UpdateConflictException, StorageException
   {
      // Will be overridden in PWC
      throw new CmisRuntimeException("Current object is not Private Working Copy.");
   }

   /**
    * {@inheritDoc}
    */
   public DocumentData checkout() throws VersioningException, UpdateConflictException, StorageException
   {
      if (isVersionSeriesCheckedOut())
      {
         throw new VersioningException("Version series already checked-out. "
            + "Not allowed have more then one PWC for version series at a time.");
      }
      try
      {
         // create node
         Session session = getNode().getSession();
         String name = this.getName();
         TypeDefinition type = getTypeDefinition();
         Node workingCopies =
            (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_WORKING_COPIES);
         Node wc = workingCopies.addNode(this.getObjectId(), "xcmis:workingCopy");
         Node pwcNode = wc.addNode(name, type.getLocalName());
         if (!pwcNode.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
         {
            pwcNode.addMixin(JcrCMIS.CMIS_MIX_DOCUMENT);
         }
         if (pwcNode.canAddMixin(JcrCMIS.MIX_VERSIONABLE))
         {
            pwcNode.addMixin(JcrCMIS.MIX_VERSIONABLE);
         }

         JcrNodeEntry pwcNodeEntry = new JcrNodeEntry(pwcNode);

         pwcNodeEntry.setValue(CmisConstants.OBJECT_TYPE_ID, type.getId());
         pwcNodeEntry.setValue(CmisConstants.BASE_TYPE_ID, type.getBaseId().value());
         String userId = session.getUserID();
         pwcNodeEntry.setValue(CmisConstants.CREATED_BY, userId);
         pwcNodeEntry.setValue(CmisConstants.LAST_MODIFIED_BY, userId);
         Calendar cal = Calendar.getInstance();
         pwcNodeEntry.setValue(CmisConstants.CREATION_DATE, cal);
         pwcNodeEntry.setValue(CmisConstants.LAST_MODIFICATION_DATE, cal);
         pwcNodeEntry.setValue(CmisConstants.VERSION_SERIES_ID, this.getVersionSeriesId());
         pwcNodeEntry.setValue(CmisConstants.IS_LATEST_VERSION, true);
         pwcNodeEntry.setValue(CmisConstants.IS_MAJOR_VERSION, false);
         pwcNodeEntry.setValue(CmisConstants.VERSION_LABEL, StorageImpl.PWC_LABEL);
         pwcNodeEntry.setValue(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, true);
         pwcNodeEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, pwcNodeEntry.getId());
         pwcNodeEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, session.getUserID());
         pwcNodeEntry.setValue("xcmis:latestVersionId", this.getObjectId());

         // copy content
         pwcNodeEntry.setContentStream(getContentStream());

         // Copy the other properties from document.
         try
         {
            for (PropertyDefinition<?> def : type.getPropertyDefinitions())
            {
               String pId = def.getId();
               if (!CHECKOUT_SKIP.contains(pId))
               {
                  pwcNodeEntry.setProperty(this.getProperty(pId));
               }
            }
         }
         catch (NameConstraintViolationException never)
         {
            // Should never happen.
            throw new CmisRuntimeException(never.getMessage(), never);
         }

         // Update source document.
         jcrEntry.setValue(CmisConstants.IS_LATEST_VERSION, false);
         jcrEntry.setValue(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, true);
         jcrEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, pwcNodeEntry.getId());
         jcrEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, userId);

         session.save();

         // TODO : indexing of newly created PWC

         DocumentData pwc = new PWC(pwcNodeEntry, indexListener, renditionManager, this);
         return pwc;
      }
      catch (IOException ioe)
      {
         throw new CmisRuntimeException("Unable checkout. " + ioe.getMessage(), ioe);
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable checkout. " + re.getMessage(), re);
      }

   }

   /**
    * {@inheritDoc}
    */
   public ContentStream getContentStream() throws IOException
   {
      return jcrEntry.getContentStream();
   }

   /**
    * {@inheritDoc}
    */
   public ContentStream getContentStream(String streamId) throws IOException
   {
      if (streamId == null)
      {
         return getContentStream();
      }
      if (streamId.equals(jcrEntry.getString(CmisConstants.CONTENT_STREAM_ID)))
      {
         return getContentStream();
      }

      try
      {
         Node rendition = getNode().getNode(streamId);
         javax.jcr.Property renditionContent = rendition.getProperty(JcrCMIS.CMIS_RENDITION_STREAM);
         MimeType mimeType = MimeType.fromString(rendition.getProperty(JcrCMIS.CMIS_RENDITION_MIME_TYPE).getString());
         if (rendition.hasProperty(JcrCMIS.CMIS_RENDITION_ENCODING))
         {
            mimeType.getParameters().put(CmisConstants.CHARSET,
               rendition.getProperty(JcrCMIS.CMIS_RENDITION_ENCODING).getString());
         }

         return new BaseContentStream(renditionContent.getStream(), renditionContent.getLength(), null, mimeType);
      }
      catch (PathNotFoundException pnfe)
      {
         if (renditionManager != null)
         {
            return renditionManager.getStream(this, streamId);
         }
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get rendition stream. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getContentStreamMimeType()
   {
      String mimeType = jcrEntry.getString(CmisConstants.CONTENT_STREAM_MIME_TYPE);
      if (mimeType != null)
      {
         return mimeType;
      }
      try
      {
         return getNode().getProperty("jcr:content/jcr:mimeType").getString();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get content stream mime type. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public FolderData getParent() throws ConstraintException
   {
      Collection<FolderData> parents = getParents();
      if (parents.size() > 1)
      {
         throw new ConstraintException("Object has more then one parent.");
      }
      else if (parents.size() == 0)
      {
         return null;
      }
      else
      {
         return parents.iterator().next();
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
         Set<FolderData> parents = new HashSet<FolderData>();
         for (PropertyIterator iterator = node.getReferences(); iterator.hasNext();)
         {
            Node link = iterator.nextProperty().getParent();
            if (link.isNodeType("nt:linkedFile"))
            {
               Node parent = link.getParent();
               parents.add(new FolderDataImpl(new JcrNodeEntry(parent), indexListener, renditionManager));
            }
         }
         if (!node.getParent().isNodeType("xcmis:unfiledObject"))
         {
            Node parent = node.getParent();
            parents.add(new FolderDataImpl(new JcrNodeEntry(parent), indexListener, renditionManager));
         }
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
   public String getVersionLabel()
   {
      return jcrEntry.getString(CmisConstants.VERSION_LABEL);
   }

   /**
    * {@inheritDoc}
    */
   public String getVersionSeriesCheckedOutBy()
   {
      return jcrEntry.getString(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY);
   }

   /**
    * {@inheritDoc}
    */
   public String getVersionSeriesCheckedOutId()
   {
      return jcrEntry.getString(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID);
   }

   /**
    * {@inheritDoc}
    */
   public String getVersionSeriesId()
   {
      return jcrEntry.getString(CmisConstants.VERSION_SERIES_ID);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasContent()
   {
      return jcrEntry.hasContent();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLatestMajorVersion()
   {
      return isLatestVersion() && isMajorVersion();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isLatestVersion()
   {
      Boolean latest = jcrEntry.getBoolean(CmisConstants.IS_LATEST_VERSION);
      return latest == null ? true : latest;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMajorVersion()
   {
      Boolean major = jcrEntry.getBoolean(CmisConstants.IS_MAJOR_VERSION);
      return major == null ? false : major;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isPWC()
   {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isVersionSeriesCheckedOut()
   {
      Boolean checkout = jcrEntry.getBoolean(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT);
      return checkout == null ? false : checkout;
   }

   /**
    * {@inheritDoc}
    */
   public void setContentStream(ContentStream contentStream) throws IOException, VersioningException,
      UpdateConflictException, StorageException
   {
      jcrEntry.setContentStream(contentStream);
      save();
   }

   /**
    * @return length of content in bytes
    */
   protected long getContentStreamLength()
   {
      Long length = jcrEntry.getLong(CmisConstants.CONTENT_STREAM_LENGTH);
      if (length != null)
      {
         return length;
      }
      try
      {
         return getNode().getProperty("jcr:content/jcr:data").getLength();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get content stream length. " + re.getMessage(), re);
      }
   }

   protected void delete() throws StorageException
   {
      String objectId = getObjectId();
      try
      {
         Node node = getNode();
         Session session = node.getSession();
         // Check is Document node has any references.
         // It minds Document is multfiled, need remove all links first.
         for (PropertyIterator references = node.getReferences(); references.hasNext();)
         {
            Node next = references.nextProperty().getParent();
            if (next.isNodeType("nt:linkedFile"))
            {
               next.remove();
            }
         }
         String pwcId = getVersionSeriesCheckedOutId();
         if (pwcId != null)
         {
            // remove PWC
            Node pwcNode = ((ExtendedSession)session).getNodeByIdentifier(pwcId);
            pwcNode.getParent().remove();
         }
         if (getParents().size() == 0)
         {
            // Unfiled document. Remove node with wrapper.
            node.getParent().remove();
         }
         else
         {
            node.remove();
         }
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

   void unfile()
   {
      jcrEntry.unfile();
      if (indexListener != null)
      {
         indexListener.updated(this);
      }
   }
}
