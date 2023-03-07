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
import org.exoplatform.services.document.test.TestMSXExcelDocumentReader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestMSXWordOnTikaDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestMSXWordOnTikaDocumentReader extends BaseStandaloneTest
{
   DocumentReaderService service;

   private static final Log LOG = ExoLogger.getLogger(TestMSXWordOnTikaDocumentReader.class);

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      service = (DocumentReaderService)getComponentInstanceOfType(DocumentReaderService.class);
   }

   public void testGetContentAsStringDoc() throws Exception
   {
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.docx");
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
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.docx");
      file = createTempFile("test", ".docx");
      replaceFirstInZip(
         is,
         file,
         "word/document.xml",
         new String[]{"<w:document", "<w:t>Before"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + BaseStandaloneTest.class.getResource("/test.txt") + "\">]><w:document", "<w:t>Before&xxe;"});
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
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.docx");
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

   public void testGetContentAsReader() throws Exception
   {
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.docx");
      try
      {
         Reader reader =
            ((AdvancedDocumentReader)service
               .getDocumentReader("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
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
         assertTrue(text
            .contains("Before the test starts there is a directions section, which takes a few minutes to read"));
      }
      finally
      {
         is.close();
      }
   }
}
