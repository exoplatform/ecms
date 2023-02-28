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
package org.exoplatform.services.document.test;

import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;
import org.exoplatform.services.document.impl.MSXWordDocumentReader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: TestMSXWordDocumentReader.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class TestMSXWordDocumentReader extends BaseStandaloneTest
{
   DocumentReaderServiceImpl service;

   private static final Log LOG = ExoLogger.getLogger(TestMSXWordDocumentReader.class);

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      service = new DocumentReaderServiceImpl(null);
      service.addDocumentReader(new MSXWordDocumentReader());
   }

   public void testDOCXGetContentAsStringDoc() throws Exception
   {
      InputStream is = TestMSXWordDocumentReader.class.getResourceAsStream("/test.docx");
      try
      {
         String text =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
               .getContentAsText(is);
         assertTrue(text
            .contains("Before the test starts there is a directions section, which takes a few minutes to read"));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringXXE() throws Exception
   {
      InputStream is = TestMSXWordDocumentReader.class.getResourceAsStream("/test.docx");
      file = createTempFile("test", ".docx");
      replaceFirstInZip(
         is,
         file,
         "word/document.xml",
         new String[]{"<w:document", "<w:t>Before"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestMSXWordDocumentReader.class.getResource("/test.txt") + "\">]><w:document", "<w:t>Before&xxe;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
               .getContentAsText(is);
         fail("XXE are not allowed and must generate an error");
       } catch (DocumentReadException e) {
         LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringXEE() throws Exception
   {
      InputStream is = TestMSXWordDocumentReader.class.getResourceAsStream("/test.docx");
      file = createTempFile("test", ".docx");
      replaceFirstInZip(is, file, "word/document.xml", new String[]{"<w:document", "<w:t>Before"}, new String[]{
         "<!DOCTYPE lolz [<!ENTITY xee \"xee\">"
            + "<!ENTITY xee1 \"&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;\">"
            + "<!ENTITY xee2 \"&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;\">"
            + "<!ENTITY xee3 \"&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;\">"
            + "<!ENTITY xee4 \"&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;\">"
            + "<!ENTITY xee5 \"&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;\">"
            + "<!ENTITY xee6 \"&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;\">]>" + "<w:document",
         "<w:t>Before&xee6;"});
      is = new FileInputStream(file);
      try
      {
         service.getDocumentReader("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .getContentAsText(is);

         fail("An exception is expected");
      }
      catch (DocumentReadException e)
      {
         // Expected
      }
      finally
      {
         is.close();
      }
   }

   public void testDOTXGetContentAsStringDoc() throws Exception
   {
      InputStream is = TestMSXWordDocumentReader.class.getResourceAsStream("/testWORD.dotx");
      try
      {
         String text =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.wordprocessingml.template")
               .getContentAsText(is);
         assertTrue(text.contains("template"));
      }
      finally
      {
         is.close();
      }
   }

   public void testDOCMGetContentAsStringDoc() throws Exception
   {
      InputStream is = TestMSXWordDocumentReader.class.getResourceAsStream("/testWORD.docm");
      try
      {
         String text =
            service.getDocumentReader("application/vnd.ms-word.document.macroenabled.12").getContentAsText(is);
         assertTrue(text.contains("template"));
      }
      finally
      {
         is.close();
      }
   }

   public void testDOTMGetContentAsStringDoc() throws Exception
   {
      InputStream is = TestMSXWordDocumentReader.class.getResourceAsStream("/testWORD.dotm");
      try
      {
         String text =
            service.getDocumentReader("application/vnd.ms-word.template.macroenabled.12").getContentAsText(is);
         assertTrue(text.contains("Template with macros"));
      }
      finally
      {
         is.close();
      }
   }
}
