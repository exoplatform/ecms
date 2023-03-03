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

import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by The eXo Platform SAS A parser of XML files.
 * 
 * @author <a href="mailto:zagrebin_v@mail.ru">Victor Zagrebin</a>
 * @version March 07, 2006
 */
public class XMLDocumentReader extends BaseDocumentReader
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.XMLDocumentReader");

   /**
    * Get the text/xml, application/xml, application/x-google-gadget mime types.
    * 
    * @return The string with text/xml,  application/xml, application/x-google-gadget mime type.
    */
   public String[] getMimeTypes()
   {
      return new String[]{"text/xml", "application/xml", "application/x-google-gadget"};
   }

   /**
    * Returns a text from xml file content which situated between tags.
    * 
    * @param is an input stream with html file content.
    * @return The string only with text from file content.
    */
   public String getContentAsText(InputStream is) throws IOException, DocumentReadException
   {
      if (is == null)
      {
         throw new IllegalArgumentException("InputStream is null.");
      }
      try
      {
         return parse(is);
      }
      catch (SAXException e) {
        throw new DocumentReadException("Problem during the document parsing.", e);
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
         is.close();
      }
      catch (IOException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }
      return new Properties();
   }

   /**
    * Cleans the string from tags.
    * 
    * @param str the string which contain a text with user's tags.
    * @return The string cleaned from user's tags and their bodies.
    */
   private String parse(InputStream is) throws SAXException
   {
      StringWriter writer = new StringWriter();

      DefaultHandler dh = new WriteOutContentHandler(writer);
      try
      {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        SAXParser parser = parserFactory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
         reader.setContentHandler(dh);
         reader.setErrorHandler(dh);
         reader.setDTDHandler(dh);
         reader.parse(new InputSource(is));
      }
      catch (IOException e)
      {
        throw new RuntimeException("Exception when reading inputFile - " + e.getMessage());
      }
      catch (ParserConfigurationException e)
      {
        throw new RuntimeException("SAX parser appears to be broken - " + e.getMessage());
      }

      return writer.toString();

   }

   class WriteOutContentHandler extends DefaultHandler
   {
      private final Writer writer;

      public WriteOutContentHandler(Writer writer)
      {
         this.writer = writer;
      }

      /**
       * Writes the given characters to the given character stream.
       */
      @Override
      public void characters(char[] ch, int start, int length) throws SAXException
      {
         try
         {
            writer.write(ch, start, length);
         }
         catch (IOException e)
         {
            throw new SAXException(e.getMessage(), e);
         }
      }

      @Override
      public void endDocument() throws SAXException
      {
         try
         {
            writer.flush();
         }
         catch (IOException e)
         {
            throw new SAXException(e.getMessage(), e);
         }
      }
   }

}
