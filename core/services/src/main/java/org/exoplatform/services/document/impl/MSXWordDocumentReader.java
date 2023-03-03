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
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlException;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS A parser of Microsoft Word 2007 files (docx).
 * 
 * @author <a href="mailto:phunghainam@gmail.com">Phung Hai Nam</a>
 * @author Gennady Azarenkov
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: MSXWordDocumentReader.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class MSXWordDocumentReader extends BaseDocumentReader
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.MSXWordDocumentReader");

   /**
    * @see org.exoplatform.services.document.DocumentReader#getMimeTypes()
    */
   public String[] getMimeTypes()
   {
      return new String[]{"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
         "application/vnd.openxmlformats-officedocument.wordprocessingml.template",
         "application/vnd.ms-word.document.macroenabled.12", "application/vnd.ms-word.template.macroenabled.12"};
   }

   /**
    * Returns only a text from .docx file content.
    * 
    * @param is an input stream with .docx file content.
    * @return The string only with text from file content.
    */
   public String getContentAsText(final InputStream is) throws IOException, DocumentReadException
   {
      if (is == null)
      {
         throw new IllegalArgumentException("InputStream is null.");
      }
      String text = "";
      try
      {
         if (is.available() == 0)
         {
            return "";
         }
         
         XWPFDocument doc;
         try
         {
            doc = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<XWPFDocument>()
            {
               public XWPFDocument run() throws Exception
               {
                  return new XWPFDocument(is);
               }
            });
         }
         catch (RuntimeException cause)
         {
            throw new DocumentReadException("Can not get the content: " + cause.getMessage(), cause);
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

         final XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
         try
         {
            text = SecurityHelper.doPrivilegedAction(new PrivilegedAction<String>()
            {
               public String run()
               {
                  return extractor.getText();
               }
            });
         }
         catch (Exception cause)
         {
            throw new DocumentReadException("Can not get the content: " + cause.getMessage(), cause);
         }
         finally
         {
            extractor.close();
         }
      }
      finally
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
      return text.trim();
   }

   /**
    * @see org.exoplatform.services.document.DocumentReader#getContentAsText(java.io.InputStream, java.lang.String)
    */
   public String getContentAsText(InputStream is, String encoding) throws IOException, DocumentReadException
   {
      // Ignore encoding
      return getContentAsText(is);
   }

   /**
    * @see org.exoplatform.services.document.DocumentReader#getProperties(java.io.InputStream)
    */
   public Properties getProperties(final InputStream is) throws IOException, DocumentReadException
   {
      try
      {
         OPCPackage container =
            SecurityHelper.doPrivilegedIOExceptionAction(new PrivilegedExceptionAction<OPCPackage>()
            {
               public OPCPackage run() throws Exception
               {
                  return OPCPackage.open(is);
               }
            });
         POIXMLProperties xmlProperties = new POIXMLProperties(container);
         POIPropertiesReader reader = new POIPropertiesReader();
         reader.readDCProperties(xmlProperties);
         return reader.getProperties();
      }
      catch (InvalidFormatException e)
      {
         throw new DocumentReadException("The format of the document to read is invalid.", e);
      }
      catch (XmlException e)
      {
         throw new DocumentReadException("Problem during the document parsing.", e);
      }
      catch (OpenXML4JException e)
      {
         throw new DocumentReadException("Problem during the document parsing.", e);
      }
   }

}
