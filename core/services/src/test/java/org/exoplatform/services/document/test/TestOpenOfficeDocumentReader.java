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
import org.exoplatform.services.document.impl.OpenOfficeDocumentReader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko
 * <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestOpenOfficeDocumentReader extends BaseStandaloneTest
{
   DocumentReaderServiceImpl service;

   private static final Log LOG = ExoLogger.getLogger(TestOpenOfficeDocumentReader.class);

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      service = new DocumentReaderServiceImpl(null);
      service.addDocumentReader(new OpenOfficeDocumentReader());
   }

   public void testGetContentAsString() throws Exception
   {
      InputStream is = TestOpenOfficeDocumentReader.class.getResourceAsStream("/test.odt");
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

   public void testGetContentAsStringXXE() throws Exception
   {
      InputStream is = TestOpenOfficeDocumentReader.class.getResourceAsStream("/test.odt");
      file = createTempFile("test", ".odt");
      replaceFirstInZip(
         is,
         file,
         "content.xml",
         new String[]{"<office:document-content", "<text:p text:style-name=\"Standard\">"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestOpenOfficeDocumentReader.class.getResource("/test.txt") + "\">]><office:document-content",
            "<text:p text:style-name=\"Standard\">&xxe;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.oasis.opendocument.text").getContentAsText(is);

         fail("XXE are not allowed and must generate an error");
       } catch (DocumentReadException e) {
         LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }

   /**
    * test XXE External Entity point to non-existing resource
    */
   
   public void testGetContentAsStringXXE2() throws Exception
   {
      InputStream is = TestOpenOfficeDocumentReader.class.getResourceAsStream("/test.odt");
      file = createTempFile("test", ".odt");
      replaceFirstInZip(
         is,
         file,
         "content.xml",
         new String[]{"<office:document-content", "<text:p text:style-name=\"Standard\">"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestOpenOfficeDocumentReader.class.getResource("/test123.txt") + "\">]><office:document-content",
            "<text:p text:style-name=\"Standard\">&xxe;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.oasis.opendocument.text").getContentAsText(is);

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
      InputStream is = TestOpenOfficeDocumentReader.class.getResourceAsStream("/test.odt");
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
         // Expected
      }
      finally
      {
         is.close();
      }
   }
}
