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

import org.apache.commons.io.IOUtils;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by The eXo Platform SAS A parser of Microsoft PowerPoint 2007 files (pptx).
 * 
 * @author <a href="mailto:phunghainam@gmail.com">Phung Hai Nam</a>
 * @author Gennady Azarenkov
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: MSXPPTDocumentReader.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 */
public class MSXPPTDocumentReader extends BaseDocumentReader
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.MSXPPTDocumentReader");

   private static final String URI_CORE_PROPERTIES = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties";
   private static final String URI_DC_TERMS = "http://purl.org/dc/terms/";
   private static final String PPTX_SLIDE_PREFIX = "ppt/slides/slide";

   private static final String PPTX_CORE_NAME = "docProps/core.xml";

   private static final int MAX_SLIDES = 500;

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.document.DocumentReader#getMimeTypes()
    */
   public String[] getMimeTypes()
   {
      return new String[]{"application/vnd.openxmlformats-officedocument.presentationml.presentation",
         "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
         "application/vnd.ms-powerpoint.presentation.macroenabled.12",
         "application/vnd.ms-powerpoint.slideshow.macroenabled.12"};
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.exoplatform.services.document.DocumentReader#getContentAsText(java.
    *      io.InputStream)
    */
   public String getContentAsText(InputStream is) throws IOException, DocumentReadException
   {
      return getContentAsText(is, MAX_SLIDES);
   }

   /**
    * Extracts the text content of the n first slides 
    */
   public String getContentAsText(InputStream is, int maxSlides) throws IOException, DocumentReadException
   {
      if (is == null)
      {
         throw new IllegalArgumentException("InputStream is null.");
      }
      StringBuilder appendText = new StringBuilder();
      try
      {

         int slideCount = 0;
         ZipInputStream zis = new ZipInputStream(is);

         try
         {
            ZipEntry ze = zis.getNextEntry();

            if (ze == null)
               return "";

             SAXParserFactory parserFactory = SAXParserFactory.newInstance();
             parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
             SAXParser parser = parserFactory.newSAXParser();
             XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            Map<Integer, String> slides = new TreeMap<Integer, String>();
            // PPTX: ppt/slides/slide<slide_no>.xml
            while (ze != null && slideCount < maxSlides)
            {
               String zeName = ze.getName();
               if (zeName.startsWith(PPTX_SLIDE_PREFIX) && zeName.length() > PPTX_SLIDE_PREFIX.length())
               {
                  String slideNumberStr = zeName.substring(PPTX_SLIDE_PREFIX.length(), zeName.indexOf(".xml"));
                  int slideNumber = -1;
                  try
                  {
                     slideNumber = Integer.parseInt(slideNumberStr);
                  }
                  catch (NumberFormatException e)
                  {
                     LOG.warn("Could not parse the slide number: " + e.getMessage());
                  }
                  if (slideNumber > -1 && slideNumber <= maxSlides)
                  {
                     MSPPTXContentHandler contentHandler = new MSPPTXContentHandler();
                     xmlReader.setContentHandler(contentHandler);
                     xmlReader.parse(new InputSource((new ByteArrayInputStream(IOUtils.toByteArray(zis)))));
                     slides.put(slideNumber, contentHandler.getContent());
                     slideCount++;
                  }
               }
               ze = zis.getNextEntry();
            }
            for (String slide : slides.values())
            {
               appendText.append(slide);
               appendText.append(' ');
            }
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
         return appendText.toString();
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

            while (ze != null && !ze.getName().equals(PPTX_CORE_NAME))
            {
               ze = zis.getNextEntry();
            }

            if (ze == null)
               return new Properties();

            MSPPTXMetaHandler metaHandler = new MSPPTXMetaHandler();

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

   // ----------------------------< MSPPTXContentHandler >

   private class MSPPTXContentHandler extends DefaultHandler
   {

      private StringBuilder content;

      private boolean appendChar;

      public MSPPTXContentHandler()
      {
         content = new StringBuilder();
         appendChar = false;
      }

      /**
       * Returns the text content extracted from parsed slide<slide_no>.xml
       */
      public String getContent()
      {
         return content.toString();
      }

      @Override
      public void startElement(String namespaceURI, String localName, String rawName, Attributes atts)
         throws SAXException
      {
         if (rawName.startsWith("a:t"))
         {
            appendChar = true;
            if (content.length() > 0 && content.charAt(content.length() - 1) != ' ')
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

   // ----------------------------< MSPPTXMetatHandler >

   private class MSPPTXMetaHandler extends DefaultHandler
   {

      private Properties props;

      private QName curPropertyName;

      private StringBuilder curPropertyValue;

      public MSPPTXMetaHandler()
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
         else if (namespaceURI.equals(URI_CORE_PROPERTIES) && localName.equals("lastModifiedBy"))
         {
            curPropertyName = DCMetaData.CONTRIBUTOR;
         }
         else if (namespaceURI.equals(URI_DC_TERMS) && localName.equals("modified"))
         {
            curPropertyName = DCMetaData.DATE;
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
