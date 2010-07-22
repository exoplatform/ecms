/*
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

import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.utils.MimeType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * @author <a href="mailto:andrey00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
class JcrFile extends DocumentDataImpl
{

   public JcrFile(JcrNodeEntry jcrEntry, IndexListener indexListener, RenditionManager renditionManager)
   {
      super(jcrEntry, indexListener, renditionManager);
      try
      {
         if (jcrEntry.getType().isVersionable() && jcrEntry.getNode().canAddMixin(JcrCMIS.MIX_VERSIONABLE))
         {
            jcrEntry.getNode().addMixin(JcrCMIS.MIX_VERSIONABLE);
            jcrEntry.save();
         }
      }
      catch (Exception e)
      {
         throw new CmisRuntimeException(e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void cancelCheckout() throws StorageException
   {
      throw new CmisRuntimeException("Not implemented for not CMIS type.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public DocumentData checkin(boolean major, String checkinComment, Map<String, Property<?>> properties,
      ContentStream content, List<AccessControlEntry> acl, Collection<PolicyData> policies) throws StorageException
   {
      throw new CmisRuntimeException("Not implemented for not CMIS type.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public DocumentData checkout() throws VersioningException, StorageException
   {
      throw new CmisRuntimeException("Not implemented for not CMIS type.");
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Calendar getCreationDate()
   {
      try
      {
         return getNode().getProperty(JcrCMIS.JCR_CREATED).getDate();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get cteation date. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getVersionLabel()
   {
      return StorageImpl.LATEST_LABEL;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getVersionSeriesId()
   {
      try
      {
         return getNode().getProperty(JcrCMIS.JCR_VERSION_HISTORY).getString();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get version series ID. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setContentStream(ContentStream content) throws IOException, StorageException
   {
      try
      {
         // jcr:content
         Node contentNode = getNode().getNode(JcrCMIS.JCR_CONTENT);
         if (content != null)
         {
            MimeType mediaType = content.getMediaType();
            contentNode.setProperty(JcrCMIS.JCR_MIMETYPE, mediaType.getBaseType());
            if (mediaType.getParameter(CmisConstants.CHARSET) != null)
            {
               contentNode.setProperty(JcrCMIS.JCR_ENCODING, mediaType.getParameter(CmisConstants.CHARSET));
            }
            contentNode.setProperty(JcrCMIS.JCR_DATA, content.getStream()).getLength();
            contentNode.setProperty(JcrCMIS.JCR_LAST_MODIFIED, Calendar.getInstance());
         }
         else
         {
            contentNode.setProperty(JcrCMIS.JCR_MIMETYPE, "");
            contentNode.setProperty(JcrCMIS.JCR_ENCODING, (Value)null);
            contentNode.setProperty(JcrCMIS.JCR_DATA, new ByteArrayInputStream(new byte[0]));
         }
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable set content stream. " + re.getMessage(), re);
      }
      save();
   }

   protected void save() throws StorageException
   {
      jcrEntry.save();
      if (indexListener != null)
      {
         indexListener.updated(this);
      }
   }
}
