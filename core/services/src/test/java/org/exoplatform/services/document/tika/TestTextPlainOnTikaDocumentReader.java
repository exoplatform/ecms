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
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.test.BaseStandaloneTest;

import java.io.InputStream;
import java.io.Reader;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestTextPlainOnTikaDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestTextPlainOnTikaDocumentReader extends BaseStandaloneTest
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
      InputStream is = TestTextPlainOnTikaDocumentReader.class.getResourceAsStream("/test.txt");
      try
      {
         String text = service.getDocumentReader("text/plain").getContentAsText(is);
         String expected = "This is a test text\n";
         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsReader() throws Exception
   {
      InputStream is = TestTextPlainOnTikaDocumentReader.class.getResourceAsStream("/test.txt");
      try
      {
         Reader reader = ((AdvancedDocumentReader)service.getDocumentReader("text/plain")).getContentAsReader(is);

         //read text
         StringBuffer buf = new StringBuffer();
         int c;
         while ((c = reader.read()) != -1)
         {
            char ch = (char)c;
            buf.append(ch);
         }

         String text = buf.toString();
         String expected = "This is a test text\n";
         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringWithEncoding() throws Exception
   {
      InputStream is = TestTextPlainOnTikaDocumentReader.class.getResourceAsStream("/testUTF8.txt");
      try
      {
         String text = service.getDocumentReader("text/plain").getContentAsText(is, "UTF-8");
         String expected =
            "\u0426\u0435 \u0442\u0435\u0441\u0442\u043e\u0432\u0438\u0439 \u0442\u0435\u043a\u0441\u0442. \u042d\u0442\u043e \u0442\u0435\u0441\u0442\u043e\u0432\u044b\u0439 \u0442\u0435\u043a\u0441\u0442.";
         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsReaderWithEncoding() throws Exception
   {
      InputStream is = TestTextPlainOnTikaDocumentReader.class.getResourceAsStream("/testUTF8.txt");
      try
      {
         Reader reader =
            ((AdvancedDocumentReader)service.getDocumentReader("text/plain")).getContentAsReader(is, "UTF-8");

         //read text
         StringBuffer buf = new StringBuffer();
         int c;
         while ((c = reader.read()) != -1)
         {
            char ch = (char)c;
            buf.append(ch);
         }

         String text = buf.toString();
         String expected =
            "\u0426\u0435 \u0442\u0435\u0441\u0442\u043e\u0432\u0438\u0439 \u0442\u0435\u043a\u0441\u0442. \u042d\u0442\u043e \u0442\u0435\u0441\u0442\u043e\u0432\u044b\u0439 \u0442\u0435\u043a\u0441\u0442.";
         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }
}