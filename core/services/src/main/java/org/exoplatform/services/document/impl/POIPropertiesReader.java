/**
 * Copyright (C) 2023 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
*/
package org.exoplatform.services.document.impl;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ooxml.POIXMLProperties.CoreProperties;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.document.DCMetaData;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.PrivilegedExceptionAction;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class POIPropertiesReader
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.POIPropertiesReader");

   private final Properties props = new Properties();

   public Properties getProperties()
   {
      return props;
   }

   /**
    * Metadata extraction from OLE2 documents (legacy MS office file formats)
    * 
    * @param is
    * @return
    * @throws IOException
    * @throws DocumentReadException
    */
   public Properties readDCProperties(final InputStream is) throws IOException, DocumentReadException
   {
      if (is == null)
      {
         throw new IllegalArgumentException("InputStream is null.");
      }
      @SuppressWarnings("serial")
      class POIRuntimeException extends RuntimeException
      {
         private Throwable ex;

         public POIRuntimeException(Throwable ex)
         {
            this.ex = ex;
         }

         public Throwable getException()
         {
            return ex;
         }
      }

      POIFSReaderListener readerListener = new POIFSReaderListener()
      {
         public void processPOIFSReaderEvent(final POIFSReaderEvent event)
         {

            PropertySet ps;
            try
            {
               ps = PropertySetFactory.create(event.getStream());

               if (ps instanceof SummaryInformation)
               {
                  SummaryInformation si = (SummaryInformation)ps;

                  if (si.getLastAuthor() != null && si.getLastAuthor().length() > 0)
                  {
                     props.put(DCMetaData.CONTRIBUTOR, si.getLastAuthor());
                  }
                  if (si.getComments() != null && si.getComments().length() > 0)
                  {
                     props.put(DCMetaData.DESCRIPTION, si.getComments());
                  }
                  if (si.getCreateDateTime() != null)
                  {
                     props.put(DCMetaData.DATE, si.getCreateDateTime());
                  }
                  if (si.getAuthor() != null && si.getAuthor().length() > 0)
                  {
                     props.put(DCMetaData.CREATOR, si.getAuthor());
                  }
                  if (si.getKeywords() != null && si.getKeywords().length() > 0)
                  {
                     props.put(DCMetaData.SUBJECT, si.getKeywords());
                  }
                  if (si.getLastSaveDateTime() != null)
                  {
                     props.put(DCMetaData.DATE, si.getLastSaveDateTime());
                  }
                  // if(docInfo.getProducer() != null)
                  // props.put(DCMetaData.PUBLISHER, docInfo.getProducer());
                  if (si.getSubject() != null && si.getSubject().length() > 0)
                  {
                     props.put(DCMetaData.SUBJECT, si.getSubject());
                  }
                  if (si.getTitle() != null && si.getTitle().length() > 0)
                  {
                     props.put(DCMetaData.TITLE, si.getTitle());
                  }

               }
            }
            catch (NoPropertySetStreamException e)
            {
               throw new POIRuntimeException(new DocumentReadException(e.getMessage(), e));
            }
            catch (UnsupportedEncodingException e)
            {
               throw new POIRuntimeException(new DocumentReadException(e.getMessage(), e));
            }
            catch (IOException e)
            {
               throw new POIRuntimeException(e);
            }
         }
      };

      try
      {
         final POIFSReader poiFSReader = new POIFSReader();
         poiFSReader.registerListener(readerListener, SummaryInformation.DEFAULT_STREAM_NAME);
         SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws Exception
            {
               poiFSReader.read(is);
               return null;
            }
         });
      }
      catch (POIRuntimeException e)
      {
         Throwable ex = e.getException();
         if (ex instanceof IOException)
         {
            throw (IOException)ex;
         }
         else
         {
            throw (DocumentReadException)ex;
         }
      } finally
      {
         if (is != null)
         {
            try
            {
               is.close();
            }
            catch (IOException e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
         }
      }

      return props;
   }

   /**
    * Metadata extraction from ooxml documents (MS 2007 office file formats)
    * 
    * @param documentProperties
    * @return
    * @throws IOException
    * @throws DocumentReadException
    */
   public Properties readDCProperties(POIXMLProperties documentProperties) throws IOException, DocumentReadException
   {

      CoreProperties coreProperties = documentProperties.getCoreProperties();

      Optional<String> lastModifiedBy = coreProperties.getUnderlyingProperties().getLastModifiedByProperty();

      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      df.setTimeZone(TimeZone.getDefault());

      if (lastModifiedBy.isPresent() && lastModifiedBy.get() != null && lastModifiedBy.get().length() > 0)
      {
         props.put(DCMetaData.CONTRIBUTOR, lastModifiedBy.get());
      }
      if (coreProperties.getDescription() != null && coreProperties.getDescription().length() > 0)
      {
         props.put(DCMetaData.DESCRIPTION, coreProperties.getDescription());
      }
      if (coreProperties.getCreated() != null)
      {
         try
         {
            Date d = df.parse(coreProperties.getUnderlyingProperties().getCreatedPropertyString());
            props.put(DCMetaData.DATE, d);
         }
         catch (ParseException e)
         {
            throw new DocumentReadException("Incorrect creation date: " + e.getMessage(), e);
         }
      }
      if (coreProperties.getCreator() != null && coreProperties.getCreator().length() > 0)
      {
         props.put(DCMetaData.CREATOR, coreProperties.getCreator());
      }
      if (coreProperties.getSubject() != null && coreProperties.getSubject().length() > 0)
      {
         props.put(DCMetaData.SUBJECT, coreProperties.getSubject());
      }
      if (coreProperties.getModified() != null)
      {
         try
         {
            Date d = df.parse(coreProperties.getUnderlyingProperties().getModifiedPropertyString());
            props.put(DCMetaData.DATE, d);
         }
         catch (ParseException e)
         {
            throw new DocumentReadException("Incorrect modification date: " + e.getMessage(), e);
         }
      }
      if (coreProperties.getSubject() != null && coreProperties.getSubject().length() > 0)
      {
         props.put(DCMetaData.SUBJECT, coreProperties.getSubject());
      }
      if (coreProperties.getTitle() != null && coreProperties.getTitle().length() > 0)
      {
         props.put(DCMetaData.TITLE, coreProperties.getTitle());
      }

      return props;
   }

}
