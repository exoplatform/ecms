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

import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.test.BaseStandaloneTest;
import org.exoplatform.services.document.test.TestMSWordDocumentReader;

import java.io.InputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestMSWordOnTikaDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestMSWordOnTikaDocumentReader extends BaseStandaloneTest
{
   DocumentReaderService service;

   public void setUp() throws Exception
   {
      super.setUp();
      service = (DocumentReaderService)getComponentInstanceOfType(DocumentReaderService.class);
   }

   public void testGetContentAsStringTemplate() throws Exception
   {
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.dot");
      String text = service.getDocumentReader("application/msworddot").getContentAsText(is);
      String expected = "exotest";
      assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
   }

   public void testGetContentAsStringDoc() throws Exception
   {
      InputStream is = TestMSWordDocumentReader.class.getResourceAsStream("/test.doc");
      String text = service.getDocumentReader("application/msword").getContentAsText(is);

      assertTrue(text
         .contains("Before the test starts there is a directions section, which takes a few minutes to read"));
   }

}
