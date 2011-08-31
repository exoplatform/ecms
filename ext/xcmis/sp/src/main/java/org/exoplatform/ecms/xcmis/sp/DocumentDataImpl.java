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

import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.UpdateConflictException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.TypeDefinition;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
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

   public DocumentDataImpl(JcrNodeEntry jcrEntry)
   {
      super(jcrEntry);
   }

   /**
    * {@inheritDoc}
    */
   public void cancelCheckout() throws VersioningException, UpdateConflictException, StorageException
   {
      try
      {
         String pwcId = getVersionSeriesCheckedOutId();
         PWC pwc = new PWC(entry.storage.getEntry(pwcId), this);
         pwc.delete();
      }
      catch (ObjectNotFoundException e)
      {
         throw new CmisRuntimeException("Unable cancel checkout. PWC does not exists. " + e.getMessage());
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
         Node node = entry.getNode();
         Session session = node.getSession();
         String id = getObjectId();
         String name = getName();
         TypeDefinition type = getTypeDefinition();
         // Create node in working copies storage.
         Node workingCopies =
            (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_WORKING_COPIES);
         Node workingCopyNode = workingCopies.addNode(id, "xcmis:workingCopy");
         Node pwcNode = workingCopyNode.addNode(name, type.getLocalName());
         if (!pwcNode.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
         {
            pwcNode.addMixin(JcrCMIS.CMIS_MIX_DOCUMENT);
         }
         if (pwcNode.canAddMixin(JcrCMIS.MIX_VERSIONABLE))
         {
            pwcNode.addMixin(JcrCMIS.MIX_VERSIONABLE);
         }

         // Wrap node.
         JcrNodeEntry pwcEntry = entry.storage.fromNode(pwcNode);
         pwcEntry.setValue(CmisConstants.OBJECT_TYPE_ID, type.getId());
         pwcEntry.setValue(CmisConstants.BASE_TYPE_ID, type.getBaseId().value());
         String userId = session.getUserID();
         pwcEntry.setValue(CmisConstants.CREATED_BY, userId);
         pwcEntry.setValue(CmisConstants.LAST_MODIFIED_BY, userId);
         Calendar cal = Calendar.getInstance();
         pwcEntry.setValue(CmisConstants.CREATION_DATE, cal);
         pwcEntry.setValue(CmisConstants.LAST_MODIFICATION_DATE, cal);
         pwcEntry.setValue(CmisConstants.VERSION_SERIES_ID, getVersionSeriesId());
         pwcEntry.setValue(CmisConstants.IS_LATEST_VERSION, true);
         pwcEntry.setValue(CmisConstants.IS_MAJOR_VERSION, false);
         pwcEntry.setValue(CmisConstants.VERSION_LABEL, StorageImpl.PWC_LABEL);
         pwcEntry.setValue(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, true);
         pwcEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, pwcEntry.getId());
         pwcEntry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, session.getUserID());
         pwcEntry.setValue("xcmis:latestVersionId", id);

         // copy content
         pwcEntry.setContentStream(getContentStream(null));

         // Copy the other properties from document.
         try
         {
            for (PropertyDefinition<?> propertyDefinition : type.getPropertyDefinitions())
            {
               String propertyId = propertyDefinition.getId();
               if (!CHECKOUT_SKIP.contains(propertyId))
               {
                  pwcEntry.setProperty(getProperty(propertyId));
               }
            }
         }
         catch (NameConstraintViolationException never)
         {
            // Should never happen.
            throw new CmisRuntimeException(never.getMessage(), never);
         }

         // Update current.
         entry.setValue(CmisConstants.IS_LATEST_VERSION, false);
         entry.setValue(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT, true);
         entry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID, pwcEntry.getId());
         entry.setValue(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY, userId);

         PWC pwc = new PWC(pwcEntry, this);
         pwc.save();
         return pwc;
      }
      catch (ObjectNotFoundException onfe)
      {
         throw new StorageException("Unable checkout. " + onfe.getMessage(), onfe);
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
      return entry.getContentStream(null);
   }

   /**
    * {@inheritDoc}
    */
   public ContentStream getContentStream(String streamId) throws IOException
   {
      ContentStream contentStream = entry.getContentStream(streamId);
      if (contentStream == null)
      {
         contentStream = RenditionManager.getInstance().getStream(this, streamId);
      }
      return contentStream;
   }

   /**
    * {@inheritDoc}
    */
   public String getContentStreamMimeType()
   {
      String mimeType = entry.getString(CmisConstants.CONTENT_STREAM_MIME_TYPE);
      if (mimeType == null)
      {
         // For objects which were created via non CMIS API.
         mimeType = entry.getString("jcr:content/jcr:mimeType");
      }
      return mimeType;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Calendar getCreationDate()
   {
      Calendar date = super.getCreationDate();
      if (date == null)
      {
         // For objects which were created via non CMIS API.
         date = entry.getDate(JcrCMIS.JCR_CREATED);
      }
      return date;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Calendar getLastModificationDate()
   {
      Calendar date = super.getLastModificationDate();
      if (date == null)
      {
         // For objects which were created via non CMIS API.
         date = entry.getDate("jcr:content/jcr:lastModified");
      }
      return date;
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
      return parents.iterator().next();
   }

   /**
    * {@inheritDoc}
    */
   public Collection<FolderData> getParents()
   {
      Collection<JcrNodeEntry> parentEntries = entry.getParents();
      Set<FolderData> parents = new HashSet<FolderData>(parentEntries.size());
      for (JcrNodeEntry parentEntry : parentEntries)
      {
         parents.add(new FolderDataImpl(parentEntry));
      }
      return parents;
   }

   /**
    * {@inheritDoc}
    */
   public String getVersionLabel()
   {
      return entry.getString(CmisConstants.VERSION_LABEL);
   }

   /**
    * {@inheritDoc}
    */
   public String getVersionSeriesCheckedOutBy()
   {
      return entry.getString(CmisConstants.VERSION_SERIES_CHECKED_OUT_BY);
   }

   /**
    * {@inheritDoc}
    */
   public String getVersionSeriesCheckedOutId()
   {
      return entry.getString(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID);
   }

   /**
    * {@inheritDoc}
    */
   public String getVersionSeriesId()
   {
      return entry.getString(CmisConstants.VERSION_SERIES_ID);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasContent()
   {
      return entry.hasContent();
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
      Boolean latest = entry.getBoolean(CmisConstants.IS_LATEST_VERSION);
      return latest == null ? true : latest;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isMajorVersion()
   {
      Boolean major = entry.getBoolean(CmisConstants.IS_MAJOR_VERSION);
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
      Boolean checkout = entry.getBoolean(CmisConstants.IS_VERSION_SERIES_CHECKED_OUT);
      return checkout == null ? false : checkout;
   }

   /**
    * {@inheritDoc}
    */
   public void setContentStream(ContentStream contentStream) throws IOException, VersioningException,
      UpdateConflictException, StorageException
   {
      entry.setContentStream(contentStream);
      save();
   }

   /**
    * @return length of content in bytes
    */
   protected long getContentStreamLength()
   {
      Long length = entry.getLong(CmisConstants.CONTENT_STREAM_LENGTH);
      if (length == null)
      {
         // For objects which were created via non CMIS API need calculate length.
         ContentStream contentStream = entry.getContentStream(null);
         length = contentStream == null ? 0L : contentStream.length();
      }
      return length;
   }

   /**
    * @return id of content stream if document has content and <code>null</code>
    *         otherwise
    */
   protected String getContentStreamId()
   {
      String contentId = entry.getContentStreamId();
      return contentId;
   }

   /**
    * @return content stream file name if document has content and
    *         <code>null</code> otherwise
    */
   public String getContentStreamFileName()
   {
      String contentFileName = entry.getContentStreamFileName();
      return contentFileName;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void delete() throws StorageException
   {
      entry.delete();
   }

   public String getObjectId()
   {
      return entry.getId();
   }
}
