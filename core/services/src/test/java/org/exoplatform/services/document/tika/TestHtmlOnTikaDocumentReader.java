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

import org.exoplatform.services.document.DocumentReader;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.test.BaseStandaloneTest;
import org.exoplatform.services.document.test.TestHtmlDocumentReader;

import java.io.InputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * //TODO there is no support for application/xhtml+xml (html with <?xml version="1.0" encoding="UTF-8" standalone="no"?>
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestHtmlOnTikaDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestHtmlOnTikaDocumentReader extends BaseStandaloneTest
{
   DocumentReaderService service;

   public void setUp() throws Exception
   {
      super.setUp();
      service = (DocumentReaderService)getComponentInstanceOfType(DocumentReaderService.class);
   }

   public void testGetContentAsString() throws Exception
   {

      InputStream is = TestHtmlDocumentReader.class.getResourceAsStream("/test-tika.html");
      try
      {
         String mimeType = mimetypeResolver.getMimeType("test-tika.html");

         DocumentReader dr = service.getDocumentReader(mimeType);
         String text = dr.getContentAsText(is);
         assertTrue(text.contains("This is the third maintenance release of the redesigned 2.0"));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringHTMLWithXML() throws Exception
   {

      InputStream is = TestHtmlDocumentReader.class.getResourceAsStream("/ch-core.html");
      String mimeType = mimetypeResolver.getMimeType("ch-core.html");

      DocumentReader dr = service.getDocumentReader(mimeType);
      String text = dr.getContentAsText(is);

      assertTrue((normalizeWhitespaces(text))
         .contains("The eXo Core is a set of common services that are used by eXo products and modules, it also can be used in the business logic. It's Authentication and Security, Organization, Database, Logging, JNDI, LDAP, Document reader and other services."));
   }
}
