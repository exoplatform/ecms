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
import org.exoplatform.services.document.DocumentReader;
import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;
import org.exoplatform.services.document.impl.MSXPPTDocumentReader;
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

public class TestMSXPPTDocumentReader extends BaseStandaloneTest
{
   DocumentReaderServiceImpl service;

   private static final Log LOG = ExoLogger.getLogger(TestMSXPPTDocumentReader.class);

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      service = new DocumentReaderServiceImpl(null);
      service.addDocumentReader(new MSXPPTDocumentReader());
   }

   public void testGetContentAsString() throws Exception
   {
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/test.pptx");
      try
      {
         String text =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
               .getContentAsText(is);
         String etalon =
            "TEST POWERPOINT " + "Manchester United " + "AC Milan " + "SLIDE 2 " + "Eric Cantona " + "Kaka "
               + "Ronaldo " + "The natural scients universitys ";

         assertEquals("Wrong string returned", etalon, text);
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringXXE() throws Exception
   {
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/test.pptx");
      file = createTempFile("test", ".pptx");
      replaceFirstInZip(
         is,
         file,
         "ppt/slides/slide1.xml",
         new String[]{"<p:sld", "<a:t>"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestMSXPPTDocumentReader.class.getResource("/test.txt") + "\">]><p:sld", "<a:t>&xxe;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
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
   
   /**
    * test XXE External Entity point to non-existing resource
    */
   
   public void testGetContentAsStringXXE2() throws Exception
   {
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/test.pptx");
      file = createTempFile("test", ".pptx");
      replaceFirstInZip(
         is,
         file,
         "ppt/slides/slide1.xml",
         new String[]{"<p:sld", "<a:t>"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestMSXPPTDocumentReader.class.getResource("/test123.txt") + "\">]><p:sld", "<a:t>&xxe;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
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
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/test.pptx");
      file = createTempFile("test", ".pptx");
      replaceFirstInZip(is, file, "ppt/slides/slide1.xml", new String[]{"<p:sld", "<a:t>"}, new String[]{
         "<!DOCTYPE lolz [<!ENTITY xee \"xee\">"
            + "<!ENTITY xee1 \"&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;\">"
            + "<!ENTITY xee2 \"&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;\">"
            + "<!ENTITY xee3 \"&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;\">"
            + "<!ENTITY xee4 \"&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;\">"
            + "<!ENTITY xee5 \"&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;\">"
            + "<!ENTITY xee6 \"&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;\">]>" + "<p:sld",
         "<a:t>&xee6;"});
      is = new FileInputStream(file);
      try
      {
         service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
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

   public void testGetContentAsString2() throws Exception
   {
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/test2.pptx");
      try
      {
         String text =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
               .getContentAsText(is);
         int lastIndex = -1;
         int lastLength = 0;
         for (int i = 1; i <= 25; i++)
         {
            String content = "foo" + i;
            int index = text.indexOf(content);
            assertFalse("Cannot find: "+ content, index == -1);
            assertEquals("The content " + content + " has not the right position", index, lastIndex + lastLength + 1);
            lastIndex = index;
            lastLength = content.length();
         }
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringWithLimit() throws Exception
   {
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/test2.pptx");
      try
      {
         DocumentReader reader = service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation");
         assertTrue(reader instanceof MSXPPTDocumentReader);
         String text = ((MSXPPTDocumentReader)reader).getContentAsText(is, 20);
         int lastIndex = -1;
         for (int i = 1; i <= 25; i++)
         {
            String content = "foo" + i;
            int index = text.indexOf(content);
            if (i > 20)
            {
               assertTrue("Can found: "+ content, index == -1);
               continue;
            }
            assertFalse("Cannot found: "+ content, index == -1);
            assertTrue("The content " + content + " has not the right position", index > lastIndex);
            lastIndex = index;
         }
      }
      finally
      {
         is.close();
      }
   }

   public void testPPSXGetContentAsString() throws Exception
   {
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/testPPT.ppsx");
      try
      {
         String content =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.slideshow")
               .getContentAsText(is);
         assertTrue(content
            .contains("This is a test file data with the same content as every other file being tested for"));
         assertTrue(content.contains("Different words to test against"));
         assertTrue(content.contains("Quest"));
         assertTrue(content.contains("Hello"));
         assertTrue(content.contains("Watershed"));
         assertTrue(content.contains("Avalanche"));
         assertTrue(content.contains("Black Panther"));
      }
      finally
      {
         is.close();
      }
   }

   public void testPPTMGetContentAsString() throws Exception
   {
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/testPPT.pptm");
      try
      {
         String content =
            service.getDocumentReader("application/vnd.ms-powerpoint.presentation.macroenabled.12")
               .getContentAsText(is);
         assertTrue(content
            .contains("This is a test file data with the same content as every other file being tested for"));
         assertTrue(content.contains("Different words to test against"));
         assertTrue(content.contains("Quest"));
         assertTrue(content.contains("Hello"));
         assertTrue(content.contains("Watershed"));
         assertTrue(content.contains("Avalanche"));
         assertTrue(content.contains("Black Panther"));
      }
      finally
      {
         is.close();
      }
   }

   public void testPPSMGetContentAsString() throws Exception
   {
      InputStream is = TestMSXPPTDocumentReader.class.getResourceAsStream("/testPPT.ppsm");
      try
      {
         String content =
            service.getDocumentReader("application/vnd.ms-powerpoint.slideshow.macroenabled.12").getContentAsText(is);
         assertTrue(content
            .contains("This is a test file data with the same content as every other file being tested for"));
         assertTrue(content.contains("Different words to test against"));
         assertTrue(content.contains("Quest"));
         assertTrue(content.contains("Hello"));
         assertTrue(content.contains("Watershed"));
         assertTrue(content.contains("Avalanche"));
         assertTrue(content.contains("Black Panther"));
      }
      finally
      {
         is.close();
      }
   }
}
