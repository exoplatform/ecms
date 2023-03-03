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

import org.exoplatform.commons.utils.QName;
import org.exoplatform.services.document.DCMetaData;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class OpenOfficeDocumentReader extends BaseDocumentReader
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.OpenOfficeDocumentReader");

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.document.DocumentReader#getMimeTypes()
    */
   public String[] getMimeTypes()
   {
      return new String[]{"application/vnd.oasis.opendocument.database", "application/vnd.oasis.opendocument.formula",
         "application/vnd.oasis.opendocument.graphics", "application/vnd.oasis.opendocument.presentation",
         "application/vnd.oasis.opendocument.spreadsheet", "application/vnd.oasis.opendocument.text"};
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.document.DocumentReader#getContentAsText(java.
    *      io.InputStream)
    */
   public String getContentAsText(InputStream is) throws IOException, DocumentReadException
   {
      if (is == null)
      {
         throw new IllegalArgumentException("InputStream is null.");
      }
      try
      {

         ZipInputStream zis = new ZipInputStream(is);
         try
         {
            ZipEntry ze = zis.getNextEntry();

            if (ze == null)
            {
               return "";
            }

            while (!ze.getName().equals("content.xml"))
            {
               ze = zis.getNextEntry();
            }

            OpenOfficeContentHandler contentHandler = new OpenOfficeContentHandler();
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);

            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            xmlReader.setContentHandler(contentHandler);
            xmlReader.parse(new InputSource(zis));
            return contentHandler.getContent();
         }
         finally
         {
            try
            {
               zis.close();
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
      catch (ParserConfigurationException e)
      {
         throw new DocumentReadException(e.getMessage(), e);
      }
      catch (SAXException e)
      {
         throw new DocumentReadException(e.getMessage(), e);
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

   public String getContentAsText(InputStream is, String encoding) throws IOException, DocumentReadException
   {
      // Ignore encoding
      return getContentAsText(is);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.document.DocumentReader#getProperties(java.io.
    *      InputStream)
    */
   public Properties getProperties(InputStream is) throws IOException, DocumentReadException
   {
      try
      {

         ZipInputStream zis = new ZipInputStream(is);
         try
         {
            ZipEntry ze = zis.getNextEntry();
            while (!ze.getName().equals("meta.xml"))
            {
               ze = zis.getNextEntry();
            }

            OpenOfficeMetaHandler metaHandler = new OpenOfficeMetaHandler();
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();

            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
            xmlReader.setContentHandler(metaHandler);
            xmlReader.parse(new InputSource(zis));
            return metaHandler.getProperties();
         }
         finally
         {
            zis.close();
         }
      }
      catch (ParserConfigurationException e)
      {
         throw new DocumentReadException(e.getMessage(), e);
      }
      catch (SAXException e)
      {
         throw new DocumentReadException(e.getMessage(), e);
      }
      finally
      {
         if (is != null)
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

   // --------------------------------------------< OpenOfficeContentHandler >

   private class OpenOfficeContentHandler extends DefaultHandler
   {

      private StringBuilder content;

      private boolean appendChar;

      public OpenOfficeContentHandler()
      {
         content = new StringBuilder();
         appendChar = false;
      }

      /**
       * Returns the text content extracted from parsed content.xml
       */
      public String getContent()
      {
         return content.toString();
      }

      @Override
      public void startElement(String namespaceURI, String localName, String rawName, Attributes atts)
         throws SAXException
      {
         if (rawName.startsWith("text:"))
         {
            appendChar = true;
            if (content.length() > 0)
            {
               content.append(' ');
            }
         }
      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException
      {
         if (appendChar)
         {
            content.append(ch, start, length);
         }
      }

      @Override
      public void endElement(java.lang.String namespaceURI, java.lang.String localName, java.lang.String qName)
         throws SAXException
      {
         appendChar = false;
      }
   }

   private class OpenOfficeMetaHandler extends DefaultHandler
   {

      private Properties props;

      private QName curPropertyName;

      private StringBuilder curPropertyValue;

      public OpenOfficeMetaHandler()
      {
         props = new Properties();
         curPropertyValue = new StringBuilder();
      }

      public Properties getProperties()
      {
         return props;
      }

      @Override
      public void startElement(String namespaceURI, String localName, String rawName, Attributes atts)
         throws SAXException
      {
         if (namespaceURI.equals(DCMetaData.DC_NAMESPACE))
         {
            curPropertyName = new QName(DCMetaData.DC_NAMESPACE, localName);
         }
      }

      @Override
      public void characters(char[] ch, int start, int length) throws SAXException
      {
         if (curPropertyName != null)
         {
            curPropertyValue.append(ch, start, length);
         }
      }

      @Override
      public void endElement(java.lang.String namespaceURI, java.lang.String localName, java.lang.String qName)
         throws SAXException
      {
         if (curPropertyName != null)
         {
            props.put(curPropertyName, curPropertyValue.toString());
            curPropertyValue = new StringBuilder();
            curPropertyName = null;
         }
      }
   }

}
