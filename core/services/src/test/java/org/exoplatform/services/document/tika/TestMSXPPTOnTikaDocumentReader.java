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

import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.test.BaseStandaloneTest;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestMSXPPTOnTikaDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestMSXPPTOnTikaDocumentReader extends BaseStandaloneTest
{
   DocumentReaderService service;

   public void setUp() throws Exception
   {
      super.setUp();
      service = (DocumentReaderService)getComponentInstanceOfType(DocumentReaderService.class);

   }

   public void testGetContentAsString() throws Exception
   {
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.pptx");
      try
      {
         String text =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
               .getContentAsText(is);
         String expected =
            "TEST POWERPOINT\n" + "Manchester United \n" + "AC Milan\n\n\n" + "SLIDE 2 \n" + "Eric Cantona\n" + "Kaka\n"
               + "Ronaldo\n" + "The natural scients universitys\n\n\n";

         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringXEE() throws Exception
   {
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.pptx");
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
         // expected
      }
      finally
      {
         is.close();
      }
   }
}
