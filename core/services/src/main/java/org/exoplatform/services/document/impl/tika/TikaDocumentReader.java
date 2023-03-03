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
package org.exoplatform.services.document.impl.tika;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.MSOffice;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.OfficeOpenXMLCore;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParsingReader;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.xmlbeans.impl.common.SystemCache;
import org.exoplatform.commons.utils.QName;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.document.AdvancedDocumentReader;
import org.exoplatform.services.document.DCMetaData;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.HandlerNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TikaDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TikaDocumentReader implements AdvancedDocumentReader
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.TikaDocumentReader");

   private final String mimeType;

   private final Parser parser;
   
   private final Executor executor;

   public TikaDocumentReader(Parser tikaParser, String mimeType, Executor executor) throws HandlerNotFoundException
   {
      this.parser = tikaParser;
      this.mimeType = mimeType;
      this.executor = executor;
   }

   public Reader getContentAsReader(final InputStream is, final String encoding) throws IOException,
      DocumentReadException
   {
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Reader>()
         {

            public Reader run() throws Exception
            {
               Metadata metadata = new Metadata();
               metadata.set(Metadata.CONTENT_TYPE, mimeType);
               metadata.set(Metadata.CONTENT_ENCODING, encoding);
               ParseContext context = new ParseContext();
               context.set(Parser.class, parser);
               return new ParsingReader(parser, is, metadata, context, executor);
            }
         });
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IOException)
         {
            throw (IOException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   public Reader getContentAsReader(final InputStream is) throws IOException, DocumentReadException
   {
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Reader>()
         {

            public Reader run() throws Exception
            {
               Metadata metadata = new Metadata();
               metadata.set(Metadata.CONTENT_TYPE, mimeType);
               ParseContext context = new ParseContext();
               context.set(Parser.class, parser);
               return new ParsingReader(parser, is, metadata, context, executor);
            }
         });
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IOException)
         {
            throw (IOException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }

   }

   public String getContentAsText(final InputStream is) throws IOException, DocumentReadException
   {
      if (is.available() == 0)
      {
         return "";
      }
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<String>()
         {

            public String run() throws Exception
            {
               try
               {
                  Metadata metadata = new Metadata();
                  metadata.set(Metadata.CONTENT_TYPE, mimeType);

                  ContentHandler handler = new BodyContentHandler();
                  ParseContext context = new ParseContext();
                  context.set(Parser.class, parser);
                  // Workaround for XMLBEANS-512 - ensure that when we parse
                  //  the file, we start with a fresh XML Parser each time,
                  //  and avoid the risk of getting a SaxHandler that's in error
                  SystemCache.get().setSaxLoader(null);
                  try
                  {
                     parser.parse(is, handler, metadata, context);
                     return handler.toString();
                  }
                  catch (SAXException e)
                  {
                     throw new DocumentReadException(e.getMessage(), e);
                  }
                  catch (TikaException e)
                  {
                     throw new DocumentReadException(e.getMessage(), e);
                  }
               }
               finally
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
         });
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IOException)
         {
            throw (IOException)cause;
         }
         throw new DocumentReadException("Can not get the content: " + cause.getMessage(), cause);
      }
   }

   public String getContentAsText(final InputStream is, final String encoding) throws IOException,
      DocumentReadException
   {
      if (is.available() == 0)
      {
         return "";
      }
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<String>()
         {
            public String run() throws Exception
            {
               try
               {
                  Metadata metadata = new Metadata();
                  metadata.set(Metadata.CONTENT_TYPE, mimeType);
                  metadata.set(Metadata.CONTENT_ENCODING, encoding);

                  ContentHandler handler = new BodyContentHandler();
                  ParseContext context = new ParseContext();
                  context.set(Parser.class, parser);
                  try
                  {
                     parser.parse(is, handler, metadata, context);
                     return handler.toString();
                  }
                  catch (SAXException e)
                  {
                     throw new DocumentReadException(e.getMessage(), e);
                  }
                  catch (TikaException e)
                  {
                     throw new DocumentReadException(e.getMessage(), e);
                  }
               }
               finally
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
         });
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IOException)
         {
            throw (IOException)cause;
         }
         throw new DocumentReadException("Can not get the content: " + cause.getMessage(), cause);
      }
   }

   public String[] getMimeTypes()
   {
      return new String[]{mimeType};
   }

   public Properties getProperties(final InputStream is) throws IOException, DocumentReadException
   {
      if (is.available() == 0)
      {
         return new Properties();
      }
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Properties>()
         {

            @SuppressWarnings("deprecation")
            public Properties run() throws Exception
            {
               try
               {
                  Metadata metadata = new Metadata();
                  metadata.set(Metadata.CONTENT_TYPE, mimeType);

                  ContentHandler handler = new DefaultHandler();
                  ParseContext context = new ParseContext();
                  context.set(Parser.class, parser);
                  try
                  {
                     parser.parse(is, handler, metadata, context);
                  }
                  catch (SAXException e)
                  {
                     throw new DocumentReadException(e.getMessage(), e);
                  }
                  catch (TikaException e)
                  {
                     throw new DocumentReadException(e.getMessage(), e);
                  }

                  // construct Properties set
                  Properties props = new Properties();
                  convertProperty(metadata, props, DCMetaData.CONTRIBUTOR, new String[]{DublinCore.CONTRIBUTOR.getName(),
                     MSOffice.LAST_AUTHOR});
                  convertProperty(metadata, props, DCMetaData.COVERAGE, DublinCore.COVERAGE);
                  convertProperty(metadata, props, DCMetaData.CREATOR,
                     new String[]{MSOffice.AUTHOR, DublinCore.CREATOR.getName()});
                  // different parsers return date in different formats, so keep it as String
                  convertProperty(metadata, props, DCMetaData.DATE, new Property[]{DublinCore.DATE,
                     MSOffice.LAST_SAVED, MSOffice.CREATION_DATE});
                  convertProperty(metadata, props, DCMetaData.DESCRIPTION, new String[]{DublinCore.DESCRIPTION.getName(),
                     MSOffice.COMMENTS});
                  convertProperty(metadata, props, DCMetaData.FORMAT, DublinCore.FORMAT);
                  convertProperty(metadata, props, DCMetaData.IDENTIFIER, DublinCore.IDENTIFIER);
                  convertProperty(metadata, props, DCMetaData.LANGUAGE, DublinCore.LANGUAGE);
                  //convertProperty(metadata, props, DCMetaData.?, DublinCore.MODIFIED);
                  convertProperty(metadata, props, DCMetaData.PUBLISHER, DublinCore.PUBLISHER);
                  convertProperty(metadata, props, DCMetaData.RELATION, DublinCore.RELATION);
                  convertProperty(metadata, props, DCMetaData.RESOURCE, DublinCore.SOURCE);
                  convertProperty(metadata, props, DCMetaData.RIGHTS, DublinCore.RIGHTS);
                  convertProperty(metadata, props, DCMetaData.SUBJECT, new String[]{Metadata.SUBJECT,
                     OfficeOpenXMLCore.SUBJECT.getName(), DublinCore.SUBJECT.getName(), MSOffice.KEYWORDS});
                  convertProperty(metadata, props, DCMetaData.TITLE, DublinCore.TITLE);
                  convertProperty(metadata, props, DCMetaData.TYPE, DublinCore.TYPE);

                  return props;
               }
               finally
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
         });
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IOException)
         {
            throw (IOException)cause;
         }
         throw new DocumentReadException("Can not get properties: " + cause.getMessage(), cause);
      }
   }

   private void convertProperty(Metadata metadata, Properties props, QName jcrDCProp, Property tikaDCProp)
   {
      String value = (String)metadata.get(tikaDCProp);
      if (value != null)
      {
         props.put(jcrDCProp, value);
      }
   }

   /**
    * Test does Metadata contains property from tikaPropertyNames list. 
    * <p><b>Warning</b> - Order in tikaPropertyNames list is important. 
    * First property from list will be used as a result value.
    * 
    * @param metadata
    * @param props
    * @param jcrDCProp
    * @param tikaPropertyNames
    */
   private void convertProperty(Metadata metadata, Properties props, QName jcrDCProp, String[] tikaPropertyNames)
   {
      for (String propertyName : tikaPropertyNames)
      {
         String value = (String)metadata.get(propertyName);
         if (value != null)
         {
            props.put(jcrDCProp, value);
            return;
         }
      }
   }

   @SuppressWarnings("deprecation")
   private void convertProperty(Metadata metadata, Properties props, QName jcrDCProp, Property[] tikaProperty)
   {
      for (Property property : tikaProperty)
      {
         String value = (String)metadata.get(property);
         if (value != null)
         {
            if (property.equals(DublinCore.DATE) || property.equals(MSOffice.LAST_SAVED)
               || property.equals(MSOffice.CREATION_DATE))
            {
               props.put(jcrDCProp, metadata.getDate(property));
               return;
            }
            props.put(jcrDCProp, value);
            return;
         }
      }
   }
}
