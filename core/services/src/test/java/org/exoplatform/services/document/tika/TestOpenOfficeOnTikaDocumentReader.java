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
package org.exoplatform.services.document.tika;

import org.exoplatform.services.document.AdvancedDocumentReader;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.test.BaseStandaloneTest;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestOpenOfficeOnTikaDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestOpenOfficeOnTikaDocumentReader extends BaseStandaloneTest
{
   DocumentReaderService service;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      service = (DocumentReaderService)getComponentInstanceOfType(DocumentReaderService.class);
   }

   public void testGetContentAsStringDoc() throws Exception
   {
      InputStream is = TestOpenOfficeOnTikaDocumentReader.class.getResourceAsStream("/test.odt");
      try
      {
         String text = service.getDocumentReader("application/vnd.oasis.opendocument.text").getContentAsText(is);

         String expected = "This is a test Open Office document `1234567890-= !@#$%^&*()_+~|:?><|\\,./[]{}";
         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsReader() throws Exception
   {
      InputStream is = TestOpenOfficeOnTikaDocumentReader.class.getResourceAsStream("/test.odt");
      try
      {
         Reader reader =
            ((AdvancedDocumentReader)service.getDocumentReader("application/vnd.oasis.opendocument.text"))
               .getContentAsReader(is);

         //read text
         StringBuffer buf = new StringBuffer();
         int c;
         while ((c = reader.read()) != -1)
         {
            char ch = (char)c;
            buf.append(ch);
         }

         String text = buf.toString();
         String expected = "This is a test Open Office document `1234567890-= !@#$%^&*()_+~|:?><|\\,./[]{}";
         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringXXE() throws Exception
   {
      InputStream is = TestOpenOfficeOnTikaDocumentReader.class.getResourceAsStream("/test.odt");
      file = createTempFile("test", ".odt");
      replaceFirstInZip(
         is,
         file,
         "content.xml",
         new String[]{"<office:document-content", "<text:p text:style-name=\"Standard\">"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestOpenOfficeOnTikaDocumentReader.class.getResource("/test.txt") + "\">]><office:document-content",
            "<text:p text:style-name=\"Standard\">&xxe;"});
      is = new FileInputStream(file);
      try
      {
         String text = service.getDocumentReader("application/vnd.oasis.opendocument.text").getContentAsText(is);

         String expected = "This is a test Open Office document `1234567890-= !@#$%^&*()_+~|:?><|\\,./[]{}";

         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringXEE() throws Exception
   {
      InputStream is = TestOpenOfficeOnTikaDocumentReader.class.getResourceAsStream("/test.odt");
      file = createTempFile("test", ".odt");
      replaceFirstInZip(is, file, "content.xml", new String[]{"<office:document-content",
         "<text:p text:style-name=\"Standard\">"}, new String[]{
         "<!DOCTYPE lolz [<!ENTITY xee \"xee\">"
            + "<!ENTITY xee1 \"&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;\">"
            + "<!ENTITY xee2 \"&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;\">"
            + "<!ENTITY xee3 \"&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;\">"
            + "<!ENTITY xee4 \"&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;\">"
            + "<!ENTITY xee5 \"&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;\">"
            + "<!ENTITY xee6 \"&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;\">]>"
            + "<office:document-content", "<text:p text:style-name=\"Standard\">&xee6;"});
      is = new FileInputStream(file);
      try
      {
         service.getDocumentReader("application/vnd.oasis.opendocument.text").getContentAsText(is);

         fail("An exception is expected");
      }
      catch (DocumentReadException e)
      {
         // expected
      }
      finally
      {
         is.close();
      }
   }
}
