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

import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchemaBasic;
import org.apache.jempbox.xmp.XMPSchemaDublinCore;
import org.apache.jempbox.xmp.XMPSchemaPDF;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.XMLUtil;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.document.DCMetaData;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Calendar;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS A parser of Adobe PDF files.
 * 
 * @author Phung Hai Nam
 * @author Gennady Azarenkov
 * @version Oct 19, 2005
 */
public class PDFDocumentReader extends BaseDocumentReader
{

   protected static final Log LOG = ExoLogger.getLogger("exo.core.component.document.PDFDocumentReader");

   /**
    * Get the application/pdf mime type.
    * 
    * @return The application/pdf mime type.
    */
   public String[] getMimeTypes()
   {
      return new String[]{"application/pdf"};
   }

   /**
    * Returns only a text from pdf file content.
    * 
    * @param is an input stream with .pdf file content.
    * @return The string only with text from file content.
    */
   public String getContentAsText(final InputStream is) throws IOException, DocumentReadException
   {

      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<String>()
         {
            public String run() throws Exception
            {
               if (is == null)
               {
                  throw new IllegalArgumentException("InputStream is null.");
               }
               PDDocument pdDocument = null;
               StringWriter sw = new StringWriter();
               try
               {
                  if (is.available() == 0)
                     return "";

                  try
                  {
                     pdDocument = PDDocument.load(is);
                  }
                  catch (IOException e)
                  {
                     throw new DocumentReadException("Can not load PDF document.", e);
                  }

                  PDFTextStripper stripper = new PDFTextStripper();
                  stripper.setStartPage(1);
                  stripper.setEndPage(Integer.MAX_VALUE);
                  stripper.writeText(pdDocument, sw);
               }
               finally
               {
                  if (pdDocument != null)
                     try
                     {
                        pdDocument.close();
                     }
                     catch (IOException e)
                     {
                        if (LOG.isTraceEnabled())
                        {
                           LOG.trace("An exception occurred: " + e.getMessage());
                        }
                     }
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
               return sw.toString();
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
   public Properties getProperties(final InputStream is) throws IOException, DocumentReadException
   {
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Properties>()
         {
            public Properties run() throws Exception
            {
               if (is == null)
               {
                  throw new IllegalArgumentException("InputStream is null.");
               }

               PDDocument pdDocument = PDDocument.load(is);
               Properties props = new Properties();
               try
               {
                  
                  PDDocumentCatalog catalog = pdDocument.getDocumentCatalog();
                  PDMetadata meta = catalog.getMetadata();
                  if (meta != null)
                  {
                     XMPMetadata metadata = new XMPMetadata(XMLUtil.parse(meta.createInputStream()));
                     XMPSchemaDublinCore dc = metadata.getDublinCoreSchema();
                     if (dc != null)
                     {
                        try
                        {
                           if (dc.getTitle() != null)
                              props.put(DCMetaData.TITLE, fixEncoding(dc.getTitle()));
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getTitle failed: " + e.getMessage());
                        }
                        try
                        {
                           if (dc.getDescription() != null)
                              props.put(DCMetaData.DESCRIPTION, fixEncoding(dc.getDescription()));
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getSubject failed: " + e.getMessage());
                        }

                        try
                        {
                           if (dc.getCreators() != null)
                           {
                              for (String creator : dc.getCreators())
                              {
                                 props.put(DCMetaData.CREATOR, fixEncoding(creator));
                              }
                           }
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getCreator failed: " + e.getMessage());
                        }

                        try
                        {
                           if (dc.getDates() != null)
                           {
                              for (Calendar date : dc.getDates())
                              {
                                 props.put(DCMetaData.DATE, date);
                              }
                           }
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getDate failed: " + e.getMessage());
                        }
                     }

                     XMPSchemaPDF pdf = metadata.getPDFSchema();
                     if (pdf != null)
                     {
                        try
                        {
                           if (pdf.getKeywords() != null)
                              props.put(DCMetaData.SUBJECT, fixEncoding(pdf.getKeywords()));
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getKeywords failed: " + e.getMessage());
                        }

                        try
                        {
                           if (pdf.getProducer() != null)
                              props.put(DCMetaData.PUBLISHER, fixEncoding(pdf.getProducer()));
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getProducer failed: " + e.getMessage());
                        }
                     }

                     XMPSchemaBasic basic = metadata.getBasicSchema();
                     if (basic != null)
                     {
                        try
                        {
                           if (basic.getCreateDate() != null)
                              props.put(DCMetaData.DATE, basic.getCreateDate());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getCreationDate failed: " + e.getMessage());
                        }
                        try
                        {
                           if (basic.getModifyDate() != null)
                              props.put(DCMetaData.DATE, basic.getModifyDate());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getModificationDate failed: " + e.getMessage());
                        }

                        // DCMetaData.PUBLISHER - basic.getCreatorTool()
                     }
                  }

                  if (props.isEmpty())
                  {
                     // The pdf doesn't contain any metadata, try to use the document
                     // information instead
                     PDDocumentInformation docInfo = pdDocument.getDocumentInformation();

                     if (docInfo != null)
                     {
                        try
                        {
                           if (docInfo.getAuthor() != null)
                              props.put(DCMetaData.CONTRIBUTOR, docInfo.getAuthor());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getAuthor failed: " + e.getMessage());
                        }
                        try
                        {
                           if (docInfo.getCreationDate() != null)
                              props.put(DCMetaData.DATE, docInfo.getCreationDate());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getCreationDate failed: " + e.getMessage());
                        }
                        try
                        {
                           if (docInfo.getCreator() != null)
                              props.put(DCMetaData.CREATOR, docInfo.getCreator());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getCreator failed: " + e.getMessage());
                        }
                        try
                        {

                           if (docInfo.getKeywords() != null)
                              props.put(DCMetaData.SUBJECT, docInfo.getKeywords());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getKeywords failed: " + e.getMessage());
                        }
                        try
                        {
                           if (docInfo.getModificationDate() != null)
                              props.put(DCMetaData.DATE, docInfo.getModificationDate());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getModificationDate failed: " + e.getMessage());
                        }
                        try
                        {
                           if (docInfo.getProducer() != null)
                              props.put(DCMetaData.PUBLISHER, docInfo.getProducer());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getProducer failed: " + e.getMessage());
                        }
                        try
                        {
                           if (docInfo.getSubject() != null)
                              props.put(DCMetaData.DESCRIPTION, docInfo.getSubject());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getSubject failed: " + e.getMessage());
                        }
                        try
                        {
                           if (docInfo.getTitle() != null)
                              props.put(DCMetaData.TITLE, docInfo.getTitle());
                        }
                        catch (Exception e)
                        {
                           LOG.warn("getTitle failed: " + e.getMessage());
                        }

                        // docInfo.getTrapped();
                     }
                  }
               }
               finally
               {
                  if (pdDocument != null)
                  {
                     pdDocument.close();
                  }

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

   private String fixEncoding(String str)
   {
      try
      {
         String encoding = null;
         int orderMaskOffset = 0;

         if (str.startsWith("\\000\\000\\376\\377"))
         {
            encoding = "UTF-32BE";
            orderMaskOffset = 16;
         }
         else if (str.startsWith("\\377\\376\\000\\000"))
         {
            encoding = "UTF-32LE";
            orderMaskOffset = 16;
         }
         else if (str.startsWith("\\376\\377"))
         {
            encoding = "UTF-16BE";
            orderMaskOffset = 8;
         }
         else if (str.startsWith("\\377\\376"))
         {
            encoding = "UTF-16LE";
            orderMaskOffset = 8;
         }

         if (encoding == null)
         {
            // return default
            return str;
         }
         else
         {
            int i = orderMaskOffset, len = str.length();
            char c;
            StringBuilder sb = new StringBuilder(len);
            while (i < len)
            {
               c = str.charAt(i++);
               if (c == '\\')
               {
                  if (i + 3 <= len)
                  {
                     //extract octal-code
                     try
                     {
                        c = (char)Integer.parseInt(str.substring(i, i + 3), 8);
                        i += 3;
                     }
                     catch (NumberFormatException e)
                     {
                        if (LOG.isDebugEnabled())
                        {
                           LOG.debug(
                              "PDF metadata exctraction warning: can not decode octal code - "
                                 + str.substring(i - 1, i + 3) + ".", e);
                        }
                     }
                  }
                  else
                  {
                     if (LOG.isDebugEnabled())
                     {
                        LOG.debug("PDF metadata exctraction warning: octal code is not complete - "
                           + str.substring(i - 1, len));
                     }
                  }
               }
               sb.append(c);
            }

            byte[] bytes = sb.toString().getBytes();
            return new String(bytes, encoding);
         }
      }
      catch (UnsupportedEncodingException e)
      {
         LOG.warn("PDF metadata exctraction warning: can not convert metadata string " + str, e);
         return "";
      }
   }
}
