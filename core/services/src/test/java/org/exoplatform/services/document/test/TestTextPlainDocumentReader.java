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

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;
import org.exoplatform.services.document.impl.TextPlainDocumentReader;

import java.io.InputStream;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko
 * <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestTextPlainDocumentReader extends BaseStandaloneTest
{
   DocumentReaderServiceImpl service;

   public void setUp() throws Exception
   {
      super.setUp();
      service = new DocumentReaderServiceImpl(null);
      InitParams params = new InitParams();
      service.addDocumentReader(new TextPlainDocumentReader(params));
   }

   public void testGetContentAsString() throws Exception
   {
      InputStream is = TestTextPlainDocumentReader.class.getResourceAsStream("/test.txt");
      try
      {
         String text = service.getDocumentReader("text/plain").getContentAsText(is);
         assertEquals("Wrong string returned", "This is a test text\n", text.replaceAll("\r\n", "\n"));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringWithEncoding() throws Exception
   {
      InputStream is = TestTextPlainDocumentReader.class.getResourceAsStream("/testUTF8.txt");
      try
      {
         String text = service.getDocumentReader("text/plain").getContentAsText(is, "UTF-8");
         String expected =
            "\ufeff\u0426\u0435 \u0442\u0435\u0441\u0442\u043e\u0432\u0438\u0439 \u0442\u0435\u043a\u0441\u0442. \u042d\u0442\u043e \u0442\u0435\u0441\u0442\u043e\u0432\u044b\u0439 \u0442\u0435\u043a\u0441\u0442.";
         assertEquals("Wrong string returned", expected, text);
      }
      finally
      {
         is.close();
      }
   }

}
